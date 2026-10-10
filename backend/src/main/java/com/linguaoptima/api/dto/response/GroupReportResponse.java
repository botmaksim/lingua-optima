/**
 * @file GroupReportResponse.java
 * @brief Response DTO representing detailed group performance report including all homework assignments and student completion.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @brief Response DTO representing detailed group performance report.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupReportResponse {

    /** @brief Group identifier. */
    private UUID groupId;
    /** @brief Group name. */
    private String groupName;
    /** @brief Teacher display name. */
    private String teacherName;
    /** @brief Timestamp when report was generated. */
    private LocalDateTime generatedAt;
    /** @brief Total active students enrolled in cohort. */
    private int activeStudentCount;
    /** @brief Total unique homework tasks assigned to cohort. */
    private int assignedHomeworkCount;
    /** @brief Total submissions received from cohort. */
    private int totalSubmissionsCount;
    /** @brief Group average percentage score across all completed submissions. */
    private Double groupAverageScore;
    /** @brief Optional period filter lower boundary. */
    private LocalDateTime fromDate;
    /** @brief Optional period filter upper boundary. */
    private LocalDateTime toDate;
    /** @brief Human-readable period label (e.g. "Last 7 Days", "All Time"). */
    private String periodLabel;
    /** @brief Summary list of enrolled students. */
    private List<StudentSummaryDto> studentSummaries;
    /** @brief Detailed breakdown for each homework assignment. */
    private List<HomeworkReportDto> homeworkReports;

    /**
     * @brief Student summary metrics within cohort.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentSummaryDto {
        /** @brief Student user identifier. */
        private UUID studentId;
        /** @brief Student display name. */
        private String studentName;
        /** @brief Student email address. */
        private String email;
        /** @brief Student CEFR proficiency level. */
        private String cefrLevel;
        /** @brief Number of completed homework assignments. */
        private int completedTasks;
        /** @brief Total assigned homework tasks. */
        private int totalTasks;
        /** @brief Average score achieved by student across submissions. */
        private Double averageScore;
        /** @brief Number of total submissions. */
        private int submissionCount;
        /** @brief Date student joined group. */
        private LocalDate joinedDate;
    }

    /**
     * @brief Homework assignment report and student performance breakdown.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HomeworkReportDto {
        /** @brief Task identifier. */
        private UUID taskId;
        /** @brief Target grammar topic. */
        private String grammarTopic;
        /** @brief Type of exercise. */
        private TaskType taskType;
        /** @brief Target CEFR level. */
        private CefrLevel cefrLevel;
        /** @brief Maximum total points for homework. */
        private Integer totalPoints;
        /** @brief Deadline date for submission. */
        private LocalDateTime dueDate;
        /** @brief Date assignment was created. */
        private LocalDateTime assignedAt;
        /** @brief Completion results for each student in the group. */
        private List<StudentHomeworkResultDto> studentResults;
    }

    /**
     * @brief Individual student completion record for a specific homework assignment.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentHomeworkResultDto {
        /** @brief Student user identifier. */
        private UUID studentId;
        /** @brief Student display name. */
        private String studentName;
        /** @brief Student email address. */
        private String email;
        /** @brief Completion status (GRADED, SUBMITTED, PENDING, OVERDUE, NOT_ASSIGNED). */
        private String status;
        /** @brief Points scored by student. */
        private Double score;
        /** @brief Maximum points for homework. */
        private Integer totalPoints;
        /** @brief Percentage score achieved. */
        private Double percentage;
        /** @brief Number of submission attempts used. */
        private int attemptsUsed;
        /** @brief Maximum allowed attempts (0 = unlimited). */
        private Integer maxAttempts;
        /** @brief Timestamp of latest submission. */
        private LocalDateTime submittedAt;
        /** @brief Educator override commentary or personalized advice. */
        private String teacherComment;
        /** @brief Automated AI diagnostic evaluation feedback. */
        private String aiFeedback;
    }
}
