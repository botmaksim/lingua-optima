/**
 * @file ProgressServiceTest.java
 * @brief Unit and slice test suite for ProgressService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.response.ProgressResponse;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.UserRepository;
import com.linguaoptima.api.util.CefrTopicRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for ProgressService.
 */
@ExtendWith(MockitoExtension.class)
class ProgressServiceTest {

    @Mock
    private ProgressRecordRepository progressRecordRepository;
    @Mock
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ProgressService progressService;

    private User student;

    @BeforeEach
    void setUp() {
        student = User.builder()
            .id(UUID.randomUUID())
            .email("learner@lingua.com")
            .cefrLevel(CefrLevel.B1)
            .build();
    }

    /**
     * @brief Verifies unit test scenario: update from submission.
     */
    @Test
    void testUpdateFromSubmission() {
        ProgressRecord existing = ProgressRecord.builder()
            .student(student)
            .grammarTopic("Conditionals")
            .totalAttempts(4)
            .errorCount(1)
            .masteryScore(0.75)
            .build();

        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Conditionals"))
            .thenReturn(Optional.of(existing));
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgressRecord updated = progressService.updateFromSubmission(student, "Conditionals", true);
        assertEquals(5, updated.getTotalAttempts());
        assertEquals(1, updated.getErrorCount());
        assertEquals(0.8, updated.getMasteryScore());

        ProgressRecord updated2 = progressService.updateFromSubmission(student, "Conditionals", false);
        assertEquals(6, updated2.getTotalAttempts());
        assertEquals(2, updated2.getErrorCount());
    }

    /**
     * @brief Verifies unit test scenario: cefr level up triggered.
     */
    @Test
    void testCefrLevelUpTriggered() {
        List<String> b1Topics = CefrTopicRegistry.getTopicsForLevel(CefrLevel.B1);
        List<ProgressRecord> records = new ArrayList<>();
        for (String topic : b1Topics) {
            records.add(ProgressRecord.builder().grammarTopic(topic).masteryScore(0.90).build());
        }

        when(progressRecordRepository.findByStudent(student)).thenReturn(records);

        progressService.checkCefrLevelUp(student);

        assertNotNull(student.getLevelUpSuggestedAt());
        verify(userRepository).save(student);
        verify(notificationService).send(eq(student), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: cefr level up cooldown ignored.
     */
    @Test
    void testCefrLevelUpCooldownIgnored() {
        student.setLevelUpSuggestedAt(LocalDateTime.now().minusDays(3));
        progressService.checkCefrLevelUp(student);
        verify(userRepository, never()).save(any());
    }

    /**
     * @brief Verifies unit test scenario: confirm level up.
     */
    @Test
    void testConfirmLevelUp() {
        when(userRepository.save(student)).thenReturn(student);
        User updated = progressService.confirmLevelUp(student);
        assertNotNull(updated);
        assertEquals(CefrLevel.B2, updated.getCefrLevel());
        assertNull(updated.getLevelUpSuggestedAt());
        verify(userRepository).save(student);
    }

    /**
     * @brief Verifies unit test scenario: get gaps and group progress.
     */
    @Test
    void testGetGapsAndGroupProgress() {
        ProgressRecord gap = ProgressRecord.builder().grammarTopic("Weak Topic").masteryScore(0.4).build();
        when(progressRecordRepository.findByStudentIdAndMasteryScoreLessThan(student.getId(), 0.6))
            .thenReturn(List.of(gap));

        List<ProgressResponse> gaps = progressService.getGapsForStudent(student.getId());
        assertEquals(1, gaps.size());
        assertEquals("Weak Topic", gaps.get(0).getGrammarTopic());

        UUID groupId = UUID.randomUUID();
        GroupStudent gs = GroupStudent.builder().student(student).isActive(true).build();
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId)).thenReturn(List.of(gs));
        when(progressRecordRepository.findByStudentId(student.getId())).thenReturn(List.of(gap));

        List<ProgressResponse> groupProgress = progressService.getGroupProgress(groupId);
        assertEquals(1, groupProgress.size());
    }

    /**
     * @brief Verifies unit test scenario: get progress for student.
     */
    @Test
    void testGetProgressForStudent() {
        ProgressRecord pr = ProgressRecord.builder().grammarTopic("Passive").masteryScore(0.85).build();
        when(progressRecordRepository.findByStudentId(student.getId())).thenReturn(List.of(pr));

        List<ProgressResponse> res = progressService.getProgressForStudent(student.getId());
        assertEquals(1, res.size());
        assertEquals("Passive", res.get(0).getGrammarTopic());
    }

    /**
     * @brief Verifies unit test scenario: update from submission new record.
     */
    @Test
    void testUpdateFromSubmissionNewRecord() {
        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "New Topic"))
            .thenReturn(Optional.empty());
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgressRecord created = progressService.updateFromSubmission(student, "New Topic", true);
        assertNotNull(created);
        assertEquals(1, created.getTotalAttempts());
        assertEquals(0, created.getErrorCount());
        assertEquals(1.0, created.getMasteryScore());

        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "General Grammar"))
            .thenReturn(Optional.empty());
        ProgressRecord defaultTopic = progressService.updateFromSubmission(student, "  ", true);
        assertEquals("General Grammar", defaultTopic.getGrammarTopic());

        student.setCefrLevel(CefrLevel.C2);
        student.setLevelUpSuggestedAt(null);
        progressService.checkCefrLevelUp(student);

        UUID emptyGroupId = UUID.randomUUID();
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(emptyGroupId)).thenReturn(List.of());
        assertTrue(progressService.getGroupProgress(emptyGroupId).isEmpty());

        ProgressRecord nullTopicRecord = progressService.updateFromSubmission(student, null, false);
        assertEquals("General Grammar", nullTopicRecord.getGrammarTopic());

        User nullLevelStudent = User.builder().id(UUID.randomUUID()).cefrLevel(null).build();
        progressService.checkCefrLevelUp(nullLevelStudent);

        student.setCefrLevel(CefrLevel.B1);
        student.setLevelUpSuggestedAt(LocalDateTime.now().minusDays(10));
        List<String> b1Topics = CefrTopicRegistry.getTopicsForLevel(CefrLevel.B1);
        List<ProgressRecord> duplicateAndSubThreshold = new ArrayList<>();
        duplicateAndSubThreshold.add(ProgressRecord.builder().grammarTopic(b1Topics.get(0)).masteryScore(0.40).build());
        duplicateAndSubThreshold.add(ProgressRecord.builder().grammarTopic(b1Topics.get(0)).masteryScore(0.84).build());
        when(progressRecordRepository.findByStudent(student)).thenReturn(duplicateAndSubThreshold);

        progressService.checkCefrLevelUp(student);
        verify(notificationService, never()).send(eq(student), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: updateFromTaskSubmission correctly computes mastery from multi-question attempts and errors.
     */
    @Test
    void testUpdateFromTaskSubmissionMultiQuestion() {
        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Present Perfect vs Past Simple"))
            .thenReturn(Optional.empty());
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 5 questions, 1 error (80% score)
        ProgressRecord record = progressService.updateFromTaskSubmission(student, "Present Perfect vs Past Simple", 5, 1);
        assertEquals(5, record.getTotalAttempts());
        assertEquals(1, record.getErrorCount());
        assertEquals(0.80, record.getMasteryScore());

        // Subsequent task with 5 questions, 0 errors
        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Present Perfect vs Past Simple"))
            .thenReturn(Optional.of(record));

        ProgressRecord second = progressService.updateFromTaskSubmission(student, "Present Perfect vs Past Simple", 5, 0);
        assertEquals(10, second.getTotalAttempts());
        assertEquals(1, second.getErrorCount());
        assertEquals(0.90, second.getMasteryScore());

        // Test safe boundary clamping with negative errors and excessive errors
        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Boundary Topic"))
            .thenReturn(Optional.empty());

        ProgressRecord clamped = progressService.updateFromTaskSubmission(student, "Boundary Topic", 0, -2);
        assertEquals(1, clamped.getTotalAttempts());
        assertEquals(0, clamped.getErrorCount());

        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Boundary Topic Upper"))
            .thenReturn(Optional.empty());
        ProgressRecord clampedUpper = progressService.updateFromTaskSubmission(student, "Boundary Topic Upper", 3, 10);
        assertEquals(3, clampedUpper.getTotalAttempts());
        assertEquals(3, clampedUpper.getErrorCount());
    }

    /**
     * @brief Verifies unit test scenario: CEFR level up triggered with topic variations.
     */
    @Test
    void testCefrLevelUpTriggeredWithNormalizedTopicVariants() {
        student.setCefrLevel(CefrLevel.B2);
        List<ProgressRecord> records = List.of(
            ProgressRecord.builder().grammarTopic("Third & Mixed Conditionals").masteryScore(0.95).build(),
            ProgressRecord.builder().grammarTopic("Passive Voice (Continuous & Perfect)").masteryScore(0.90).build(),
            ProgressRecord.builder().grammarTopic("Reported Speech").masteryScore(0.90).build(),
            ProgressRecord.builder().grammarTopic("Wish & If Only Structures").masteryScore(0.88).build(),
            ProgressRecord.builder().grammarTopic("Modal Verbs of Deduction").masteryScore(0.92).build(),
            ProgressRecord.builder().grammarTopic("Relative Clauses (non-defining)").masteryScore(0.90).build(),
            ProgressRecord.builder().grammarTopic("Gerunds vs Infinitives").masteryScore(0.85).build()
        );

        when(progressRecordRepository.findByStudent(student)).thenReturn(records);

        progressService.checkCefrLevelUp(student);

        assertNotNull(student.getLevelUpSuggestedAt());
        verify(userRepository).save(student);
        verify(notificationService).send(eq(student), anyString(), any());
    }

    /**
     * @brief Verifies unit test scenario: normalizeTopicKey handles null and punctuation.
     */
    @Test
    void testNormalizeTopicKey() {
        assertEquals("", ProgressService.normalizeTopicKey(null));
        assertEquals("conditional mixed third", ProgressService.normalizeTopicKey("Third & Mixed Conditionals"));
        assertEquals("conditional mixed third", ProgressService.normalizeTopicKey("Third Conditional & Mixed Conditionals"));
        assertEquals("used would", ProgressService.normalizeTopicKey("Used to / Would"));
        assertEquals("used would", ProgressService.normalizeTopicKey("Used to & Would"));
    }

    /**
     * @brief Verifies unit test scenario: adjustFromOverride updates error delta without inflating total attempts.
     */
    @Test
    void testAdjustFromOverride() {
        ProgressRecord existing = ProgressRecord.builder()
            .student(student)
            .grammarTopic("Passive Voice")
            .totalAttempts(5)
            .errorCount(2)
            .masteryScore(0.60)
            .build();

        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Passive Voice"))
            .thenReturn(Optional.of(existing));
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Teacher overrides: old errors was 2, teacher marks 1 as correct, so new errors is 1
        ProgressRecord updated = progressService.adjustFromOverride(student, "Passive Voice", 2, 1);

        // Total attempts must remain 5 (no duplicate attempt inflation)
        assertEquals(5, updated.getTotalAttempts());
        // Errors adjusted by delta: 2 - 2 + 1 = 1
        assertEquals(1, updated.getErrorCount());
        // Mastery score updated: (5 - 1) / 5 = 0.80
        assertEquals(0.80, updated.getMasteryScore());
    }

    @Test
    void testAdjustFromOverride_NewRecordCreated() {
        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "Reported Speech"))
            .thenReturn(Optional.empty());
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProgressRecord created = progressService.adjustFromOverride(student, "Reported Speech", 0, 2);

        assertNotNull(created);
        assertEquals(2, created.getTotalAttempts());
        assertEquals(2, created.getErrorCount());
        assertEquals(0.0, created.getMasteryScore());
        assertEquals("Reported Speech", created.getGrammarTopic());
    }

    @Test
    void testAdjustFromOverride_NullTopicAndAttemptsExpansion() {
        ProgressRecord existing = ProgressRecord.builder()
            .student(student)
            .grammarTopic("General Grammar")
            .totalAttempts(1)
            .errorCount(0)
            .masteryScore(1.0)
            .build();

        when(progressRecordRepository.findByStudentAndGrammarTopic(student, "General Grammar"))
            .thenReturn(Optional.of(existing));
        when(progressRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // With null grammarTopic, falls back to "General Grammar", and oldErrors=0, newErrors=4 exceeds totalAttempts(1)
        ProgressRecord updated = progressService.adjustFromOverride(student, null, -1, 4);

        assertNotNull(updated);
        assertEquals(4, updated.getTotalAttempts(), "Attempts should expand to accommodate error count");
        assertEquals(4, updated.getErrorCount());
        assertEquals(0.0, updated.getMasteryScore());
    }
}

