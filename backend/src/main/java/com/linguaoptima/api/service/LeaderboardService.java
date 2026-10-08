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
 * Leaderboard Service:
 * STRICT ARCHITECTURAL RULE: NO GLOBAL LEADERBOARD.
 * Only group-level leaderboards for active students within a teacher's group.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final GroupRepository groupRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final SubmissionRepository submissionRepository;

    @Transactional(readOnly = true)
    public List<LeaderboardEntryResponse> getGroupLeaderboard(UUID groupId, User currentUser) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        // Security check: Must be teacher of group or active student in group
        boolean isTeacher = group.getTeacher().getId().equals(currentUser.getId());
        boolean isMember = groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(groupId, currentUser.getId());

        if (!isTeacher && !isMember) {
            throw new ForbiddenException("Access denied: You are neither the teacher nor an active student of this group.");
        }

        // Submissions since start of current week (Monday 00:00)
        LocalDateTime startOfWeek = LocalDateTime.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            .withHour(0).withMinute(0).withSecond(0).withNano(0);

        List<Submission> weeklySubmissions = submissionRepository.findActiveGroupSubmissionsSince(groupId, startOfWeek);
        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);

        // Map scores
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

        // Sort descending by score
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
