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

/**
 * @file SubmissionService.java
 * @brief Service responsible for student homework submissions, OCR processing, and teacher grading overrides.
 *
 * Implements strict Zero-Retention OCR architecture: uploaded images are processed in-memory
 * and instantly discarded, ensuring no image binary data or biometric photos are persisted.
 */
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

        boolean passed = score != null && score >= 70.0;
        progressService.updateFromSubmission(student, grammarTopic, passed);
        gamificationService.onSubmissionCompleted(student);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
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
            .studentText(extractedText)
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

    /**
     * @brief Retrieves submission history for the specified student.
     * @param student The student whose submission history is requested.
     * @return List of SubmissionResultResponse DTOs ordered by submission date descending.
     */
    @Transactional(readOnly = true)
    public List<SubmissionResultResponse> getMySubmissions(User student) {
        return submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId()).stream()
            .map(SubmissionResultResponse::fromEntity)
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

        return SubmissionResultResponse.fromEntity(submission);
    }
}
