/**
 * @file ExportServiceTest.java
 * @brief Unit and slice test suite for ExportService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for ExportService.
 */
@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    /** @brief Test fixture or mock dependency for group repository. */
    @Mock
    private GroupRepository groupRepository;
    /** @brief Test fixture or mock dependency for group student repository. */
    @Mock
    private GroupStudentRepository groupStudentRepository;
    /** @brief Test fixture or mock dependency for submission repository. */
    @Mock
    private SubmissionRepository submissionRepository;
    /** @brief Test fixture or mock dependency for progress record repository. */
    @Mock
    private ProgressRecordRepository progressRecordRepository;
    /** @brief Test fixture or mock dependency for user repository. */
    @Mock
    private UserRepository userRepository;

    /** @brief Test fixture or mock dependency for export service. */
    @InjectMocks
    private ExportService exportService;

    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;
    /** @brief Test fixture or mock dependency for other teacher. */
    private User otherTeacher;
    /** @brief Test fixture or mock dependency for student. */
    private User student;
    /** @brief Test fixture or mock dependency for group. */
    private Group group;

    /**
     * @brief Initializes test fixtures and mock state before each test in ExportServiceTest.
     */
    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        student = User.builder().id(UUID.randomUUID()).fullName("Student Sam").email("sam@lingua.com").cefrLevel(CefrLevel.B1).build();
        group = Group.builder().id(UUID.randomUUID()).name("Group 1").teacher(teacher).build();
    }

    /**
     * @brief Verifies unit test scenario: generate group report csv and pdf.
     */
    @Test
    void testGenerateGroupReportCsvAndPdf() {
        GroupStudent gs = GroupStudent.builder().group(group).student(student).isActive(true).build();
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs));

        byte[] csv = exportService.generateGroupReport(group.getId(), "csv", teacher);
        assertNotNull(csv);
        assertTrue(csv.length > 0);

        byte[] pdf = exportService.generateGroupReport(group.getId(), "pdf", teacher);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    /**
     * @brief Verifies unit test scenario: generate group report forbidden.
     */
    @Test
    void testGenerateGroupReportForbidden() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        assertThrows(ForbiddenException.class, () -> exportService.generateGroupReport(group.getId(), "csv", otherTeacher));
    }

    /**
     * @brief Verifies unit test scenario: generate student report csv and pdf.
     */
    @Test
    void testGenerateStudentReportCsvAndPdf() {
        ProgressRecord pr = ProgressRecord.builder().student(student).grammarTopic("Passive").totalAttempts(5).masteryScore(0.8).build();
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(progressRecordRepository.findByStudent(student)).thenReturn(List.of(pr));
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of());

        byte[] csv = exportService.generateStudentReport(student.getId(), "csv", teacher);
        assertNotNull(csv);
        assertTrue(csv.length > 0);

        byte[] pdf = exportService.generateStudentReport(student.getId(), "pdf", teacher);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    /**
     * @brief Verifies unit test scenario: group not found throws.
     */
    @Test
    void testGroupNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> exportService.generateGroupReport(id, "csv", teacher));
    }

    /**
     * @brief Verifies unit test scenario: student not found throws.
     */
    @Test
    void testStudentNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> exportService.generateStudentReport(id, "csv", teacher));
    }

    /**
     * @brief Verifies unit test scenario: CSV and PDF generation failure handling when repository throws during iteration.
     */
    @Test
    void testExportExceptionHandling() {
        GroupStudent brokenGs = GroupStudent.builder().group(group).student(null).isActive(true).build();
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(brokenGs));

        assertThrows(RuntimeException.class, () -> exportService.generateGroupReport(group.getId(), "csv", teacher));
        assertThrows(RuntimeException.class, () -> exportService.generateGroupReport(group.getId(), "pdf", teacher));

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(progressRecordRepository.findByStudent(student)).thenReturn(java.util.Collections.singletonList(null));
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> exportService.generateStudentReport(student.getId(), "csv", teacher));
        assertThrows(RuntimeException.class, () -> exportService.generateStudentReport(student.getId(), "pdf", teacher));
    }
}

