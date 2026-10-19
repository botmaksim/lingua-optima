/**
 * @file SubmissionService.java
 * @brief Service responsible for student homework submissions, OCR processing, and teacher grading overrides.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
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
import com.linguaoptima.api.domain.enums.TaskType;
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
        String provider = student.getPreferredProvider() != null
            ? "AI_" + student.getPreferredProvider().name()
            : "AI_GEMINI";
        boolean isSpeaking = "SPEAKING".equalsIgnoreCase(request.getType())
                || (assignment != null && assignment.getTask() != null && assignment.getTask().getType() == TaskType.SPEAKING)
                || (request.getText() != null && request.getText().startsWith("[SPEAKING_PRACTICE]"));

        if ("ESSAY".equalsIgnoreCase(request.getType())) {
            scoring = scoringService.scoreEssay(request.getText(), student.getCefrLevel().name(), student);
        } else if (isSpeaking) {
            scoring = scoringService.scoreSpeakingTask(request.getText(), answerKey, student);
        } else {
            scoring = scoringService.scoreGrammarTask(request.getText(), answerKey, student);
        }

        long estimatedTokens = UsageService.estimateTokens(request.getText()) + 600L;
        usageService.consumeTokens(student, estimatedTokens);

        Double score = scoringService.extractScore(scoring);
        String feedback = String.valueOf(scoring.getOrDefault("feedback", "Submission evaluated successfully."));
        String feedbackToStore = feedback;
        try {
            feedbackToStore = objectMapper.writeValueAsString(scoring);
        } catch (Exception ignored) {}

        Submission submission = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .studentText(request.getText())
            .aiScore(score)
            .aiFeedback(feedbackToStore)
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
        recordSubmissionProgress(student, grammarTopic, response.getEffectiveScore(), response, currentTask);
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
    /** @brief Maximum allowed photo attachments per single homework submission. */
    public static final int MAX_HOMEWORK_PHOTOS = 5;

    /** @brief Maximum allowed aggregate byte size for multi-photo homework submissions (25 MB). */
    public static final long MAX_AGGREGATE_PHOTO_BYTES = 25 * 1024 * 1024L;

    /** @brief Maximum allowed single photo byte size (10 MB). */
    public static final long MAX_SINGLE_PHOTO_BYTES = 10 * 1024 * 1024L;

    /**
     * @brief Processes single or multiple uploaded homework photos via in-memory OCR and scores the extracted text.
     *
     * In accordance with Zero-Retention OCR design, the uploaded image bytes are passed
     * directly to OCR and immediately garbage collected; only the recognized text is stored.
     *
     * @param files List of multipart image files containing handwritten or printed text.
     * @param assignmentId Optional task assignment identifier.
     * @param student The student submitting the photo.
     * @return SubmissionResultResponse DTO containing extracted text and grading results.
     * @throws OcrException if images exceed limit, are invalid, or recognition fails.
     * @throws ResourceNotFoundException if assignment is not found.
     * @throws ForbiddenException if assignment does not belong to the student.
     */
    @Transactional
    public SubmissionResultResponse submitImages(List<MultipartFile> files, UUID assignmentId, User student) {
        List<MultipartFile> validFiles = (files != null)
            ? files.stream().filter(f -> f != null && !f.isEmpty()).toList()
            : List.of();

        if (validFiles.isEmpty()) {
            throw new OcrException("No image provided. Please select a valid photo.");
        }

        if (validFiles.size() > MAX_HOMEWORK_PHOTOS) {
            throw new OcrException("Maximum " + MAX_HOMEWORK_PHOTOS + " photos allowed per submission.");
        }

        long totalBytes = validFiles.stream().mapToLong(MultipartFile::getSize).sum();
        if (totalBytes > MAX_AGGREGATE_PHOTO_BYTES) {
            throw new OcrException("Total images size exceeds 25MB limit.");
        }

        for (MultipartFile f : validFiles) {
            if (f.getSize() > MAX_SINGLE_PHOTO_BYTES) {
                String name = f.getOriginalFilename() != null ? f.getOriginalFilename() : "Uploaded photo";
                throw new OcrException("Image '" + name + "' exceeds 10MB limit.");
            }
        }

        usageService.incrementOcr(student, validFiles.size());
        usageService.incrementEvaluation(student);

        List<String> pageTexts = new ArrayList<>();
        for (MultipartFile file : validFiles) {
            byte[] imageBytes;
            try {
                imageBytes = file.getBytes();
            } catch (IOException e) {
                throw new OcrException("Failed to read image file.", e);
            }
            String extracted = ocrService.extractText(imageBytes);
            if (extracted != null && !extracted.isBlank()) {
                pageTexts.add(extracted.trim());
            }
        }

        if (pageTexts.isEmpty()) {
            throw new OcrException("Image is unclear, please try again.");
        }

        String extractedText;
        if (pageTexts.size() == 1) {
            extractedText = pageTexts.get(0);
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < pageTexts.size(); i++) {
                sb.append("--- Page ").append(i + 1).append(" ---\n")
                  .append(pageTexts.get(i)).append("\n\n");
            }
            extractedText = sb.toString().trim();
        }

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
        String feedbackToStore = feedback;
        try {
            feedbackToStore = objectMapper.writeValueAsString(scoring);
        } catch (Exception ignored) {}

        Submission submission = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.IMAGE)
            .studentText(extractedText)
            .aiScore(score)
            .aiFeedback(feedbackToStore)
            .providerUsed("TESSERACT_OCR+AI")
            .submittedAt(LocalDateTime.now())
            .build();

        Submission saved = submissionRepository.save(submission);

        SubmissionResultResponse response = SubmissionResultResponse.fromEntity(saved);
        response.setRubric(scoring);
        enrichSubmissionResult(response, saved, scoring);

        Task currentTask = (assignment != null) ? assignment.getTask() : null;
        recordSubmissionProgress(student, grammarTopic, response.getEffectiveScore(), response, currentTask);
        gamificationService.onSubmissionCompleted(student);

        return response;
    }

    /**
     * @brief Processes a single uploaded homework photo via in-memory OCR.
     * @param file The multipart image file containing handwritten or printed text.
     * @param assignmentId Optional task assignment identifier.
     * @param student The student submitting the photo.
     * @return SubmissionResultResponse DTO containing extracted text and grading results.
     */
    @Transactional
    public SubmissionResultResponse submitImage(MultipartFile file, UUID assignmentId, User student) {
        return submitImages(file != null ? List.of(file) : List.of(), assignmentId, student);
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

        Map<String, Object> feedbackMap = new LinkedHashMap<>();
        if (submission.getAiFeedback() != null && submission.getAiFeedback().trim().startsWith("{")) {
            try {
                feedbackMap = objectMapper.readValue(submission.getAiFeedback(), new TypeReference<Map<String, Object>>() {});
            } catch (Exception ignored) {}
        } else if (submission.getAiFeedback() != null) {
            feedbackMap.put("feedback", submission.getAiFeedback());
        }

        if (request.getFeedback() != null) {
            feedbackMap.put("feedback", request.getFeedback());
        }
        if (request.getCorrections() != null) {
            feedbackMap.put("corrections", request.getCorrections());
        }
        if (request.getItems() != null) {
            feedbackMap.put("items", request.getItems());
        }

        if (!feedbackMap.isEmpty()) {
            try {
                submission.setAiFeedback(objectMapper.writeValueAsString(feedbackMap));
            } catch (Exception e) {
                if (request.getFeedback() != null) {
                    submission.setAiFeedback(request.getFeedback());
                }
            }
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

        Task currentTask = (saved.getAssignment() != null) ? saved.getAssignment().getTask() : null;
        String grammarTopic = currentTask != null ? currentTask.getGrammarTopic() : "General";
        recordSubmissionProgress(saved.getStudent(), grammarTopic, request.getOverrideScore(), response, currentTask);

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
        List<String> rawWeaknesses = new ArrayList<>();
        List<String> rawStrengths = new ArrayList<>();

        Map<Integer, SubmissionItemResponse> aiItemsMap = resolveAiItemsMap(rubric, submission.getAiFeedback());

        // 1. Check CAT Session
        if (submission.getAssignment() != null) {
            Optional<SessionState> sessionOpt = sessionStateRepository.findByAssignmentId(submission.getAssignment().getId());
            if (sessionOpt.isPresent() && sessionOpt.get().getAnswersJson() != null && !sessionOpt.get().getAnswersJson().isBlank()) {
                enrichFromCatSession(sessionOpt.get(), items, rawWeaknesses, rawStrengths);
            }
        }

        // 2. Check Task Questions (MCQ, GAP_FILL, REWRITE, READING, etc.)
        if (items.isEmpty()) {
            Task task = null;
            if (submission.getAssignment() != null && submission.getAssignment().getTask() != null) {
                task = submission.getAssignment().getTask();
            } else if (response.getTaskId() != null) {
                task = taskRepository.findById(response.getTaskId()).orElse(null);
            }
            if (task != null) {
                enrichFromTaskQuestions(task, submission.getStudentText(), items, rawWeaknesses, rawStrengths, aiItemsMap);
            }
        }

        // 3. Extract from Rubric
        if (rubric != null) {
            extractFromRubric(rubric, corrections, rawWeaknesses, rawStrengths, items);
        }

        // 4. Synthesize questions from studentText only if items still empty and student text contains explicit question markers (e.g. Q1:, Question 1:)
        if (items.isEmpty() && submission.getStudentText() != null && hasQuestionMarkers(submission.getStudentText())) {
            enrichFromParsedStudentAnswers(submission.getStudentText(), submission.getAiFeedback(), items, rawWeaknesses, rawStrengths);
        }

        // 5. Extract from Feedback if it contains JSON
        extractFromFeedback(submission.getAiFeedback(), corrections, rawWeaknesses, rawStrengths, items, response);

        if (!items.isEmpty()) {
            int totalWeight = items.stream().mapToInt(it -> (it.getPoints() != null && it.getPoints() > 0) ? it.getPoints() : 1).sum();
            int earnedWeight = items.stream().filter(SubmissionItemResponse::isCorrect).mapToInt(it -> (it.getPoints() != null && it.getPoints() > 0) ? it.getPoints() : 1).sum();
            double objectiveScore = totalWeight > 0
                ? Math.round(((double) earnedWeight / totalWeight) * 1000.0) / 10.0
                : 0.0;

            if (submission.getOverrideScore() == null) {
                if (submission.getAiScore() != null) {
                    submission.setAiScore(objectiveScore);
                    try {
                        submissionRepository.save(submission);
                    } catch (Exception e) {
                        log.debug("Failed to persist updated submission score: {}", e.getMessage());
                    }
                    response.setScore(objectiveScore);
                }
                response.setEffectiveScore(objectiveScore);
            } else {
                response.setScore(objectiveScore);
                response.setEffectiveScore(submission.getOverrideScore());
            }
        }

        // Consolidate Diagnostics with Strict Mutual Exclusivity and Question Analytics
        Map<String, RuleStat> ruleStats = new LinkedHashMap<>();
        int attemptedCount = 0;
        int correctCount = 0;
        int skippedCount = 0;

        if (!items.isEmpty()) {
            for (SubmissionItemResponse item : items) {
                String rule = item.getGrammarRule();
                if (rule == null || rule.isBlank()) {
                    rule = response.getGrammarTopic() != null && !response.getGrammarTopic().isBlank()
                        ? response.getGrammarTopic()
                        : "Target Grammar Structure";
                }
                rule = rule.trim();

                RuleStat stat = ruleStats.computeIfAbsent(rule, RuleStat::new);
                stat.total++;

                boolean isSkipped = item.isSkipped() || isSkippedAnswer(item.getStudentAnswer());
                if (isSkipped) {
                    stat.skipped++;
                    skippedCount++;
                } else {
                    attemptedCount++;
                    stat.attempted++;
                    if (item.isCorrect()) {
                        stat.correct++;
                        correctCount++;
                    } else {
                        stat.incorrect++;
                    }
                }
            }
        }

        // 1. Weaknesses: Rules where mistakes occurred (sorted by errors descending)
        List<String> dedupWeaknesses = new ArrayList<>();
        if (!ruleStats.isEmpty()) {
            dedupWeaknesses = ruleStats.values().stream()
                .filter(stat -> stat.incorrect > 0)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.incorrect, a.incorrect);
                    if (cmp != 0) return cmp;
                    return Double.compare(b.errorRate(), a.errorRate());
                })
                .map(stat -> stat.ruleName)
                .collect(Collectors.toList());
        }

        for (SentenceCorrectionResponse corr : corrections) {
            if (corr.getGrammarRule() != null && !corr.getGrammarRule().isBlank()) {
                String cRule = corr.getGrammarRule().trim();
                if (dedupWeaknesses.stream().noneMatch(w -> w.equalsIgnoreCase(cRule))) {
                    dedupWeaknesses.add(cRule);
                }
            }
        }

        for (String rw : rawWeaknesses) {
            if (rw != null && !rw.isBlank()) {
                String trimmed = rw.trim();
                if (dedupWeaknesses.stream().noneMatch(w -> w.equalsIgnoreCase(trimmed))) {
                    dedupWeaknesses.add(trimmed);
                }
            }
        }

        // 2. Skipped Rules: Rules where questions were skipped, but NO errors occurred
        List<String> skippedRules = new ArrayList<>();
        if (!ruleStats.isEmpty()) {
            final List<String> currentWeaknesses = dedupWeaknesses;
            skippedRules = ruleStats.values().stream()
                .filter(stat -> stat.skipped > 0 && stat.incorrect == 0)
                .map(stat -> stat.ruleName)
                .filter(r -> currentWeaknesses.stream().noneMatch(w -> w.equalsIgnoreCase(r)))
                .collect(Collectors.toList());
        }

        // 3. Strengths (Mastered Concepts):
        // Demonstrated complete mastery: at least 1 correct answer, ZERO errors, ZERO skipped!
        List<String> dedupStrengths = new ArrayList<>();
        if (!ruleStats.isEmpty()) {
            dedupStrengths = ruleStats.values().stream()
                .filter(stat -> stat.correct > 0 && stat.incorrect == 0 && stat.skipped == 0)
                .map(stat -> stat.ruleName)
                .collect(Collectors.toList());
        } else {
            for (String rs : rawStrengths) {
                if (rs != null && !rs.isBlank()) {
                    String trimmed = rs.trim();
                    if (dedupStrengths.stream().noneMatch(s -> s.equalsIgnoreCase(trimmed))) {
                        dedupStrengths.add(trimmed);
                    }
                }
            }
        }

        // STRICT INVARIANT: Strengths and Weaknesses MUST be completely disjoint!
        final List<String> finalWeaknesses = dedupWeaknesses;
        dedupStrengths.removeIf(s -> finalWeaknesses.stream().anyMatch(w -> w.equalsIgnoreCase(s.trim())));

        // 4. Recommendations and Suggested Topics
        String summary = buildSummary(response, items, corrections, attemptedCount, correctCount, skippedCount);
        String recommendations = buildRecommendations(response, dedupWeaknesses, dedupStrengths, skippedRules);
        List<String> suggestedTopics = buildSuggestedTopics(response, dedupWeaknesses, skippedRules);

        double attemptAccuracy = attemptedCount > 0
            ? Math.round(((double) correctCount / attemptedCount) * 1000.0) / 10.0
            : 0.0;

        AiAnalysisResponse analysis = AiAnalysisResponse.builder()
            .summary(summary)
            .weaknesses(dedupWeaknesses)
            .strengths(dedupStrengths)
            .recommendations(recommendations)
            .suggestedTopics(suggestedTopics)
            .attemptedCount(attemptedCount)
            .correctCount(correctCount)
            .totalQuestions(items.size())
            .skippedCount(skippedCount)
            .attemptAccuracy(attemptAccuracy)
            .build();

        response.setItems(items);
        response.setCorrections(corrections);
        response.setAiAnalysis(analysis);
    }

    /**
     * @brief Determines whether a student response represents a skipped or blank question.
     * @param answer Raw student response.
     * @return True if response represents an unattempted question.
     */
    private boolean isSkippedAnswer(String answer) {
        if (answer == null || answer.isBlank()) return true;
        String trimmed = answer.trim().toLowerCase();
        return trimmed.equals("no answer")
            || trimmed.equals("no_answer")
            || trimmed.equals("unanswered")
            || trimmed.equals("skipped")
            || trimmed.equals("none")
            || trimmed.equals("-");
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
            boolean isSkipped = isSkippedAnswer(ans);
            boolean mentionsError = lowerFeedback.contains("q" + qNum) && (lowerFeedback.contains("incorrect") || lowerFeedback.contains("wrong") || lowerFeedback.contains("error"));
            boolean isCorrect = !isSkipped && !mentionsError;

            String rule = "Question " + qNum + " Application";
            String explanation = isCorrect ? "Accurate application of grammatical rule." : (isSkipped ? "Unanswered question." : "Needs review: see diagnostic feedback.");

            items.add(SubmissionItemResponse.builder()
                .questionNumber(qNum)
                .sentence("Question " + qNum + ": " + (isSkipped ? "No answer" : ans))
                .studentAnswer(isSkipped ? "No answer" : ans)
                .correctAnswer(isCorrect ? ans : "See feedback")
                .isCorrect(isCorrect)
                .isSkipped(isSkipped)
                .explanation(explanation)
                .grammarRule(rule)
                .options(new ArrayList<>())
                .build());

            if (isCorrect) {
                strengths.add(rule);
            } else if (!isSkipped) {
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
        enrichFromTaskQuestions(task, studentText, items, weaknesses, strengths, Collections.emptyMap());
    }

    private void enrichFromTaskQuestions(Task task, String studentText, List<SubmissionItemResponse> items, List<String> weaknesses, List<String> strengths, Map<Integer, SubmissionItemResponse> aiItemsMap) {
        if (task.getQuestions() == null || task.getQuestions().isEmpty()) return;

        Map<Integer, String> studentAnswers = parseStudentAnswers(studentText);
        Map<Integer, String> explanations = parseAnswerKeyExplanations(task.getAnswerKey());

        for (TaskQuestion q : task.getQuestions()) {
            int qOrder = q.getQuestionOrder();
            String rawAns = studentAnswers.getOrDefault(qOrder, "");
            boolean isSkipped = isSkippedAnswer(rawAns);
            String studentAns = isSkipped ? "No answer" : rawAns;
            String correctAns = q.getCorrectAnswer() != null ? q.getCorrectAnswer() : "";

            List<String> options = parseOptions(q.getOptionsJson());
            boolean isMatch = !isSkipped && isAnswerMatching(studentAns, correctAns);

            SubmissionItemResponse aiItem = (aiItemsMap != null) ? aiItemsMap.get(qOrder) : null;
            boolean isOpenEnded = options.isEmpty() || task.getType() == TaskType.REWRITE
                || task.getType() == TaskType.OPEN_BRACKETS || task.getType() == TaskType.SHORT_ANSWER
                || task.getType() == TaskType.READING;

            boolean isCorrect;
            if (isMatch) {
                isCorrect = true;
            } else if (!isSkipped && isOpenEnded && aiItem != null && aiItem.isCorrect()) {
                isCorrect = true;
            } else {
                isCorrect = false;
            }

            String rule = q.getGrammarRule() != null && !q.getGrammarRule().isBlank()
                ? q.getGrammarRule()
                : (task.getGrammarTopic() != null ? task.getGrammarTopic() : "Grammar");
            if (aiItem != null && aiItem.getGrammarRule() != null && !aiItem.getGrammarRule().isBlank()) {
                rule = aiItem.getGrammarRule();
            }

            String explanation = explanations.get(qOrder);
            if (aiItem != null && aiItem.getExplanation() != null && !aiItem.getExplanation().isBlank()) {
                explanation = aiItem.getExplanation();
            } else if (explanation == null || explanation.isBlank()) {
                if (isCorrect) {
                    explanation = "Correct! Accurately applies the rule: " + rule + ".";
                } else if (isSkipped) {
                    explanation = "Unanswered question. The expected answer is '" + correctAns + "'. Tested rule: " + rule + ".";
                } else {
                    explanation = "Incorrect. The expected answer is '" + correctAns + "'. Tested rule: " + rule + ".";
                }
            }

            items.add(SubmissionItemResponse.builder()
                .questionNumber(qOrder)
                .sentence(q.getQuestionText())
                .studentAnswer(studentAns)
                .correctAnswer(correctAns)
                .isCorrect(isCorrect)
                .isSkipped(isSkipped)
                .explanation(explanation)
                .grammarRule(rule)
                .points(q.getPoints())
                .options(options)
                .build());

            if (isCorrect) {
                strengths.add(rule);
            } else if (!isSkipped) {
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
     * @brief Checks if a student's answer matches the target answer key, accounting for case, punctuation, homoglyphs, and variants.
     * @param studentAns The student's submitted response.
     * @param correctAns The expected reference answer.
     * @return True if the response is considered a valid match.
     */
    private boolean isAnswerMatching(String studentAns, String correctAns) {
        if (studentAns == null || correctAns == null) return false;
        String s = normalizeText(studentAns);
        String c = normalizeText(correctAns);
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
        if (correctAns.contains(" or ")) {
            for (String part : correctAns.split(" or ")) {
                if (s.equals(normalizeText(part))) return true;
            }
        }
        return false;
    }

    /**
     * @brief Normalizes raw response strings by stripping punctuation, extra whitespace, Cyrillic lookalikes, and ordinal indicators.
     * @param text Raw response string.
     * @return Lowercase normalized token string.
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        // 1. Replace non-breaking spaces and unicode spaces with ASCII space
        String cleaned = text.replace('\u00A0', ' ')
            .replace('\u202F', ' ')
            .replace('\u200B', ' ')
            .toLowerCase()
            .trim();

        // 2. Transliterate common Cyrillic confusable homoglyphs to Latin equivalents
        cleaned = cleaned.replace('а', 'a')
            .replace('с', 'c')
            .replace('е', 'e')
            .replace('о', 'o')
            .replace('р', 'p')
            .replace('х', 'x')
            .replace('у', 'y')
            .replace('і', 'i')
            .replace('ј', 'j')
            .replace('ѕ', 's');

        // 3. Strip question numbers like Q1: or 1. or Option letters A) or B.
        cleaned = cleaned.replaceAll("^(?:q\\d+[:.)\\-\\s]+|\\d+[:.)\\-\\s]+|[a-da-d][:.)\\-\\s]+)", "");

        // 4. Strip punctuation and collapse whitespace
        return cleaned.replaceAll("[.,!?;:'\"`“”‘’()]", "")
            .replaceAll("\\s+", " ")
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
                    String rawQText = node.path("questionText").asText("");
                    String qText = !rawQText.isBlank() ? rawQText : "Diagnostic adaptive question " + qNum;
                    String ans = node.path("answer").asText("");
                    if (ans.isBlank()) {
                        ans = node.path("givenAnswer").asText("");
                    }
                    String corr = node.path("correctAnswer").asText("");
                    boolean isSkipped = isSkippedAnswer(ans);
                    boolean isCorr = !isSkipped && (node.path("isCorrect").asBoolean(false) || isAnswerMatching(ans, corr));
                    String rawRule = node.path("grammarRule").asText("");
                    String rule = !rawRule.isBlank() ? rawRule : "Adaptive grammar assessment";
                    int diff = node.path("difficulty").asInt(2);

                    String exp = isCorr
                        ? "Correct! Demonstrated mastery at CAT difficulty level " + diff + "/4. Tested rule: " + rule + "."
                        : (isSkipped
                            ? "Unanswered question. The expected answer is '" + corr + "'. Tested rule: " + rule + "."
                            : "Incorrect. The expected answer is '" + corr + "'. Tested rule: " + rule + ".");

                    List<String> options = new ArrayList<>();
                    if (node.has("options") && node.get("options").isArray()) {
                        for (JsonNode opt : node.get("options")) {
                            options.add(opt.asText());
                        }
                    } else if (node.has("optionsJson")) {
                        options = parseOptions(node.get("optionsJson").asText());
                    }

                    items.add(SubmissionItemResponse.builder()
                        .questionNumber(qNum)
                        .sentence(qText)
                        .studentAnswer(isSkipped ? "No answer" : ans)
                        .correctAnswer(corr)
                        .isCorrect(isCorr)
                        .isSkipped(isSkipped)
                        .explanation(exp)
                        .grammarRule(rule)
                        .options(options)
                        .build());

                    if (isCorr) {
                        strengths.add(rule);
                    } else if (!isSkipped) {
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
                    String studentAns = m.get("studentAnswer") != null ? String.valueOf(m.get("studentAnswer")) : "";
                    String correctAns = m.get("correctAnswer") != null ? String.valueOf(m.get("correctAnswer")) : "";
                    boolean aiIsCorrect = Boolean.TRUE.equals(m.get("isCorrect"));
                    boolean isCorrect = aiIsCorrect || isAnswerMatching(studentAns, correctAns);

                    items.add(SubmissionItemResponse.builder()
                        .questionNumber(m.get("questionNumber") instanceof Number n ? n.intValue() : items.size() + 1)
                        .sentence(m.get("sentence") != null ? String.valueOf(m.get("sentence")) : "")
                        .studentAnswer(studentAns)
                        .correctAnswer(correctAns)
                        .isCorrect(isCorrect)
                        .explanation(m.get("explanation") != null ? String.valueOf(m.get("explanation")) : "")
                        .grammarRule(m.get("grammarRule") != null ? String.valueOf(m.get("grammarRule")) : "")
                        .build());
                }
            }
        }
    }

    /**
     * @brief Resolves map of AI-evaluated question items by question number.
     * @param rubric Optional AI grading rubric map.
     * @param aiFeedback Serialized feedback JSON string.
     * @return Map of question number to evaluated item response.
     */
    private Map<Integer, SubmissionItemResponse> resolveAiItemsMap(Map<String, Object> rubric, String aiFeedback) {
        Map<Integer, SubmissionItemResponse> map = new LinkedHashMap<>();
        if (rubric != null && rubric.get("items") instanceof List<?> list) {
            for (Object obj : list) {
                if (obj instanceof Map<?, ?> m) {
                    int qNum = m.get("questionNumber") instanceof Number n ? n.intValue() : map.size() + 1;
                    boolean isCorrect = Boolean.TRUE.equals(m.get("isCorrect"));
                    String expl = m.get("explanation") != null ? String.valueOf(m.get("explanation")) : "";
                    String rule = m.get("grammarRule") != null ? String.valueOf(m.get("grammarRule")) : "";
                    map.put(qNum, SubmissionItemResponse.builder()
                        .questionNumber(qNum)
                        .isCorrect(isCorrect)
                        .explanation(expl)
                        .grammarRule(rule)
                        .build());
                }
            }
        }
        if (map.isEmpty() && aiFeedback != null && aiFeedback.trim().startsWith("{")) {
            try {
                JsonNode root = objectMapper.readTree(aiFeedback);
                if (root.has("items") && root.path("items").isArray()) {
                    for (JsonNode iNode : root.path("items")) {
                        int qNum = iNode.path("questionNumber").asInt(map.size() + 1);
                        boolean isCorrect = iNode.path("isCorrect").asBoolean(false);
                        String expl = iNode.path("explanation").asText("");
                        String rule = iNode.path("grammarRule").asText("");
                        map.put(qNum, SubmissionItemResponse.builder()
                            .questionNumber(qNum)
                            .isCorrect(isCorrect)
                            .explanation(expl)
                            .grammarRule(rule)
                            .build());
                    }
                }
            } catch (Exception ignored) {}
        }
        return map;
    }

    /**
     * @brief Extracts corrections, items, feedback, and diagnostic notes from serialized JSON in AI feedback if present.
     * @param aiFeedback Raw AI feedback text.
     * @param corrections Target list of sentence corrections.
     * @param weaknesses Target list of weaknesses.
     * @param strengths Target list of strengths.
     * @param items Target list of itemized questions.
     * @param response Optional submission response to update feedback.
     */

    private void extractFromFeedback(String aiFeedback, List<SentenceCorrectionResponse> corrections, List<String> weaknesses, List<String> strengths, List<SubmissionItemResponse> items, SubmissionResultResponse response) {
        if (aiFeedback == null || !aiFeedback.trim().startsWith("{")) return;
        try {
            JsonNode root = objectMapper.readTree(aiFeedback);
            if (root.has("feedback") && response != null) {
                String fb = root.path("feedback").asText("");
                if (!fb.isBlank()) {
                    response.setFeedback(fb);
                }
            }
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
            if (root.has("items") && items.isEmpty()) {
                for (JsonNode iNode : root.path("items")) {
                    List<String> options = new ArrayList<>();
                    if (iNode.has("options") && iNode.path("options").isArray()) {
                        for (JsonNode opt : iNode.path("options")) {
                            options.add(opt.asText());
                        }
                    }
                    items.add(SubmissionItemResponse.builder()
                        .questionNumber(iNode.path("questionNumber").asInt(items.size() + 1))
                        .sentence(iNode.path("sentence").asText(""))
                        .studentAnswer(iNode.path("studentAnswer").asText(""))
                        .correctAnswer(iNode.path("correctAnswer").asText(""))
                        .isCorrect(iNode.path("isCorrect").asBoolean(false))
                        .isSkipped(iNode.path("isSkipped").asBoolean(false))
                        .explanation(iNode.path("explanation").asText(""))
                        .grammarRule(iNode.path("grammarRule").asText(""))
                        .points(iNode.path("points").asInt(10))
                        .options(options)
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
     * @param attemptedCount Number of questions answered.
     * @param correctCount Number of questions correct.
     * @param skippedCount Number of questions skipped.
     * @return Diagnostic summary sentence.
     */
    private String buildSummary(SubmissionResultResponse response, List<SubmissionItemResponse> items, List<SentenceCorrectionResponse> corrections, int attemptedCount, int correctCount, int skippedCount) {
        if (!items.isEmpty()) {
            int total = items.size();
            long overallPercent = Math.round(((double) correctCount / total) * 100);
            if (skippedCount > 0) {
                long attemptPercent = attemptedCount > 0 ? Math.round(((double) correctCount / attemptedCount) * 100) : 0;
                return "You answered " + correctCount + " out of " + total + " questions correctly (" + overallPercent + "% overall score). Attempt accuracy: " + attemptPercent + "% (" + correctCount + " of " + attemptedCount + " answered, " + skippedCount + " skipped).";
            }
            return "You answered " + correctCount + " out of " + total + " questions correctly (" + overallPercent + "% accuracy).";
        }
        if (!corrections.isEmpty()) {
            return "Essay evaluated against CEFR criteria. Identified " + corrections.size() + " sentence-level corrections to enhance accuracy and style.";
        }
        return "Submission evaluated successfully. Overall score: " + Math.round(response.getEffectiveScore() != null ? response.getEffectiveScore() : 0.0) + "/100.";
    }

    /**
     * @brief Generates actionable personalized recommendations from diagnostic strengths, weaknesses, and skipped rules.
     * @param response Submission response DTO.
     * @param weaknesses Detected grammar weaknesses.
     * @param strengths Demonstrated grammar strengths.
     * @param skippedRules Rules where questions were left unanswered with zero errors.
     * @return Recommendation narrative string.
     */
    private String buildRecommendations(SubmissionResultResponse response, List<String> weaknesses, List<String> strengths, List<String> skippedRules) {
        if (!weaknesses.isEmpty()) {
            return "Identified areas for practice: " + String.join(", ", weaknesses) + ". Focus on reviewing core structural rules and contrastive patterns to eliminate recurring errors.";
        }
        if (!skippedRules.isEmpty()) {
            return "Good accuracy on attempted questions! To consolidate complete mastery, be sure to attempt all questions, especially in: " + String.join(", ", skippedRules) + ".";
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
     * @param skippedRules Rules where questions were left unanswered.
     * @return List of suggested follow-up study topics.
     */
    private List<String> buildSuggestedTopics(SubmissionResultResponse response, List<String> weaknesses, List<String> skippedRules) {
        if (!weaknesses.isEmpty()) {
            return weaknesses;
        }
        if (!skippedRules.isEmpty()) {
            return skippedRules;
        }
        if (response.getGrammarTopic() != null && !response.getGrammarTopic().isBlank()) {
            return List.of(response.getGrammarTopic() + " (Advanced Practice)");
        }
        return List.of("Grammar & Sentence Structure");
    }

    /**
     * @brief Helper accumulator tracking item-level performance per grammar rule.
     */
    private static class RuleStat {
        final String ruleName;
        int total = 0;
        int attempted = 0;
        int correct = 0;
        int incorrect = 0;
        int skipped = 0;

        RuleStat(String ruleName) {
            this.ruleName = ruleName;
        }

        double errorRate() {
            return attempted > 0 ? (double) incorrect / attempted : 0.0;
        }
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
        if (response != null && response.getItems() != null && !response.getItems().isEmpty()) {
            errors = (int) response.getItems().stream().filter(it -> !it.isCorrect()).count();
        } else if (score != null) {
            double normalizedScore = Math.max(0.0, Math.min(100.0, score));
            errors = (int) Math.round(attempts * (1.0 - (normalizedScore / 100.0)));
        } else {
            errors = 0;
        }

        progressService.updateFromTaskSubmission(student, grammarTopic, attempts, errors);
    }
}
