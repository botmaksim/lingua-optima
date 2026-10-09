package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.response.LeaderboardEntryResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
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
 * @file LeaderboardService.java
 * @brief Class-scoped student leaderboard service.
 *
 * Implements privacy-preserving competitive ranking strictly scoped within individual teacher groups.
 * In accordance with architectural safety rules, global leaderboards across unaffiliated users are prohibited.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final GroupRepository groupRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final SubmissionRepository submissionRepository;

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
            String alias = (student.getDisplayAlias() != null && !student.getDisplayAlias().isBlank())
                ? student.getDisplayAlias()
                : "Linguist #" + student.getId().toString().substring(0, 8);

            leaderboard.add(LeaderboardEntryResponse.builder()
                .rank(rank++)
                .studentId(student.getId())
                .displayAlias(alias)
                .weeklyScore(Math.round(entry.getValue() * 10.0) / 10.0)
                .cefrLevel(student.getCefrLevel())
                .build());
        }

        return leaderboard;
    }
}
