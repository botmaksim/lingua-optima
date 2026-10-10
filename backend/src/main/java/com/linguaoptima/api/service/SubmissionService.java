/**
 * @file SubmissionService.java
 * @brief Service responsible for student homework submissions, OCR processing, and teacher grading overrides.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.SessionState;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.TaskQuestion;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.AiAnalysisResponse;
import com.linguaoptima.api.dto.response.SentenceCorrectionResponse;
import com.linguaoptima.api.dto.response.SubmissionItemResponse;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.OcrException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.SessionStateRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import com.linguaoptima.api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @brief Service responsible for student homework submissions, OCR processing, and teacher grading overrides.
 *
 * Implements strict Zero-Retention OCR architecture: uploaded images are processed in-memory
 * and instantly discarded, ensuring no image binary data or biometric photos are persisted.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    /** @brief Field representing submission repository in SubmissionService. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field representing task assignment repository in SubmissionService. */
    private final TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Field representing task repository in SubmissionService. */
    private final TaskRepository taskRepository;
    /** @brief Field representing session state repository in SubmissionService. */
    private final SessionStateRepository sessionStateRepository;
    /** @brief Field representing ocr service in SubmissionService. */
    private final OCRService ocrService;
    /** @brief Field representing scoring service in SubmissionService. */
    private final ScoringService scoringService;
    /** @brief Field representing usage service in SubmissionService. */
    private final UsageService usageService;
    /** @brief Field representing progress service in SubmissionService. */
    private final ProgressService progressService;
    /** @brief Field representing gamification service in SubmissionService. */
    private final GamificationService gamificationService;
    /** @brief Field representing notification service in SubmissionService. */
    private final NotificationService notificationService;
    /** @brief Field representing object mapper in SubmissionService. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Evaluates and saves a written text or essay submission using AI.
     * @param request Submission request containing text content, optional assignment ID, and type.
     * @param student The student submitting the homework.
     * @return SubmissionResultResponse DTO containing AI evaluation, score, and rubric.
     * @throws ResourceNotFoundException if assignment does not exist.
     * @throws ForbiddenException if assignment does not belong to the student.
     */
    @Transactional
    public SubmissionResultResponse submitText(TextSubmissionRequest request, User student) {
        usageService.incrementEvaluation(student);

        TaskAssignment assignment = null;
        String answerKey = "";
        String grammarTopic = "General";

        if (request.getAssignmentId() != null) {
            assignment = taskAssignmentRepository.findById(request.getAssignmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + request.getAssignmentId()));
            if (!assignment.getStudent().getId().equals(student.getId())) {
                throw new ForbiddenException("Assignment does not belong to student.");
            }
            validateAndRecordAttempt(assignment);

            if (assignment.getTask() != null) {
                answerKey = assignment.getTask().getAnswerKey();
                grammarTopic = assignment.getTask().getGrammarTopic();
            }
        } else if (request.getTaskId() != null) {
            Task task = taskRepository.findById(request.getTaskId()).orElse(null);
            if (task != null) {
                answerKey = task.getAnswerKey();
                grammarTopic = task.getGrammarTopic();
                Optional<TaskAssignment> existingOpt = taskAssignmentRepository.findByStudentIdAndTaskId(student.getId(), task.getId());
                if (existingOpt.isPresent()) {
                    assignment = existingOpt.get();
                    validateAndRecordAttempt(assignment);
                } else {
                    assignment = taskAssignmentRepository.save(TaskAssignment.builder()
                        .student(student)
                        .task(task)
                        .assignedBy(task.getCreatedBy() != null ? task.getCreatedBy() : student)
                        .status(AssignmentStatus.SUBMITTED)
                        .maxAttempts(0)
                        .attemptsUsed(1)
                        .createdAt(LocalDateTime.now())
                        .build());
                }
            }
        }

        Map<String, Object> scoring;
        String provider = "AI_GEMINI";
        if ("ESSAY".equalsIgnoreCase(request.getType())) {
            scoring = scoringService.scoreEssay(request.getText(), student.getCefrLevel().name(), student);
        } else {
            scoring = scoringService.scoreGrammarTask(request.getText(), answerKey, student);
            provider = "AI_GROQ";
        }

        Double score = scoringService.extractScore(scoring);
        String feedback = String.valueOf(scoring.getOrDefault("feedback", "Submission evaluated successfully."));

        Submission submission = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .studentText(request.getText())
            .aiScore(score)
            .aiFeedback(feedback)
            .providerUsed(provider)
            .submittedAt(LocalDateTime.now())
            .build();

        Submission saved = submissionRepository.save(submission);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
        enrichSubmissionResult(response, saved, scoring);

        Task currentTask = (assignment != null) ? assignment.getTask() : null;
        if (currentTask == null && request.getTaskId() != null) {
            currentTask = taskRepository.findById(request.getTaskId()).orElse(null);
        }
        recordSubmissionProgress(student, grammarTopic, score, response, currentTask);
        gamificationService.onSubmissionCompleted(student);
        return response;
    }

    /**
     * @brief Processes an uploaded homework photo via in-memory OCR and scores the extracted text.
     *
     * In accordance with Zero-Retention OCR design, the uploaded image bytes are passed
     * directly to OCR and immediately garbage collected; only the recognized text is stored.
     *
     * @param file The multipart image file containing handwritten or printed text.
     * @param assignmentId Optional task assignment identifier.
     * @param student The student submitting the photo.
     * @return SubmissionResultResponse DTO containing extracted text and grading results.
     * @throws OcrException if image reading or text recognition fails.
     * @throws ResourceNotFoundException if assignment is not found.
     * @throws ForbiddenException if assignment does not belong to the student.
     */
    @Transactional
    public SubmissionResultResponse submitImage(MultipartFile file, UUID assignmentId, User student) {
        usageService.incrementOcr(student);
        usageService.incrementEvaluation(student);

        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (IOException e) {
            throw new OcrException("Failed to read image file.", e);
        }

        String extractedText = ocrService.extractText(imageBytes);

        TaskAssignment assignment = null;
        String answerKey = "";
        String grammarTopic = "OCR Homework";

        if (assignmentId != null) {
            assignment = taskAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));
            if (!assignment.getStudent().getId().equals(student.getId())) {
                throw new ForbiddenException("Assignment does not belong to student.");
            }
            validateAndRecordAttempt(assignment);

            if (assignment.getTask() != null) {
                answerKey = assignment.getTask().getAnswerKey();
                grammarTopic = assignment.getTask().getGrammarTopic();
            }
        }

        Map<String, Object> scoring = scoringService.scoreGrammarTask(extractedText, answerKey, student);
        Double score = scoringService.extractScore(scoring);
        String feedback = String.valueOf(scoring.getOrDefault("feedback", "OCR text successfully scored."));

        Submission submission = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.IMAGE)
            .studentText(extractedText)
            .aiScore(score)
            .aiFeedback(feedback)
            .providerUsed("TESSERACT_OCR+AI")
            .submittedAt(LocalDateTime.now())
            .build();

        Submission saved = submissionRepository.save(submission);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
        enrichSubmissionResult(response, saved, scoring);

        Task currentTask = (assignment != null) ? assignment.getTask() : null;
        recordSubmissionProgress(student, grammarTopic, score, response, currentTask);
        gamificationService.onSubmissionCompleted(student);
        return response;
    }

    /**
     * @brief Verifies that the student has remaining attempts and records the new submission attempt.
     * @param assignment The task assignment being submitted.
     * @throws ForbiddenException if the maximum allowed attempts have already been exhausted.
     */
    private void validateAndRecordAttempt(TaskAssignment assignment) {
        Integer maxAttempts = assignment.getMaxAttempts();
        int used = assignment.getAttemptsUsed();
        if (maxAttempts != null && maxAttempts > 0 && used >= maxAttempts) {
            throw new ForbiddenException("Maximum attempts (" + maxAttempts + ") reached for this assigned task.");
        }
        assignment.setAttemptsUsed(used + 1);
        assignment.setStatus(AssignmentStatus.SUBMITTED);
        taskAssignmentRepository.save(assignment);
    }

    /**
     * @brief Allows an educator to override an AI-generated score with manual grade and commentary.
     * @param submissionId Unique identifier of the student submission.
     * @param request Payload containing teacher override score and comment.
     * @param teacher Educator executing the score override.
     * @return SubmissionResultResponse DTO reflecting the updated score.
     * @throws ForbiddenException if caller is not an educator or administrator.
     * @throws ResourceNotFoundException if submission is not found.
     */
    @Transactional
    public SubmissionResultResponse overrideScore(UUID submissionId, OverrideRequest request, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can override scores.");
        }

        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        submission.setOverrideScore(request.getOverrideScore());
        if (request.getTeacherComment() != null) {
            submission.setTeacherComment(request.getTeacherComment());
        }
        if (request.getFeedback() != null) {
            submission.setAiFeedback(request.getFeedback());
        }
        Submission saved = submissionRepository.save(submission);

        if (saved.getAssignment() != null) {
            saved.getAssignment().setStatus(AssignmentStatus.GRADED);
            taskAssignmentRepository.save(saved.getAssignment());
        }

        notificationService.send(submission.getStudent(),
            "👨‍🏫 Your grade was reviewed by " + teacher.getFullName() + ". New score: " + request.getOverrideScore(),
            NotificationType.GRADE);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        enrichSubmissionResult(response, saved, null);
        return response;
    }

    /**
     * @brief Retrieves submission history for the specified student or educator.
     * @param student The user whose submission history is requested.
     * @return List of SubmissionResultResponse DTOs ordered by submission date descending.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getMySubmissions(User student) {
        if (student.getRole() == Role.TEACHER || student.getRole() == Role.ADMIN) {
            return submissionRepository.findAll().stream()
                .map(SubmissionResultResponse::fromEntity)
                .collect(Collectors.toList());
        }
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
            .map(SubmissionResultResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @brief Retrieves all active student submissions for a specific group.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator requesting group submissions.
     * @return List of SubmissionResultResponse DTOs for the group.
     * @throws ForbiddenException if caller is not an educator or administrator.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getGroupSubmissions(UUID groupId, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can view group submissions.");
        }
        return submissionRepository.findActiveGroupSubmissions(groupId).stream()
            .map(sub -> {
                SubmissionResultResponse res = SubmissionResultResponse.fromEntity(sub);
                enrichSubmissionResult(res, sub, null);
                return res;
            })
            .collect(Collectors.toList());
    }

    /**
     * @brief Retrieves all student submissions across all groups and assignments belonging to this educator.
     *
     * Explicitly excludes any self-submissions the educator made as a student for personal practice.
     *
     * @param teacher Authenticated educator principal.
     * @return List of student SubmissionResultResponse DTOs pending or reviewed by the educator.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getTeacherSubmissions(User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can access the educator submissions review queue.");
        }
        return submissionRepository.findTeacherStudentSubmissions(teacher.getId()).stream()
            .map(sub -> {
                SubmissionResultResponse res = SubmissionResultResponse.fromEntity(sub);
                enrichSubmissionResult(res, sub, null);
                return res;
            })
            .collect(Collectors.toList());
    }

    /**
     * @brief Retrieves a specific submission by identifier, ensuring authorization access control.
     * @param submissionId Unique identifier of the submission.
     * @param user User requesting to view the submission.
     * @return SubmissionResultResponse DTO containing full submission details.
     * @throws ResourceNotFoundException if submission does not exist.
     * @throws ForbiddenException if user is neither the submitting student nor an educator.
     */
    @Transactional(readOnly = true)
    public SubmissionResultResponse getSubmissionById(UUID submissionId, User user) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        boolean isStudent = submission.getStudent().getId().equals(user.getId());
        boolean isTeacher = user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN;

        if (!isStudent && !isTeacher) {
            throw new ForbiddenException("Access denied to this submission.");
        }

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(submission);
        enrichSubmissionResult(response, submission, null);
        return response;
    }

    /**
     * @brief Enriches a submission result with sentence-by-sentence evaluation items, corrections, and AI gap analysis.
     * @param response The response DTO to enrich.
     * @param submission The domain submission entity.
     * @param rubric Optional scoring rubric map from AI grading.
     */
    public void enrichSubmissionResult(SubmissionResultResponse response, Submission submission, Map<String, Object> rubric) {
        if (response == null || submission == null) return;

        List<SubmissionItemResponse> items = new ArrayList<>();
        List<SentenceCorrectionResponse> corrections = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        List<String> strengths = new ArrayList<>();

        // 1. Check CAT Session
        if (submission.getAssignment() != null) {
            Optional<SessionState> sessionOpt = sessionStateRepository.findByAssignmentId(submission.getAssignment().getId());
            if (sessionOpt.isPresent() && sessionOpt.get().getAnswersJson() != null && !sessionOpt.get().getAnswersJson().isBlank()) {
                enrichFromCatSession(sessionOpt.get(), items, weaknesses, strengths);
            }
        }

        // 2. Check Task Questions (MCQ, GAP_FILL, REWRITE, etc.)
        if (items.isEmpty() && submission.getAssignment() != null && submission.getAssignment().getTask() != null) {
            Task task = submission.getAssignment().getTask();
            enrichFromTaskQuestions(task, submission.getStudentText(), items, weaknesses, strengths);
        }

        // 3. Extract from Rubric
        if (rubric != null) {
            extractFromRubric(rubric, corrections, weaknesses, strengths, items);
        }

        // 4. Synthesize questions from studentText only if items still empty and student text contains explicit question markers (e.g. Q1:, Question 1:)
        if (items.isEmpty() && submission.getStudentText() != null && hasQuestionMarkers(submission.getStudentText())) {
            enrichFromParsedStudentAnswers(submission.getStudentText(), submission.getAiFeedback(), items, weaknesses, strengths);
        }

        // 5. Extract from Feedback if it contains JSON
        extractFromFeedback(submission.getAiFeedback(), corrections, weaknesses, strengths);

        // Deduplicate
        List<String> dedupWeaknesses = weaknesses.stream()
            .filter(w -> w != null && !w.isBlank())
            .distinct()
            .collect(Collectors.toList());

        List<String> dedupStrengths = strengths.stream()
            .filter(s -> s != null && !s.isBlank())
            .distinct()
            .collect(Collectors.toList());

        String summary = buildSummary(response, items, corrections);
        String recommendations = buildRecommendations(response, dedupWeaknesses, dedupStrengths);
        List<String> suggestedTopics = buildSuggestedTopics(response, dedupWeaknesses);

        AiAnalysisResponse analysis = AiAnalysisResponse.builder()
            .summary(summary)
            .weaknesses(dedupWeaknesses)
            .strengths(dedupStrengths)
            .recommendations(recommendations)
            .suggestedTopics(suggestedTopics)
            .build();

        response.setItems(items);
        response.setCorrections(corrections);
        response.setAiAnalysis(analysis);
    }

    /**
     * @brief Checks if student text contains explicit question number indicators (e.g. Q1:, Question 1:, Q2.).
     * @param text Student text.
     * @return True if question indicators are detected.
     */
    private boolean hasQuestionMarkers(String text) {
        if (text == null || text.isBlank()) return false;
        Pattern pattern = Pattern.compile("(?:^|\\s)(?:Q|Question\\s*)\\d+[:.)\\-\\s]+", Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }

    /**
     * @brief Synthesizes question breakdown items from student text when structured task questions were not linked.
     * @param studentText Raw student text.
     * @param feedback Diagnostic feedback string.
     * @param items Target items list.
     * @param weaknesses Target weaknesses list.
     * @param strengths Target strengths list.
     */
    private void enrichFromParsedStudentAnswers(String studentText, String feedback, List<SubmissionItemResponse> items, List<String> weaknesses, List<String> strengths) {
        Map<Integer, String> studentAnswers = parseStudentAnswers(studentText);
        if (studentAnswers.isEmpty()) return;

        List<Integer> sortedKeys = new ArrayList<>(studentAnswers.keySet());
        java.util.Collections.sort(sortedKeys);

        String lowerFeedback = feedback != null ? feedback.toLowerCase() : "";

        for (int qNum : sortedKeys) {
            String ans = studentAnswers.get(qNum);
            boolean mentionsError = lowerFeedback.contains("q" + qNum) && (lowerFeedback.contains("incorrect") || lowerFeedback.contains("wrong") || lowerFeedback.contains("error"));
            boolean isCorrect = !mentionsError;

            String rule = "Question " + qNum + " Application";
            String explanation = isCorrect ? "Accurate application of grammatical rule." : "Needs review: see diagnostic feedback.";

            items.add(SubmissionItemResponse.builder()
                .questionNumber(qNum)
                .sentence("Question " + qNum + ": " + ans)
                .studentAnswer(ans)
                .correctAnswer(isCorrect ? ans : "See feedback")
                .isCorrect(isCorrect)
                .explanation(explanation)
                .grammarRule(rule)
                .options(new ArrayList<>())
                .build());

            if (isCorrect) {
                strengths.add(rule);
            } else {
                weaknesses.add(rule);
            }
        }
    }

    /**
     * @brief Enriches evaluation items and rule diagnostics from persisted task questions.
     * @param task Associated educational task entity.
     * @param studentText Raw submitted student text.
     * @param items Target list of evaluated items.
     * @param weaknesses Target list of detected grammar weaknesses.
     * @param strengths Target list of detected grammar strengths.
     */
    private void enrichFromTaskQuestions(Task task, String studentText, List<SubmissionItemResponse> items, List<String> weaknesses, List<String> strengths) {
        if (task.getQuestions() == null || task.getQuestions().isEmpty()) return;

        Map<Integer, String> studentAnswers = parseStudentAnswers(studentText);
        Map<Integer, String> explanations = parseAnswerKeyExplanations(task.getAnswerKey());

        for (TaskQuestion q : task.getQuestions()) {
            int qOrder = q.getQuestionOrder();
            String studentAns = studentAnswers.getOrDefault(qOrder, "");
            if (studentAns.isBlank()) {
                studentAns = "No answer";
            }
            String correctAns = q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "";
            boolean isCorrect = isAnswerMatching(studentAns, correctAns);

            String rule = q.getGrammarRule() != null && !q.getGrammarRule().isBlank()
                ? q.getGrammarRule()
                : (task.getGrammarTopic() != null ? task.getGrammarTopic() : "Grammar");

            String explanation = explanations.get(qOrder);
            if (explanation == null || explanation.isBlank()) {
                if (isCorrect) {
                    explanation = "Correct! Accurately applies the rule: " + rule + ".";
                } else {
                    explanation = "Incorrect. The expected answer is '" + correctAns + "'. Tested rule: " + rule + ".";
                }
            }

            List<String> options = parseOptions(q.getOptionsJson());

            items.add(SubmissionItemResponse.builder()
                .questionNumber(qOrder)
                .sentence(q.getQuestionText())
                .studentAnswer(studentAns)
                .correctAnswer(correctAns)
                .isCorrect(isCorrect)
                .explanation(explanation)
                .grammarRule(rule)
                .options(options)
                .build());

            if (isCorrect) {
                strengths.add(rule);
            } else {
                weaknesses.add(rule);
            }
        }
    }

    /**
     * @brief Parses option choices from serialized JSON string.
     * @param optionsJson Serialized options JSON.
     * @return List of option strings.
     */
    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) return new ArrayList<>();
        List<String> list = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(optionsJson);
            if (root.isArray()) {
                for (JsonNode n : root) {
                    list.add(n.asText());
                }
            }
        } catch (Exception ignored) {
            log.debug("Failed to parse options JSON: {}", optionsJson);
        }
        return list;
    }

    /**
     * @brief Checks if a student's answer matches the target answer key, accounting for case, punctuation, and variants.
     * @param studentAns The student's submitted response.
     * @param correctAns The expected reference answer.
     * @return True if the response is considered a valid match.
     */
    private boolean isAnswerMatching(String studentAns, String correctAns) {
        if (studentAns == null || correctAns == null) return false;
        String s = normalizeText(studentAns);
        String c = normalizeText(correctAns);
        if (s.isEmpty() && c.isEmpty()) return true;
        if (s.isEmpty() || c.isEmpty()) return false;
        if (s.equals(c)) return true;

        if (correctAns.contains("/")) {
            for (String part : correctAns.split("/")) {
                if (s.equals(normalizeText(part))) return true;
            }
        }
        if (correctAns.contains(",")) {
            for (String part : correctAns.split(",")) {
                if (s.equals(normalizeText(part))) return true;
            }
        }
        return false;
    }

    /**
     * @brief Normalizes raw response strings by stripping punctuation, extra whitespace, and ordinal indicators.
     * @param text Raw response string.
     * @return Lowercase normalized token string.
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.trim().toLowerCase()
            .replaceAll("^(?:q\\d+[:.)\\-\\s]+|\\d+[:.)\\-\\s]+)", "")
            .replaceAll("[.,!?;:'\"()]", "")
            .trim();
    }

    /**
     * @brief Parses student responses into question-indexed lookup map.
     * @param studentText Raw multi-line submitted text.
     * @return Map of question index to student answer.
     */
    private Map<Integer, String> parseStudentAnswers(String studentText) {
        Map<Integer, String> answers = new HashMap<>();
        if (studentText == null || studentText.isBlank()) return answers;

        Pattern inlinePattern = Pattern.compile("(?:Q|Question\\s*)?(\\d+)[:.)\\-\\s]+(.*?)(?=(?:\\s+(?:Q|Question\\s*)?\\d+[:.)\\-\\s]+)|$)", Pattern.CASE_INSENSITIVE);
        Matcher inlineMatcher = inlinePattern.matcher(studentText);
        int matchCount = 0;
        Map<Integer, String> inlineAnswers = new HashMap<>();
        while (inlineMatcher.find()) {
            try {
                int qNum = Integer.parseInt(inlineMatcher.group(1));
                String ans = inlineMatcher.group(2).trim();
                inlineAnswers.put(qNum, ans);
                matchCount++;
            } catch (NumberFormatException ignored) {
            }
        }
        if (matchCount > 1) {
            return inlineAnswers;
        }

        String[] lines = studentText.split("\\r?\\n");
        Pattern pattern = Pattern.compile("^(?:Q|Question\\s*)?(\\d+)[:.)\\-\\s]+(.*)$", Pattern.CASE_INSENSITIVE);

        int fallbackIdx = 1;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isBlank()) continue;
            Matcher matcher = pattern.matcher(trimmed);
            if (matcher.find()) {
                try {
                    int qNum = Integer.parseInt(matcher.group(1));
                    String ans = matcher.group(2).trim();
                    answers.put(qNum, ans);
                } catch (NumberFormatException ignored) {
                    answers.putIfAbsent(fallbackIdx++, trimmed);
                }
            } else {
                answers.putIfAbsent(fallbackIdx++, trimmed);
            }
        }
        return answers;
    }

    /**
     * @brief Parses pedagogical explanation notes from persisted answer key JSON string.
     * @param answerKeyJson Serialized answer key JSON.
     * @return Map of question order index to explanation text.
     */
    private Map<Integer, String> parseAnswerKeyExplanations(String answerKeyJson) {
        Map<Integer, String> explanations = new HashMap<>();
        if (answerKeyJson == null || answerKeyJson.isBlank()) return explanations;
        try {
            JsonNode root = objectMapper.readTree(answerKeyJson);
            if (root.isArray()) {
                int idx = 1;
                for (JsonNode node : root) {
                    int order = node.path("questionOrder").asInt(node.path("questionId").asInt(idx++));
                    String exp = node.path("explanation").asText("");
                    if (!exp.isBlank()) {
                        explanations.put(order, exp);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return explanations;
    }

    /**
     * @brief Enriches evaluated question items from adaptive CAT testing session state.
     * @param session Adaptive testing session state entity.
     * @param items Target list of evaluated items.
     * @param weaknesses Target list of detected grammar weaknesses.
     * @param strengths Target list of detected grammar strengths.
     */
    private void enrichFromCatSession(SessionState session, List<SubmissionItemResponse> items, List<String> weaknesses, List<String> strengths) {
        try {
            JsonNode root = objectMapper.readTree(session.getAnswersJson());
            if (root.isArray()) {
                int idx = 1;
                for (JsonNode node : root) {
                    int qNum = idx++;
                    String qText = node.path("questionText").asText("Diagnostic adaptive question " + qNum);
                    String ans = node.path("answer").asText("");
                    String corr = node.path("correctAnswer").asText("");
                    boolean isCorr = node.path("isCorrect").asBoolean(false);
                    String rule = node.path("grammarRule").asText("Adaptive grammar assessment");
                    int diff = node.path("difficulty").asInt(2);

                    String exp = isCorr
                        ? "Correct! Demonstrated mastery at CAT difficulty level " + diff + "/4. Tested rule: " + rule + "."
                        : "Incorrect. The expected answer is '" + corr + "'. Tested rule: " + rule + ".";

                    List<String> options = new ArrayList<>();
                    if (node.has("options") && node.get("options").isArray()) {
                        for (JsonNode opt : node.get("options")) {
                            options.add(opt.asText());
                        }
                    }

                    items.add(SubmissionItemResponse.builder()
                        .questionNumber(qNum)
                        .sentence(qText)
                        .studentAnswer(ans)
                        .correctAnswer(corr)
                        .isCorrect(isCorr)
                        .explanation(exp)
                        .grammarRule(rule)
                        .options(options)
                        .build());

                    if (isCorr) {
                        strengths.add(rule);
                    } else {
                        weaknesses.add(rule);
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * @brief Extracts sentence-level corrections, weaknesses, and strengths from grading rubric map.
     * @param rubric Evaluation rubric map.
     * @param corrections Target list of sentence corrections.
     * @param weaknesses Target list of weaknesses.
     * @param strengths Target list of strengths.
     * @param items Target list of evaluated question items.
     */
    @SuppressWarnings("unchecked")
    private void extractFromRubric(Map<String, Object> rubric, List<SentenceCorrectionResponse> corrections, List<String> weaknesses, List<String> strengths, List<SubmissionItemResponse> items) {
        Object corrObj = rubric.get("corrections");
        if (corrObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    corrections.add(SentenceCorrectionResponse.builder()
                        .original(m.get("original") != null ? String.valueOf(m.get("original")) : "")
                        .corrected(m.get("corrected") != null ? String.valueOf(m.get("corrected")) : "")
                        .explanation(m.get("explanation") != null ? String.valueOf(m.get("explanation")) : "")
                        .grammarRule(m.get("grammarRule") != null ? String.valueOf(m.get("grammarRule")) : "Grammar & Style")
                        .build());
                }
            }
        }

        Object wObj = rubric.get("weaknesses");
        if (wObj instanceof List<?> list) {
            list.forEach(w -> weaknesses.add(String.valueOf(w)));
        }

        Object sObj = rubric.get("strengths");
        if (sObj instanceof List<?> list) {
            list.forEach(s -> strengths.add(String.valueOf(s)));
        }

        Object itemsObj = rubric.get("items");
        if (itemsObj instanceof List<?> list && items.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    items.add(SubmissionItemResponse.builder()
                        .questionNumber(m.get("questionNumber") instanceof Number n ? n.intValue() : items.size() + 1)
                        .sentence(m.get("sentence") != null ? String.valueOf(m.get("sentence")) : "")
                        .studentAnswer(m.get("studentAnswer") != null ? String.valueOf(m.get("studentAnswer")) : "")
                        .correctAnswer(m.get("correctAnswer") != null ? String.valueOf(m.get("correctAnswer")) : "")
                        .isCorrect(Boolean.TRUE.equals(m.get("isCorrect")))
                        .explanation(m.get("explanation") != null ? String.valueOf(m.get("explanation")) : "")
                        .grammarRule(m.get("grammarRule") != null ? String.valueOf(m.get("grammarRule")) : "")
                        .build());
                }
            }
        }
    }

    /**
     * @brief Extracts corrections and diagnostic notes from serialized JSON in AI feedback if present.
     * @param aiFeedback Raw AI feedback text.
     * @param corrections Target list of sentence corrections.
     * @param weaknesses Target list of weaknesses.
     * @param strengths Target list of strengths.
     */
    private void extractFromFeedback(String aiFeedback, List<SentenceCorrectionResponse> corrections, List<String> weaknesses, List<String> strengths) {
        if (aiFeedback == null || !aiFeedback.trim().startsWith("{")) return;
        try {
            JsonNode root = objectMapper.readTree(aiFeedback);
            if (root.has("corrections") && corrections.isEmpty()) {
                for (JsonNode cNode : root.path("corrections")) {
                    corrections.add(SentenceCorrectionResponse.builder()
                        .original(cNode.path("original").asText(""))
                        .corrected(cNode.path("corrected").asText(""))
                        .explanation(cNode.path("explanation").asText(""))
                        .grammarRule(cNode.path("grammarRule").asText("Grammar & Style"))
                        .build());
                }
            }
            if (root.has("weaknesses") && weaknesses.isEmpty()) {
                for (JsonNode w : root.path("weaknesses")) {
                    weaknesses.add(w.asText());
                }
            }
            if (root.has("strengths") && strengths.isEmpty()) {
                for (JsonNode s : root.path("strengths")) {
                    strengths.add(s.asText());
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * @brief Generates high-level diagnostic summary text.
     * @param response Submission response DTO.
     * @param items List of evaluated sentence items.
     * @param corrections List of sentence corrections.
     * @return Diagnostic summary sentence.
     */
    private String buildSummary(SubmissionResultResponse response, List<SubmissionItemResponse> items, List<SentenceCorrectionResponse> corrections) {
        if (!items.isEmpty()) {
            long correct = items.stream().filter(SubmissionItemResponse::isCorrect).count();
            int total = items.size();
            long percent = Math.round(((double) correct / total) * 100);
            return "You answered " + correct + " out of " + total + " questions correctly (" + percent + "% accuracy).";
        }
        if (!corrections.isEmpty()) {
            return "Essay evaluated against CEFR criteria. Identified " + corrections.size() + " sentence-level corrections to enhance accuracy and style.";
        }
        return "Submission evaluated successfully. Overall score: " + Math.round(response.getEffectiveScore() != null ? response.getEffectiveScore() : 0.0) + "/100.";
    }

    /**
     * @brief Generates actionable personalized recommendations from diagnostic strengths and weaknesses.
     * @param response Submission response DTO.
     * @param weaknesses Detected grammar weaknesses.
     * @param strengths Demonstrated grammar strengths.
     * @return Recommendation narrative string.
     */
    private String buildRecommendations(SubmissionResultResponse response, List<String> weaknesses, List<String> strengths) {
        if (!weaknesses.isEmpty()) {
            return "Identified areas for practice: " + String.join(", ", weaknesses) + ". Focus on reviewing verb conjugation patterns and clause structure to solidify accuracy.";
        }
        if (!strengths.isEmpty()) {
            return "Solid mastery demonstrated across: " + String.join(", ", strengths) + ". Continue with advanced tasks to expand grammatical range.";
        }
        return "Great effort! Review the detailed explanations and continue regular daily practice to advance your proficiency.";
    }

    /**
     * @brief Resolves suggested next syllabus modules to close detected grammar gaps.
     * @param response Submission response DTO.
     * @param weaknesses Detected grammar weaknesses.
     * @return List of suggested follow-up study topics.
     */
    private List<String> buildSuggestedTopics(SubmissionResultResponse response, List<String> weaknesses) {
        if (!weaknesses.isEmpty()) {
            return weaknesses;
        }
        if (response.getGrammarTopic() != null && !response.getGrammarTopic().isBlank()) {
            return List.of(response.getGrammarTopic() + " (Advanced Practice)");
        }
        return List.of("Grammar & Sentence Structure");
    }

    /**
     * @brief Updates student progress records reflecting actual question attempts and errors.
     * @param student The student submitting the work.
     * @param grammarTopic Grammar topic or skill category.
     * @param score Numerical AI score (0-100).
     * @param response Evaluated submission response containing parsed items.
     * @param task Associated educational task entity if present.
     */
    private void recordSubmissionProgress(User student, String grammarTopic, Double score, SubmissionResultResponse response, Task task) {
        int attempts = 5;
        if (task != null && task.getQuestions() != null && !task.getQuestions().isEmpty()) {
            attempts = task.getQuestions().size();
        } else if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            attempts = response.getItems().size();
        }

        int errors;
        if (score != null) {
            double normalizedScore = Math.max(0.0, Math.min(100.0, score));
            errors = (int) Math.round(attempts * (1.0 - (normalizedScore / 100.0)));
        } else if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            errors = (int) response.getItems().stream().filter(it -> !it.isCorrect()).count();
        } else {
            errors = 0;
        }

        progressService.updateFromTaskSubmission(student, grammarTopic, attempts, errors);
    }
}
