/**
 * @file SubmissionResultResponse.java
 * @brief Response DTO representing an evaluated student submission with scores, sentence items, and AI gap analysis.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.enums.SubmissionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @brief Response DTO representing an evaluated student submission with scores, sentence items, and AI gap analysis.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResultResponse {

    /** @brief Unique identifier of the submission. */
    private UUID id;

    /** @brief Optional identifier of the linked task assignment. */
    private UUID assignmentId;

    /** @brief Identifier of the student who submitted the work. */
    private UUID studentId;

    /** @brief Submission modality (TEXT or IMAGE). */
    private SubmissionType submissionType;

    /** @brief Submitted or OCR-extracted student text. */
    private String originalText;

    /** @brief Primary score (0-100 scale). */
    private Double score;

    /** @brief Effective score resolving teacher override over AI score. */
    private Double effectiveScore;

    /** @brief Narrative AI evaluation feedback. */
    private String feedback;

    /** @brief Structured rubric breakdown map. */
    private Map<String, Object> rubric;

    /** @brief Educator manual override score, if applied. */
    private Double overrideScore;

    /** @brief Educator commentary accompanying a score override. */
    private String teacherComment;

    /** @brief Identifier of the AI provider that evaluated the submission. */
    private String providerUsed;

    /** @brief Timestamp when the submission was recorded. */
    private LocalDateTime submittedAt;

    /** @brief Unique identifier of the associated educational task, if linked. */
    private UUID taskId;

    /** @brief Format type of the associated task (MCQ, GAP_FILL, ESSAY, REWRITE, etc.). */
    private String taskType;

    /** @brief Student-facing reading context passage or assignment topic. */
    private String taskContent;

    /** @brief Targeted grammar topic or pedagogical syllabus module. */
    private String grammarTopic;

    /** @brief Target CEFR language proficiency benchmark level. */
    private String cefrLevel;

    /** @brief Itemized question-by-question or sentence-by-sentence evaluation records. */
    @Builder.Default
    private List<SubmissionItemResponse> items = new ArrayList<>();

    /** @brief Sentence-level grammatical and stylistic corrections. */
    @Builder.Default
    private List<SentenceCorrectionResponse> corrections = new ArrayList<>();

    /** @brief Synthesized AI gap analysis highlighting strengths, weaknesses, and study recommendations. */
    private AiAnalysisResponse aiAnalysis;

    /**
     * @brief Maps a domain Submission entity to a SubmissionResultResponse DTO.
     * @param submission Domain entity instance.
     * @return Mapped SubmissionResultResponse DTO or null if input is null.
     */
    public static SubmissionResultResponse fromEntity(Submission submission) {
        if (submission == null) return null;

        UUID resolvedTaskId = null;
        String resolvedTaskType = null;
        String resolvedTaskContent = null;
        String resolvedGrammarTopic = null;
        String resolvedCefr = null;

        if (submission.getAssignment() != null && submission.getAssignment().getTask() != null) {
            Task task = submission.getAssignment().getTask();
            resolvedTaskId = task.getId();
            resolvedTaskType = task.getType() != null ? task.getType().name() : null;
            resolvedTaskContent = task.getContent();
            resolvedGrammarTopic = task.getGrammarTopic();
            resolvedCefr = task.getCefrLevel() != null ? task.getCefrLevel().name() : null;
        }

        return SubmissionResultResponse.builder()
            .id(submission.getId())
            .assignmentId(submission.getAssignment() != null ? submission.getAssignment().getId() : null)
            .studentId(submission.getStudent() != null ? submission.getStudent().getId() : null)
            .submissionType(submission.getSubmissionType())
            .originalText(submission.getStudentText())
            .score(submission.getAiScore() != null ? submission.getAiScore() : submission.getEffectiveScore())
            .effectiveScore(submission.getEffectiveScore())
            .feedback(submission.getAiFeedback())
            .overrideScore(submission.getOverrideScore())
            .teacherComment(submission.getTeacherComment())
            .providerUsed(submission.getProviderUsed())
            .submittedAt(submission.getSubmittedAt())
            .taskId(resolvedTaskId)
            .taskType(resolvedTaskType)
            .taskContent(resolvedTaskContent)
            .grammarTopic(resolvedGrammarTopic)
            .cefrLevel(resolvedCefr)
            .items(new ArrayList<>())
            .corrections(new ArrayList<>())
            .build();
    }
}
