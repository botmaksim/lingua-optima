package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.Submission;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ProgressService {

    private final ProgressRecordRepository progressRecordRepository;
    private final GroupStudentRepository groupStudentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public ProgressRecord updateFromSubmission(User student, String grammarTopic, boolean isCorrect) {
        final String topic = (grammarTopic == null || grammarTopic.isBlank()) ? "General Grammar" : grammarTopic;

        ProgressRecord record = progressRecordRepository.findByStudentAndGrammarTopic(student, topic)
            .orElseGet(() -> ProgressRecord.builder()
                .student(student)
                .grammarTopic(topic)
                .totalAttempts(0)
                .errorCount(0)
                .masteryScore(0.0)
                .updatedAt(LocalDateTime.now())
                .build());

        record.setTotalAttempts(record.getTotalAttempts() + 1);
        if (!isCorrect) {
            record.setErrorCount(record.getErrorCount() + 1);
        }

        double mastery = (double) (record.getTotalAttempts() - record.getErrorCount()) / record.getTotalAttempts();
        record.setMasteryScore(Math.round(mastery * 100.0) / 100.0);
        record.setUpdatedAt(LocalDateTime.now());

        ProgressRecord saved = progressRecordRepository.save(record);
        checkCefrLevelUp(student);
        return saved;
    }

    @Transactional
    public void checkCefrLevelUp(User student) {
        if (student.getCefrLevel() == CefrLevel.C1) {
            return; // Highest level
        }

        // 7-day cooldown check
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
        if (ratio >= 0.80) { // 80%+ topics mastered at 85%+
            student.setLevelUpSuggestedAt(LocalDateTime.now());
            userRepository.save(student);

            CefrLevel next = student.getCefrLevel().getNextLevel();
            notificationService.send(student,
                "🌟 You mastered " + student.getCefrLevel() + "! Ready to level up to " + next + "?",
                NotificationType.CONTEXTUAL);
            log.info("Level up suggested to {} for student {}", next, student.getEmail());
        }
    }

    @Transactional
    public void confirmLevelUp(User student) {
        CefrLevel next = student.getCefrLevel().getNextLevel();
        student.setCefrLevel(next);
        student.setLevelUpSuggestedAt(null);
        userRepository.save(student);
        notificationService.send(student,
            "🎉 Congratulations! Your CEFR level has been upgraded to " + next + ".",
            NotificationType.SYSTEM);
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getProgressForStudent(UUID studentId) {
        return progressRecordRepository.findByStudentId(studentId).stream()
            .map(ProgressResponse::fromEntity)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProgressResponse> getGapsForStudent(UUID studentId) {
        return progressRecordRepository.findByStudentIdAndMasteryScoreLessThan(studentId, 0.6).stream()
            .map(ProgressResponse::fromEntity)
            .collect(Collectors.toList());
    }

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
