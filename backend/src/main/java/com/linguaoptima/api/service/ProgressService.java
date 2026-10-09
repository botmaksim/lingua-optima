/**
 * @file ProgressService.java
 * @brief Service managing student topic mastery, knowledge gap tracking, and CEFR level-up workflows.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.NotificationType;
import com.linguaoptima.api.dto.response.ProgressResponse;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.UserRepository;
import com.linguaoptima.api.util.CefrTopicRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Service managing student topic mastery, knowledge gap tracking, and CEFR level-up workflows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    /** @brief Field representing progress record repository in ProgressService. */
    private final ProgressRecordRepository progressRecordRepository;
    /** @brief Field representing group student repository in ProgressService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing user repository in ProgressService. */
    private final UserRepository userRepository;
    /** @brief Field representing notification service in ProgressService. */
    private final NotificationService notificationService;

    /**
     * @brief Updates topic progress from a completed submission with attempt count, error count, and mastery score.
     * @param student The student completing the exercise.
     * @param grammarTopic Grammar topic or skill category.
     * @param attemptsCount Total number of questions or items attempted in this submission.
     * @param errorsCount Total number of incorrect questions or errors in this submission.
     * @return Updated ProgressRecord entity.
     */
    @Transactional
    public ProgressRecord updateFromTaskSubmission(User student, String grammarTopic, int attemptsCount, int errorsCount) {
        final String topic = (grammarTopic == null || grammarTopic.isBlank()) ? "General Grammar" : grammarTopic;
        final int safeAttempts = Math.max(1, attemptsCount);
        final int safeErrors = Math.max(0, Math.min(safeAttempts, errorsCount));

        ProgressRecord record = progressRecordRepository.findByStudentAndGrammarTopic(student, topic)
            .orElseGet(() -> ProgressRecord.builder()
                .student(student)
                .grammarTopic(topic)
                .totalAttempts(0)
                .errorCount(0)
                .masteryScore(0.0)
                .updatedAt(LocalDateTime.now())
                .build());

        record.setTotalAttempts(record.getTotalAttempts() + safeAttempts);
        record.setErrorCount(record.getErrorCount() + safeErrors);

        double mastery = (double) (record.getTotalAttempts() - record.getErrorCount()) / record.getTotalAttempts();
        record.setMasteryScore(Math.round(mastery * 100.0) / 100.0);
        record.setUpdatedAt(LocalDateTime.now());

        ProgressRecord saved = progressRecordRepository.save(record);
        checkCefrLevelUp(student);
        return saved;
    }

    /**
     * @brief Updates topic mastery record based on a student submission answer and evaluates level-up readiness.
     * @param student The student completing the exercise.
     * @param grammarTopic Grammar topic or skill category.
     * @param isCorrect Whether the answer or submission was evaluated as correct/passed.
     * @return Updated ProgressRecord entity.
     */
    @Transactional
    public ProgressRecord updateFromSubmission(User student, String grammarTopic, boolean isCorrect) {
        return updateFromTaskSubmission(student, grammarTopic, 1, isCorrect ? 0 : 1);
    }

    /**
     * @brief Checks if student has achieved 85%+ mastery across 80%+ of CEFR level topics and triggers level-up notification.
     * @param student The student whose progress is being evaluated.
     */
    @Transactional
    public void checkCefrLevelUp(User student) {
        if (student.getCefrLevel() == CefrLevel.C2) {
            return;
        }

        if (student.getLevelUpSuggestedAt() != null &&
            student.getLevelUpSuggestedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            return;
        }

        List<String> levelTopics = CefrTopicRegistry.getTopicsForLevel(student.getCefrLevel());
        if (levelTopics.isEmpty()) return;

        List<ProgressRecord> records = progressRecordRepository.findByStudent(student);
        Map<String, Double> topicMastery = records.stream()
            .collect(Collectors.toMap(ProgressRecord::getGrammarTopic, ProgressRecord::getMasteryScore, (a, b) -> b));

        int masteredCount = 0;
        for (String topic : levelTopics) {
            Double mastery = topicMastery.get(topic);
            if (mastery != null && mastery >= 0.85) {
                masteredCount++;
            }
        }

        double ratio = (double) masteredCount / levelTopics.size();
        if (ratio >= 0.80) {
            student.setLevelUpSuggestedAt(LocalDateTime.now());
            userRepository.save(student);

            CefrLevel next = student.getCefrLevel().getNextLevel();
            notificationService.send(student,
                "🌟 You mastered " + student.getCefrLevel() + "! Ready to level up to " + next + "?",
                NotificationType.CONTEXTUAL);
            log.info("Level up suggested to {} for student {}", next, student.getEmail());
        }
    }

    /**
     * @brief Confirms student acceptance of a suggested CEFR level upgrade.
     * @param student Authenticated student accepting the upgrade.
     * @return Updated User entity with newly assigned CEFR level.
     */
    @Transactional
    public User confirmLevelUp(User student) {
        CefrLevel next = student.getCefrLevel().getNextLevel();
        student.setCefrLevel(next);
        student.setLevelUpSuggestedAt(null);
        User saved = userRepository.save(student);
        notificationService.send(saved,
            "🎉 Congratulations! Your CEFR level has been upgraded to " + next + ".",
            NotificationType.SYSTEM);
        return saved;
    }

    /**
     * @brief Retrieves all topic progress records for the specified student.
     * @param studentId Unique identifier of the student.
     * @return List of ProgressResponse DTOs.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getProgressForStudent(UUID studentId) {
        return progressRecordRepository.findByStudentId(studentId).stream()
            .map(ProgressResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @brief Retrieves knowledge gaps (topics with mastery below 60%) for the specified student.
     * @param studentId Unique identifier of the student.
     * @return List of ProgressResponse DTOs representing deficient topics.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getGapsForStudent(UUID studentId) {
        return progressRecordRepository.findByStudentIdAndMasteryScoreLessThan(studentId, 0.6).stream()
            .map(ProgressResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * @brief Aggregates progress metrics across all active students within a class group.
     * @param groupId Unique identifier of the group.
     * @return Aggregated list of ProgressResponse DTOs per topic.
     */
    @Transactional(readOnly = true)
    public List<ProgressResponse> getGroupProgress(UUID groupId) {
        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);
        List<UUID> activeStudentIds = activeStudents.stream()
            .map(gs -> gs.getStudent().getId())
            .toList();

        if (activeStudentIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<ProgressRecord>> recordsByTopic = new HashMap<>();
        for (UUID sid : activeStudentIds) {
            List<ProgressRecord> records = progressRecordRepository.findByStudentId(sid);
            for (ProgressRecord pr : records) {
                recordsByTopic.computeIfAbsent(pr.getGrammarTopic(), k -> new ArrayList<>()).add(pr);
            }
        }

        List<ProgressResponse> result = new ArrayList<>();
        recordsByTopic.forEach((topic, records) -> {
            int totalAttempts = records.stream().mapToInt(ProgressRecord::getTotalAttempts).sum();
            int errorCount = records.stream().mapToInt(ProgressRecord::getErrorCount).sum();
            double avgMastery = records.stream().mapToDouble(ProgressRecord::getMasteryScore).average().orElse(0.0);

            result.add(ProgressResponse.builder()
                .grammarTopic(topic)
                .totalAttempts(totalAttempts)
                .errorCount(errorCount)
                .masteryScore(Math.round(avgMastery * 100.0) / 100.0)
                .updatedAt(LocalDateTime.now())
                .build());
        });

        return result;
    }
}
