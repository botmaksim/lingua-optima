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

@ExtendWith(MockitoExtension.class)
class ExportServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private GroupStudentRepository groupStudentRepository;
    @Mock
    private SubmissionRepository submissionRepository;
    @Mock
    private ProgressRecordRepository progressRecordRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ExportService exportService;

    private User teacher;
    private User otherTeacher;
    private User student;
    private Group group;

    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        otherTeacher = User.builder().id(UUID.randomUUID()).role(Role.TEACHER).build();
        student = User.builder().id(UUID.randomUUID()).fullName("Student Sam").email("sam@lingua.com").cefrLevel(CefrLevel.B1).build();
        group = Group.builder().id(UUID.randomUUID()).name("Group 1").teacher(teacher).build();
    }

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

    @Test
    void testGenerateGroupReportForbidden() {
        when(groupRepository.findById(group.getId())).thenReturn(Optional.of(group));
        assertThrows(ForbiddenException.class, () -> exportService.generateGroupReport(group.getId(), "csv", otherTeacher));
    }

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

    @Test
    void testGroupNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(groupRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> exportService.generateGroupReport(id, "csv", teacher));
    }

    @Test
    void testStudentNotFoundThrows() {
        UUID id = UUID.randomUUID();
        when(userRepository.findById(id)).thenReturn(Optional.empty());
        assertThrows(com.linguaoptima.api.exception.ResourceNotFoundException.class,
            () -> exportService.generateStudentReport(id, "csv", teacher));
    }
}
