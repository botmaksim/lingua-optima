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
 * @file ProgressServiceTest.java
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

    @Test
    void testCefrLevelUpCooldownIgnored() {
        student.setLevelUpSuggestedAt(LocalDateTime.now().minusDays(3));
        progressService.checkCefrLevelUp(student);
        verify(userRepository, never()).save(any());
    }

    @Test
    void testConfirmLevelUp() {
        progressService.confirmLevelUp(student);
        assertEquals(CefrLevel.B2, student.getCefrLevel());
        assertNull(student.getLevelUpSuggestedAt());
        verify(userRepository).save(student);
    }

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

    @Test
    void testGetProgressForStudent() {
        ProgressRecord pr = ProgressRecord.builder().grammarTopic("Passive").masteryScore(0.85).build();
        when(progressRecordRepository.findByStudentId(student.getId())).thenReturn(List.of(pr));

        List<ProgressResponse> res = progressService.getProgressForStudent(student.getId());
        assertEquals(1, res.size());
        assertEquals("Passive", res.get(0).getGrammarTopic());
    }

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
    }
}
