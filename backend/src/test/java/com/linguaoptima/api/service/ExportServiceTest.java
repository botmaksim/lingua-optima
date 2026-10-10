/**
 * @file ExportServiceTest.java
 * @brief Unit and slice test suite for ExportService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Group;
import com.linguaoptima.api.domain.GroupStudent;
import com.linguaoptima.api.domain.ProgressRecord;
import com.linguaoptima.api.domain.Submission;
import com.linguaoptima.api.domain.Task;
import com.linguaoptima.api.domain.TaskAssignment;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubmissionType;
import com.linguaoptima.api.domain.enums.TaskType;
import com.linguaoptima.api.dto.response.GroupReportResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import com.linguaoptima.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    /** @brief Test fixture or mock dependency for task assignment repository. */
    @Mock
    private TaskAssignmentRepository taskAssignmentRepository;

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
        teacher = User.builder().id(UUID.randomUUID()).fullName("Professor Smith").role(Role.TEACHER).build();
        otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        student = User.builder().id(UUID.randomUUID()).fullName("Student Sam").email("sam@lingua.com").cefrLevel(CefrLevel.B1).build();
        group = Group.builder().id(UUID.randomUUID()).name("Group 1").teacher(teacher).build();
    }

    /**
     * @brief Verifies unit test scenario: generate group report csv and pdf with homework assignments and submissions.
     */
    @Test
    void testGenerateGroupReportCsvAndPdf() {
        GroupStudent gs = GroupStudent.builder().group(group).student(student).isActive(true).build();
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs));

        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Past Simple")
            .type(TaskType.MCQ)
            .cefrLevel(CefrLevel.B1)
            .totalPoints(100)
            .createdAt(LocalDateTime.now())
            .build();

        TaskAssignment a1 = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task)
            .student(student)
            .assignedBy(teacher)
            .status(AssignmentStatus.SUBMITTED)
            .attemptsUsed(1)
            .maxAttempts(1)
            .dueDate(LocalDateTime.now().plusDays(2))
            .createdAt(LocalDateTime.now())
            .build();

        Submission sub = Submission.builder()
            .assignment(a1)
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .aiScore(85.0)
            .overrideScore(90.0)
            .teacherComment("Well done! Excellent comprehension of irregular past forms and very thorough syntactic accuracy throughout the whole task.")
            .aiFeedback("Good effort. 9 out of 10 correct.")
            .submittedAt(LocalDateTime.now())
            .build();

        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of(sub));
        when(taskAssignmentRepository.findByStudentIdsWithTaskAndStudent(List.of(student.getId()))).thenReturn(List.of(a1));

        byte[] csv = exportService.generateGroupReport(group.getId(), "csv", teacher);
        assertNotNull(csv);
        assertTrue(csv.length > 0);

        byte[] pdf = exportService.generateGroupReport(group.getId(), "pdf", teacher);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        GroupReportResponse preview = exportService.getGroupReportData(group.getId(), teacher);
        assertNotNull(preview);
        assertEquals(group.getName(), preview.getGroupName());
        assertEquals(1, preview.getHomeworkReports().size());
        assertEquals("Past Simple", preview.getHomeworkReports().get(0).getGrammarTopic());
    }

    /**
     * @brief Verifies unit test scenario: generate group report with overdue and pending homework assignments without submissions.
     */
    @Test
    void testGenerateGroupReportWithOverdueAndPendingAssignments() {
        User student2 = User.builder().id(UUID.randomUUID()).fullName("Student Alice").email("alice@lingua.com").cefrLevel(CefrLevel.B2).build();
        GroupStudent gs1 = GroupStudent.builder().group(group).student(student).isActive(true).build();
        GroupStudent gs2 = GroupStudent.builder().group(group).student(student2).isActive(true).build();

        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(group.getId())).thenReturn(List.of(gs1, gs2));

        Task task1 = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Conditionals")
            .type(TaskType.GAP_FILL)
            .cefrLevel(CefrLevel.B2)
            .totalPoints(50)
            .build();

        TaskAssignment aOverdue = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task1)
            .student(student)
            .assignedBy(teacher)
            .status(AssignmentStatus.PENDING)
            .attemptsUsed(0)
            .maxAttempts(1)
            .dueDate(LocalDateTime.now().minusDays(1)) // Overdue!
            .build();

        TaskAssignment aPending = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task1)
            .student(student2)
            .assignedBy(teacher)
            .status(AssignmentStatus.PENDING)
            .attemptsUsed(0)
            .maxAttempts(0) // Unlimited
            .dueDate(LocalDateTime.now().plusDays(3)) // Future
            .build();

        Task task2 = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Relative Clauses")
            .type(TaskType.MCQ)
            .cefrLevel(CefrLevel.B1)
            .totalPoints(100)
            .build();

        TaskAssignment aOnlyStudent1 = TaskAssignment.builder()
            .id(UUID.randomUUID())
            .task(task2)
            .student(student)
            .assignedBy(teacher)
            .status(AssignmentStatus.PENDING)
            .build();

        when(submissionRepository.findActiveGroupSubmissions(group.getId())).thenReturn(List.of());
        when(taskAssignmentRepository.findByStudentIdsWithTaskAndStudent(List.of(student.getId(), student2.getId())))
            .thenReturn(List.of(aOverdue, aPending, aOnlyStudent1));

        byte[] pdf = exportService.generateGroupReport(group.getId(), "pdf", teacher);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        byte[] csv = exportService.generateGroupReport(group.getId(), "csv", teacher);
        assertNotNull(csv);
        assertTrue(csv.length > 0);
    }

    /**
     * @brief Verifies unit test scenario: generate group report with null teacher and empty assignments.
     */
    @Test
    void testGenerateGroupReportWithNullTeacherAndEmptyAssignments() {
        Group nullTeacherGroup = Group.builder().id(UUID.randomUUID()).name("No Teacher Group").teacher(null).build();
        User student2 = User.builder().id(UUID.randomUUID()).fullName("Student Bob").email("bob@lingua.com").cefrLevel(CefrLevel.A2).build();
        GroupStudent gs = GroupStudent.builder().group(nullTeacherGroup).student(student2).isActive(true).build();

        when(groupRepository.findById(nullTeacherGroup.getId())).thenReturn(Optional.of(nullTeacherGroup));
        when(groupStudentRepository.findByGroupIdAndIsActiveTrue(nullTeacherGroup.getId())).thenReturn(List.of(gs));
        when(submissionRepository.findActiveGroupSubmissions(nullTeacherGroup.getId())).thenReturn(List.of());
        when(taskAssignmentRepository.findByStudentIdsWithTaskAndStudent(List.of(student2.getId()))).thenReturn(List.of());

        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        byte[] csv = exportService.generateGroupReport(nullTeacherGroup.getId(), "csv", admin);
        assertNotNull(csv);
        byte[] pdf = exportService.generateGroupReport(nullTeacherGroup.getId(), "pdf", admin);
        assertNotNull(pdf);
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
     * @brief Verifies unit test scenario: generate student report csv and pdf with topic mastery and submissions.
     */
    @Test
    void testGenerateStudentReportCsvAndPdf() {
        ProgressRecord pr = ProgressRecord.builder().student(student).grammarTopic("Passive").totalAttempts(5).masteryScore(0.8).build();
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(progressRecordRepository.findByStudent(student)).thenReturn(List.of(pr));

        Task task = Task.builder()
            .id(UUID.randomUUID())
            .grammarTopic("Passive Voice")
            .type(TaskType.REWRITE)
            .totalPoints(100)
            .build();
        TaskAssignment assignment = TaskAssignment.builder()
            .task(task)
            .student(student)
            .build();

        Submission sub = Submission.builder()
            .assignment(assignment)
            .student(student)
            .submissionType(SubmissionType.TEXT)
            .aiScore(88.0)
            .teacherComment("Good syntax")
            .submittedAt(LocalDateTime.now())
            .build();

        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of(sub));

        byte[] csv = exportService.generateStudentReport(student.getId(), "csv", teacher);
        assertNotNull(csv);
        assertTrue(csv.length > 0);

        byte[] pdf = exportService.generateStudentReport(student.getId(), "pdf", teacher);
        assertNotNull(pdf);
        assertTrue(pdf.length > 0);

        User admin = User.builder().id(UUID.randomUUID()).role(Role.ADMIN).build();
        byte[] adminPdf = exportService.generateStudentReport(student.getId(), "pdf", admin);
        assertNotNull(adminPdf);

        byte[] selfPdf = exportService.generateStudentReport(student.getId(), "pdf", student);
        assertNotNull(selfPdf);
    }

    /**
     * @brief Verifies unit test scenario: generate student report with empty records and empty submissions.
     */
    @Test
    void testGenerateStudentReportWithEmptyRecordsAndSubmissions() {
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(progressRecordRepository.findByStudent(student)).thenReturn(List.of());
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of());

        byte[] pdf = exportService.generateStudentReport(student.getId(), "pdf", teacher);
        assertNotNull(pdf);
        byte[] csv = exportService.generateStudentReport(student.getId(), "csv", teacher);
        assertNotNull(csv);
    }

    /**
     * @brief Verifies unit test scenario: generate student report forbidden when teacher does not teach student.
     */
    @Test
    void testGenerateStudentReportForbidden() {
        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(otherTeacher)).thenReturn(List.of());
        assertThrows(ForbiddenException.class,
            () -> exportService.generateStudentReport(student.getId(), "csv", otherTeacher));
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
        assertThrows(RuntimeException.class, () -> exportService.generateGroupCsv(GroupReportResponse.builder().build()));
        assertThrows(RuntimeException.class, () -> exportService.generateGroupPdf(GroupReportResponse.builder().build()));

        when(userRepository.findById(student.getId())).thenReturn(Optional.of(student));
        when(groupRepository.findByTeacher(teacher)).thenReturn(List.of(group));
        when(groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(group.getId(), student.getId())).thenReturn(true);
        when(progressRecordRepository.findByStudent(student)).thenReturn(java.util.Collections.singletonList(null));
        when(submissionRepository.findByStudentIdOrderBySubmittedAtDesc(student.getId())).thenReturn(List.of());
        assertThrows(RuntimeException.class, () -> exportService.generateStudentReport(student.getId(), "csv", teacher));
        assertThrows(RuntimeException.class, () -> exportService.generateStudentReport(student.getId(), "pdf", teacher));
    }
}
