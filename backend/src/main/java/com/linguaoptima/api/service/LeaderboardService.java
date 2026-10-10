/**
 * @file LeaderboardService.java
 * @brief Class-scoped student leaderboard service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Class-scoped student leaderboard service.
 *
 * Implements privacy-preserving competitive ranking strictly scoped within individual teacher groups.
 * In accordance with architectural safety rules, global leaderboards across unaffiliated users are prohibited.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    /** @brief Field representing group repository in LeaderboardService. */
    private final GroupRepository groupRepository;
    /** @brief Field representing group student repository in LeaderboardService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing submission repository in LeaderboardService. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field representing task assignment repository in LeaderboardService. */
    private final TaskAssignmentRepository taskAssignmentRepository;

    /**
     * @brief Computes weekly leaderboard rankings for active students within a designated group.
     * @param groupId Unique identifier of the group.
     * @param currentUser Authenticated user (must be the group's educator or an active student member).
     * @return Ordered list of LeaderboardEntryResponse DTOs sorted descending by weekly score.
     * @throws ResourceNotFoundException if group is not found.
     * @throws ForbiddenException if caller is neither the educator nor an active student of the group.
     */
    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getGroupLeaderboard(UUID groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        boolean isTeacher = group.getTeacher().getId().equals(currentUser.getId());
        boolean isMember = groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(groupId, currentUser.getId());

        if (!isTeacher && !isMember) {
            throw new ForbiddenException("Access denied: You are neither the teacher nor an active student of this group.");
        }

        LocalDateTime startOfWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Submission> weeklySubmissions = submissionRepository.findActiveGroupSubmissionsSince(groupId, startOfWeek);
        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);

        Map<UUID, Double> scoresByStudent = new HashMap<>();
        Map<UUID, User> userMap = new HashMap<>();

        for (GroupStudent gs : activeStudents) {
            User s = gs.getStudent();
            userMap.put(s.getId(), s);
            scoresByStudent.put(s.getId(), 0.0);
        }

        for (Submission sub : weeklySubmissions) {
            UUID sid = sub.getStudent().getId();
            if (scoresByStudent.containsKey(sid)) {
                scoresByStudent.put(sid, scoresByStudent.get(sid) + sub.getEffectiveScore());
            }
        }

        List<Map.Entry<UUID, Double>> sorted = scoresByStudent.entrySet().stream()
            .sorted(Map.Entry.<UUID, Double>comparingByValue().reversed())
            .toList();

        List<LeaderboardEntryResponse> leaderboard = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<UUID, Double> entry : sorted) {
            User student = userMap.get(entry.getKey());
            String alias;
            if (student.getDisplayAlias() != null && !student.getDisplayAlias().isBlank() && !student.getDisplayAlias().startsWith("Linguist #")) {
                alias = student.getDisplayAlias();
            } else if (student.getFullName() != null && !student.getFullName().isBlank()) {
                alias = student.getFullName();
            } else {
                alias = "Linguist #" + (student.getId() != null ? student.getId().toString().substring(0, 8) : "Learner");
            }

            // Calculate task completion progress in this teacher's cohort
            List<TaskAssignment> studentAssignments = taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(student.getId()).stream()
                .filter(a -> a.getAssignedBy() != null && a.getAssignedBy().getId().equals(group.getTeacher().getId()))
                .toList();
            int totalTasks = studentAssignments.size();
            int completedTasks = (int) studentAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.SUBMITTED || a.getStatus() == AssignmentStatus.GRADED)
                .count();
            int completionRate = totalTasks > 0 ? (int) Math.round((double) completedTasks / totalTasks * 100.0) : 0;

            // Calculate average score on group tasks
            List<Submission> allGroupSubs = submissionRepository.findActiveGroupSubmissions(groupId).stream()
                .filter(sub -> sub.getStudent().getId().equals(student.getId()))
                .toList();
            double avgScore = allGroupSubs.isEmpty() ? 0.0
                : Math.round(allGroupSubs.stream().mapToDouble(Submission::getEffectiveScore).average().orElse(0.0) * 10.0) / 10.0;

            leaderboard.add(LeaderboardEntryResponse.builder()
                .rank(rank++)
                .studentId(student.getId())
                .displayAlias(alias)
                .fullName(student.getFullName())
                .weeklyScore(Math.round(entry.getValue() * 10.0) / 10.0)
                .completedTasks(completedTasks)
                .totalTasks(totalTasks)
                .completionRate(completionRate)
                .averageScore(avgScore)
                .cefrLevel(student.getCefrLevel())
                .streakCount(student.getStreakCount())
                .build());
        }

        return leaderboard;
    }
}
