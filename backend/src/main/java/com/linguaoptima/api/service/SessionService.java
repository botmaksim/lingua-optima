package com.linguaoptima.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.SessionStatus;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.dto.request.AnswerRequest;
import com.linguaoptima.api.dto.response.AnswerFeedbackResponse;
import com.linguaoptima.api.dto.response.QuestionResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final SessionStateRepository sessionStateRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final TaskQuestionRepository taskQuestionRepository;
    private final SubmissionRepository submissionRepository;
    private final ProgressService progressService;
    private final GamificationService gamificationService;
    private final ObjectMapper objectMapper;

    @Transactional
    public SessionState startSession(UUID assignmentId, User student) {
        TaskAssignment assignment = taskAssignmentRepository.findById(assignmentId)
            .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (!assignment.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Assignment does not belong to student.");
        }

        assignment.setStatus(AssignmentStatus.IN_PROGRESS);
        taskAssignmentRepository.save(assignment);

        SessionState state = SessionState.builder()
            .assignment(assignment)
            .student(student)
            .currentQuestionIndex(0)
            .currentDifficulty(2) // Initial: MEDIUM = 2
            .answersJson("[]")
            .status(SessionStatus.IN_PROGRESS)
            .startedAt(LocalDateTime.now())
            .lastActiveAt(LocalDateTime.now())
            .build();

        return sessionStateRepository.save(state);
    }

    @Transactional(readOnly = true)
    public Optional<SessionState> getActiveSession(User student) {
        return sessionStateRepository.findFirstByStudentIdAndStatusOrderByStartedAtDesc(
            student.getId(), SessionStatus.IN_PROGRESS);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getNextQuestion(UUID sessionId, User student) {
        SessionState session = getSessionAndVerify(sessionId, student);
        UUID taskId = session.getAssignment().getTask().getId();

        List<TaskQuestion> matchingQuestions = taskQuestionRepository.findByTaskIdAndDifficulty(
            taskId, session.getCurrentDifficulty());

        if (matchingQuestions.isEmpty()) {
            matchingQuestions = taskQuestionRepository.findByTaskIdOrderByQuestionOrder(taskId);
        }

        if (matchingQuestions.isEmpty()) {
            throw new ResourceNotFoundException("No questions available for this task.");
        }

        // Pick next question not yet answered
        List<Map<String, Object>> recordedAnswers = parseAnswers(session.getAnswersJson());
        Set<UUID> answeredIds = new HashSet<>();
        for (Map<String, Object> ans : recordedAnswers) {
            Object qid = ans.get("questionId");
            if (qid != null) answeredIds.add(UUID.fromString(qid.toString()));
        }

        TaskQuestion nextQ = matchingQuestions.stream()
            .filter(q -> !answeredIds.contains(q.getId()))
            .findFirst()
            .orElse(matchingQuestions.get(0));

        return QuestionResponse.fromEntity(nextQ);
    }

    @Transactional
    public AnswerFeedbackResponse submitAnswer(UUID sessionId, AnswerRequest request, User student) {
        SessionState session = getSessionAndVerify(sessionId, student);

        TaskQuestion question = taskQuestionRepository.findById(request.getQuestionId())
            .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + request.getQuestionId()));

        boolean isCorrect = question.getCorrectAnswer().trim().equalsIgnoreCase(request.getAnswer().trim());

        // CAT Algorithm: Adjust difficulty
        int oldDiff = session.getCurrentDifficulty();
        int newDiff = isCorrect ? Math.min(oldDiff + 1, 4) : Math.max(oldDiff - 1, 1);
        session.setCurrentDifficulty(newDiff);
        session.setCurrentQuestionIndex(session.getCurrentQuestionIndex() + 1);
        session.setLastActiveAt(LocalDateTime.now());

        // Update topic mastery
        progressService.updateFromSubmission(student, question.getGrammarRule(), isCorrect);

        // Record answer
        List<Map<String, Object>> answers = parseAnswers(session.getAnswersJson());
        answers.add(Map.of(
            "questionId", question.getId().toString(),
            "givenAnswer", request.getAnswer(),
            "correctAnswer", question.getCorrectAnswer(),
            "isCorrect", isCorrect,
            "difficulty", oldDiff
        ));
        session.setAnswersJson(writeAnswers(answers));

        boolean completed = session.getCurrentQuestionIndex() >= 10;
        Double finalMastery = null;

        if (completed) {
            session.setStatus(SessionStatus.COMPLETED);
            finalMastery = completeSessionInternal(session, student, answers);
        }

        sessionStateRepository.save(session);

        return AnswerFeedbackResponse.builder()
            .correct(isCorrect)
            .correctAnswer(question.getCorrectAnswer())
            .explanation(isCorrect ? "Correct!" : "Incorrect. Correct answer is: " + question.getCorrectAnswer())
            .newDifficulty(newDiff)
            .currentQuestionIndex(session.getCurrentQuestionIndex())
            .completed(completed)
            .masteryScore(finalMastery)
            .build();
    }

    @Transactional
    public SubmissionResultResponse completeSession(UUID sessionId, User student) {
        SessionState session = getSessionAndVerify(sessionId, student);
        session.setStatus(SessionStatus.COMPLETED);
        session.setLastActiveAt(LocalDateTime.now());

        List<Map<String, Object>> answers = parseAnswers(session.getAnswersJson());
        double mastery = completeSessionInternal(session, student, answers);
        sessionStateRepository.save(session);

        Submission sub = submissionRepository.findByAssignmentId(session.getAssignment().getId()).stream()
            .findFirst()
            .orElseGet(() -> submissionRepository.save(Submission.builder()
                .assignment(session.getAssignment())
                .student(student)
                .submissionType(SubmissionType.TEXT)
                .studentText("Adaptive CAT Session Completed. Mastery: " + mastery)
                .aiScore(mastery * 100.0)
                .aiFeedback("Adaptive CAT evaluation completed.")
                .providerUsed("CAT_ALGORITHM")
                .submittedAt(LocalDateTime.now())
                .build()));

        return SubmissionResultResponse.fromEntity(sub);
    }

    private double completeSessionInternal(SessionState session, User student, List<Map<String, Object>> answers) {
        // Mastery score formula: (sum of correctly_answered_difficulty_levels) / (sum of all_difficulty_levels)
        int correctDiffSum = 0;
        int totalDiffSum = 0;

        for (Map<String, Object> ans : answers) {
            int diff = ((Number) ans.getOrDefault("difficulty", 2)).intValue();
            totalDiffSum += diff;
            if (Boolean.TRUE.equals(ans.get("isCorrect"))) {
                correctDiffSum += diff;
            }
        }

        double score = totalDiffSum > 0 ? (double) correctDiffSum / totalDiffSum : 0.0;
        double roundedScore = Math.round(score * 100.0) / 100.0;

        TaskAssignment assignment = session.getAssignment();
        assignment.setStatus(AssignmentStatus.SUBMITTED);
        taskAssignmentRepository.save(assignment);

        Submission submission = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .studentText("CAT session with " + answers.size() + " questions answered.")
            .aiScore(roundedScore * 100.0)
            .aiFeedback("CAT Session completed. Accuracy weighted by difficulty: " + (roundedScore * 100.0) + "%")
            .providerUsed("CAT_ALGORITHM")
            .submittedAt(LocalDateTime.now())
            .build();
        submissionRepository.save(submission);

        gamificationService.onSubmissionCompleted(student);
        return roundedScore;
    }

    private SessionState getSessionAndVerify(UUID sessionId, User student) {
        SessionState session = sessionStateRepository.findById(sessionId)
            .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
        if (!session.getStudent().getId().equals(student.getId())) {
            throw new ForbiddenException("Access denied to session.");
        }
        return session;
    }

    private List<Map<String, Object>> parseAnswers(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String writeAnswers(List<Map<String, Object>> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (Exception e) {
            return "[]";
        }
    }
}
