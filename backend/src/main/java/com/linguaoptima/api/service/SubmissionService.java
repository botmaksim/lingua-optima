package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.dto.request.OverrideRequest;
import com.linguaoptima.api.dto.request.TextSubmissionRequest;
import com.linguaoptima.api.dto.response.SubmissionResultResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.OcrException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final TaskAssignmentRepository taskAssignmentRepository;
    private final OCRService ocrService;
    private final ScoringService scoringService;
    private final UsageService usageService;
    private final ProgressService progressService;
    private final GamificationService gamificationService;
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

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
            assignment.setStatus(AssignmentStatus.SUBMITTED);
            taskAssignmentRepository.save(assignment);

            if (assignment.getTask() != null) {
                answerKey = assignment.getTask().getAnswerKey();
                grammarTopic = assignment.getTask().getGrammarTopic();
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

        // Update progress and streaks
        boolean passed = score != null && score >= 70.0;
        progressService.updateFromSubmission(student, grammarTopic, passed);
        gamificationService.onSubmissionCompleted(student);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
        return response;
    }

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

        // ZERO-RETENTION OCR: Process bytes in memory, then nullify
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
            assignment.setStatus(AssignmentStatus.SUBMITTED);
            taskAssignmentRepository.save(assignment);

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
            .studentText(extractedText) // Text only, NEVER the photo bytes
            .aiScore(score)
            .aiFeedback(feedback)
            .providerUsed("TESSERACT_OCR+AI")
            .submittedAt(LocalDateTime.now())
            .build();

        Submission saved = submissionRepository.save(submission);

        boolean passed = score != null && score >= 70.0;
        progressService.updateFromSubmission(student, grammarTopic, passed);
        gamificationService.onSubmissionCompleted(student);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
        return response;
    }

    @Transactional
    public SubmissionResultResponse overrideScore(UUID submissionId, OverrideRequest request, User teacher) {
        if (teacher.getRole() != Role.TEACHER && teacher.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only educators can override scores.");
        }

        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        submission.setOverrideScore(request.getOverrideScore());
        submission.setTeacherComment(request.getTeacherComment());
        Submission saved = submissionRepository.save(submission);

        if (saved.getAssignment() != null) {
            saved.getAssignment().setStatus(AssignmentStatus.GRADED);
            taskAssignmentRepository.save(saved.getAssignment());
        }

        notificationService.send(submission.getStudent(),
            "👨‍🏫 Your grade was reviewed by " + teacher.getFullName() + ". New score: " + request.getOverrideScore(),
            NotificationType.GRADE);

        return SubmissionResultResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getMySubmissions(User student) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
            .map(SubmissionResultResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SubmissionResultResponse getSubmissionById(UUID submissionId, User user) {
        Submission submission = submissionRepository.findById(submissionId)
            .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + submissionId));

        boolean isStudent = submission.getStudent().getId().equals(user.getId());
        boolean isTeacher = user.getRole() == Role.TEACHER || user.getRole() == Role.ADMIN;

        if (!isStudent && !isTeacher) {
            throw new ForbiddenException("Access denied to this submission.");
        }

        return SubmissionResultResponse.fromEntity(submission);
    }
}
