/**
 * @file ExportService.java
 * @brief Report generation service exporting student and class performance to CSV and PDF formats.
 */
package com.linguaoptima.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.domain.enums.AssignmentStatus;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.response.GroupReportResponse;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.TaskAssignmentRepository;
import com.linguaoptima.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Report generation service exporting student and class performance to CSV and PDF formats.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    /** @brief Field representing group repository in ExportService. */
    private final GroupRepository groupRepository;
    /** @brief Field representing group student repository in ExportService. */
    private final GroupStudentRepository groupStudentRepository;
    /** @brief Field representing submission repository in ExportService. */
    private final SubmissionRepository submissionRepository;
    /** @brief Field representing task assignment repository in ExportService. */
    private final TaskAssignmentRepository taskAssignmentRepository;
    /** @brief Field representing progress record repository in ExportService. */
    private final ProgressRecordRepository progressRecordRepository;
    /** @brief Field representing user repository in ExportService. */
    private final UserRepository userRepository;

    /**
     * @brief Generates an export report for a group in either CSV or PDF format with homework breakdown.
     * @param groupId Unique identifier of the group.
     * @param format Desired export format ("csv" or "pdf").
     * @param teacher Educator requesting the report.
     * @return Byte array containing raw report file bytes.
     * @throws ResourceNotFoundException if group is not found.
     * @throws ForbiddenException if teacher does not own the group.
     */
    @Transactional(readOnly = true)
    public byte[] generateGroupReport(UUID groupId, String format, User teacher) {
        return generateGroupReport(groupId, format, null, null, teacher);
    }

    /**
     * @brief Generates an export report for a group filtered by a specific date period.
     * @param groupId Unique identifier of the group.
     * @param format Desired export format ("csv" or "pdf").
     * @param from Lower timestamp bound for submissions and assignments.
     * @param to Upper timestamp bound for submissions and assignments.
     * @param teacher Educator requesting the report.
     * @return Byte array containing raw report file bytes.
     */
    @Transactional(readOnly = true)
    public byte[] generateGroupReport(UUID groupId, String format, LocalDateTime from, LocalDateTime to, User teacher) {
        GroupReportResponse reportData = getGroupReportData(groupId, from, to, teacher);

        if ("csv".equalsIgnoreCase(format)) {
            return generateGroupCsv(reportData);
        } else {
            return generateGroupPdf(reportData);
        }
    }

    /**
     * @brief Builds comprehensive structured report data for a group, including roster and detailed homework results.
     * @param groupId Unique identifier of the group.
     * @param teacher Educator requesting the report data.
     * @return Populated GroupReportResponse DTO.
     */
    @Transactional(readOnly = true)
    public GroupReportResponse getGroupReportData(UUID groupId, User teacher) {
        return getGroupReportData(groupId, null, null, teacher);
    }

    /**
     * @brief Builds structured report data for a group filtered by date period.
     * @param groupId Unique identifier of the group.
     * @param from Optional lower date bound.
     * @param to Optional upper date bound.
     * @param teacher Educator requesting the report.
     * @return Populated GroupReportResponse DTO.
     */
    @Transactional(readOnly = true)
    public GroupReportResponse getGroupReportData(UUID groupId, LocalDateTime from, LocalDateTime to, User teacher) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (teacher.getRole() != Role.ADMIN && (group.getTeacher() == null || !group.getTeacher().getId().equals(teacher.getId()))) {
            throw new ForbiddenException("Access denied: You are not the teacher of this group.");
        }

        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);
        List<UUID> studentIds = activeStudents.stream().map(gs -> gs.getStudent().getId()).toList();

        List<Submission> allGroupSubs = submissionRepository.findActiveGroupSubmissions(groupId);

        // Filter submissions by date range if specified
        if (from != null) {
            final LocalDateTime f = from;
            allGroupSubs = allGroupSubs.stream()
                .filter(s -> s.getSubmittedAt() != null && !s.getSubmittedAt().isBefore(f))
                .toList();
        }
        if (to != null) {
            final LocalDateTime t = to;
            allGroupSubs = allGroupSubs.stream()
                .filter(s -> s.getSubmittedAt() != null && !s.getSubmittedAt().isAfter(t))
                .toList();
        }

        // Map student to assignments
        Map<UUID, List<TaskAssignment>> studentAssignmentsMap = new LinkedHashMap<>();
        List<TaskAssignment> allCohortAssignments = new ArrayList<>();
        if (!studentIds.isEmpty()) {
            List<TaskAssignment> fetched = taskAssignmentRepository.findByStudentIdsWithTaskAndStudent(studentIds);
            if (fetched != null && !fetched.isEmpty()) {
                allCohortAssignments.addAll(fetched);
            } else {
                for (UUID sId : studentIds) {
                    List<TaskAssignment> studentList = taskAssignmentRepository.findByStudentIdOrderByCreatedAtDesc(sId);
                    if (studentList != null) {
                        allCohortAssignments.addAll(studentList);
                    }
                }
            }

            allCohortAssignments = allCohortAssignments.stream()
                .filter(a -> a.getAssignedBy() != null && group.getTeacher() != null && a.getAssignedBy().getId().equals(group.getTeacher().getId()))
                .toList();

            if (from != null) {
                final LocalDateTime f = from;
                allCohortAssignments = allCohortAssignments.stream()
                    .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isBefore(f))
                    .toList();
            }
            if (to != null) {
                final LocalDateTime t = to;
                allCohortAssignments = allCohortAssignments.stream()
                    .filter(a -> a.getCreatedAt() != null && !a.getCreatedAt().isAfter(t))
                    .toList();
            }

            for (TaskAssignment a : allCohortAssignments) {
                studentAssignmentsMap.computeIfAbsent(a.getStudent().getId(), k -> new ArrayList<>()).add(a);
            }
        }

        // Student summaries
        List<GroupReportResponse.StudentSummaryDto> summaries = new ArrayList<>();
        for (GroupStudent gs : activeStudents) {
            User s = gs.getStudent();
            List<TaskAssignment> studentAssignments = studentAssignmentsMap.getOrDefault(s.getId(), List.of());
            int totalTasks = studentAssignments.size();
            int completedTasks = (int) studentAssignments.stream()
                .filter(a -> a.getStatus() == AssignmentStatus.SUBMITTED || a.getStatus() == AssignmentStatus.GRADED)
                .count();

            List<Submission> sSubs = allGroupSubs.stream()
                .filter(sub -> sub.getStudent().getId().equals(s.getId()))
                .toList();
            Double avgScore = sSubs.isEmpty() ? null : sSubs.stream().mapToDouble(Submission::getEffectiveScore).average().orElse(0.0);

            summaries.add(GroupReportResponse.StudentSummaryDto.builder()
                .studentId(s.getId())
                .studentName(s.getFullName())
                .email(s.getEmail())
                .cefrLevel(s.getCefrLevel().name())
                .completedTasks(completedTasks)
                .totalTasks(totalTasks)
                .averageScore(avgScore != null ? Math.round(avgScore * 10.0) / 10.0 : null)
                .submissionCount(sSubs.size())
                .joinedDate(gs.getJoinedAt().toLocalDate())
                .build());
        }

        // Unique tasks
        List<Task> uniqueTasks = allCohortAssignments.stream()
            .map(TaskAssignment::getTask)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Task::getId, t -> t, (t1, t2) -> t1, LinkedHashMap::new))
            .values()
            .stream()
            .toList();

        List<GroupReportResponse.HomeworkReportDto> homeworkReports = new ArrayList<>();
        for (Task task : uniqueTasks) {
            int maxPts = task.getTotalPoints() != null ? task.getTotalPoints() : 100;
            List<TaskAssignment> taskAssignments = allCohortAssignments.stream()
                .filter(a -> a.getTask() != null && a.getTask().getId().equals(task.getId()))
                .toList();

            LocalDateTime dueDate = taskAssignments.stream()
                .map(TaskAssignment::getDueDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);

            LocalDateTime assignedAt = taskAssignments.stream()
                .map(TaskAssignment::getCreatedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(task.getCreatedAt());

            List<GroupReportResponse.StudentHomeworkResultDto> results = new ArrayList<>();
            for (GroupStudent gs : activeStudents) {
                User s = gs.getStudent();
                TaskAssignment assignment = taskAssignments.stream()
                    .filter(a -> a.getStudent().getId().equals(s.getId()))
                    .findFirst()
                    .orElse(null);

                if (assignment != null) {
                    List<Submission> aSubs = allGroupSubs.stream()
                        .filter(sub -> sub.getAssignment() != null && sub.getAssignment().getId().equals(assignment.getId()))
                        .toList();
                    Submission latestSub = aSubs.isEmpty() ? null : aSubs.get(0);

                    if (latestSub != null) {
                        double effScore = latestSub.getEffectiveScore();
                        double pct = maxPts > 0 ? (effScore / maxPts) * 100.0 : effScore;
                        String status = latestSub.getOverrideScore() != null ? "GRADED" : assignment.getStatus().name();

                        results.add(GroupReportResponse.StudentHomeworkResultDto.builder()
                            .studentId(s.getId())
                            .studentName(s.getFullName())
                            .email(s.getEmail())
                            .status(status)
                            .score(effScore)
                            .totalPoints(maxPts)
                            .percentage(Math.round(pct * 10.0) / 10.0)
                            .attemptsUsed(assignment.getAttemptsUsed())
                            .maxAttempts(assignment.getMaxAttempts())
                            .submittedAt(latestSub.getSubmittedAt())
                            .teacherComment(latestSub.getTeacherComment())
                            .aiFeedback(latestSub.getAiFeedback())
                            .build());
                    } else {
                        boolean isOverdue = assignment.getDueDate() != null && assignment.getDueDate().isBefore(LocalDateTime.now());
                        results.add(GroupReportResponse.StudentHomeworkResultDto.builder()
                            .studentId(s.getId())
                            .studentName(s.getFullName())
                            .email(s.getEmail())
                            .status(isOverdue ? "OVERDUE" : assignment.getStatus().name())
                            .score(null)
                            .totalPoints(maxPts)
                            .percentage(null)
                            .attemptsUsed(assignment.getAttemptsUsed())
                            .maxAttempts(assignment.getMaxAttempts())
                            .submittedAt(null)
                            .teacherComment(isOverdue ? "Deadline passed" : "Not submitted yet")
                            .aiFeedback(null)
                            .build());
                    }
                } else {
                    results.add(GroupReportResponse.StudentHomeworkResultDto.builder()
                        .studentId(s.getId())
                        .studentName(s.getFullName())
                        .email(s.getEmail())
                        .status("NOT_ASSIGNED")
                        .score(null)
                        .totalPoints(maxPts)
                        .percentage(null)
                        .attemptsUsed(0)
                        .maxAttempts(1)
                        .submittedAt(null)
                        .teacherComment("—")
                        .aiFeedback(null)
                        .build());
                }
            }

            homeworkReports.add(GroupReportResponse.HomeworkReportDto.builder()
                .taskId(task.getId())
                .grammarTopic(task.getGrammarTopic())
                .taskType(task.getType())
                .cefrLevel(task.getCefrLevel())
                .totalPoints(maxPts)
                .dueDate(dueDate)
                .assignedAt(assignedAt)
                .studentResults(results)
                .build());
        }

        Double groupAvg = allGroupSubs.isEmpty() ? 0.0 : allGroupSubs.stream().mapToDouble(Submission::getEffectiveScore).average().orElse(0.0);

        String periodLabel = "All Time";
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (from != null && to != null) {
            periodLabel = from.format(dtf) + " to " + to.format(dtf);
        } else if (from != null) {
            periodLabel = "Since " + from.format(dtf);
        } else if (to != null) {
            periodLabel = "Until " + to.format(dtf);
        }

        return GroupReportResponse.builder()
            .groupId(group.getId())
            .groupName(group.getName())
            .teacherName(group.getTeacher() != null ? group.getTeacher().getFullName() : "N/A")
            .generatedAt(LocalDateTime.now())
            .fromDate(from)
            .toDate(to)
            .periodLabel(periodLabel)
            .activeStudentCount(activeStudents.size())
            .assignedHomeworkCount(uniqueTasks.size())
            .totalSubmissionsCount(allGroupSubs.size())
            .groupAverageScore(Math.round(groupAvg * 10.0) / 10.0)
            .studentSummaries(summaries)
            .homeworkReports(homeworkReports)
            .build();
    }

    /**
     * @brief Generates a detailed progress report for an individual student in CSV or PDF format.
     * @param studentId Unique identifier of the student.
     * @param format Desired export format ("csv" or "pdf").
     * @param teacher Educator requesting the student report.
     * @return Byte array of the generated report.
     * @throws ResourceNotFoundException if student is not found.
     */
    @Transactional(readOnly = true)
    public byte[] generateStudentReport(UUID studentId, String format, User teacher) {
        User student = userRepository.findById(studentId)
            .orElseThrow(() -> new ResourceNotFoundException("Student not found: " + studentId));

        if (teacher.getRole() != Role.ADMIN && !teacher.getId().equals(studentId)) {
            boolean isTeacherOfStudent = groupRepository.findByTeacher(teacher).stream()
                .anyMatch(g -> groupStudentRepository.existsByGroupIdAndStudentIdAndIsActiveTrue(g.getId(), studentId));
            if (!isTeacherOfStudent) {
                throw new ForbiddenException("Access denied: You do not have permission to view this student's report.");
            }
        }

        List<ProgressRecord> records = progressRecordRepository.findByStudent(student);
        List<Submission> submissions = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);

        if ("csv".equalsIgnoreCase(format)) {
            return generateStudentCsv(student, records, submissions);
        } else {
            return generateStudentPdf(student, records, submissions);
        }
    }

    /**
     * @brief Generates CSV bytes for a group roster and detailed homework results.
     * @param report GroupReportResponse report data.
     * @return UTF-8 byte array of the CSV document.
     */
    byte[] generateGroupCsv(GroupReportResponse report) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Lingua Optima - Group Performance Report", report.getGroupName()});
            writer.writeNext(new String[]{"Teacher", report.getTeacherName()});
            writer.writeNext(new String[]{"Generated", report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))});
            writer.writeNext(new String[]{"Active Students", String.valueOf(report.getActiveStudentCount())});
            writer.writeNext(new String[]{"Assigned Homeworks", String.valueOf(report.getAssignedHomeworkCount())});
            writer.writeNext(new String[]{"Total Submissions", String.valueOf(report.getTotalSubmissionsCount())});
            writer.writeNext(new String[]{"Group Average Score", report.getGroupAverageScore() != null ? String.format("%.1f%%", report.getGroupAverageScore()) : "0.0%"});
            writer.writeNext(new String[]{""});

            // Section 1: Roster Summary
            writer.writeNext(new String[]{"--- COHORT ROSTER SUMMARY ---"});
            writer.writeNext(new String[]{"Student Name", "Email", "CEFR Level", "Tasks Completed", "Average Score", "Submissions", "Joined Date"});

            for (GroupReportResponse.StudentSummaryDto s : report.getStudentSummaries()) {
                String avgScoreStr = s.getAverageScore() != null ? String.format("%.1f%%", s.getAverageScore()) : "N/A";
                writer.writeNext(new String[]{
                    s.getStudentName(),
                    s.getEmail(),
                    s.getCefrLevel(),
                    s.getTotalTasks() > 0 ? (s.getCompletedTasks() + "/" + s.getTotalTasks()) : "0/0",
                    avgScoreStr,
                    String.valueOf(s.getSubmissionCount()),
                    s.getJoinedDate() != null ? s.getJoinedDate().toString() : ""
                });
            }

            // Section 2: Detailed Homework Completion
            writer.writeNext(new String[]{""});
            writer.writeNext(new String[]{"--- DETAILED HOMEWORK COMPLETION BY STUDENT ---"});
            writer.writeNext(new String[]{
                "Homework Topic", "Task Type", "CEFR Level", "Total Points", "Due Date",
                "Student Name", "Email", "Status", "Score", "Score (%)",
                "Attempts Used", "Max Attempts", "Submitted At", "Teacher Advice / Comment", "AI Diagnostic Feedback"
            });

            if (report.getHomeworkReports() == null || report.getHomeworkReports().isEmpty()) {
                writer.writeNext(new String[]{"No homework assignments have been assigned to this cohort yet."});
            } else {
                for (GroupReportResponse.HomeworkReportDto hw : report.getHomeworkReports()) {
                    String dueStr = hw.getDueDate() != null ? hw.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "No Deadline";
                    for (GroupReportResponse.StudentHomeworkResultDto res : hw.getStudentResults()) {
                        String scoreStr = res.getScore() != null ? String.format("%.0f / %d", res.getScore(), res.getTotalPoints()) : "—";
                        String scorePctStr = res.getPercentage() != null ? String.format("%.1f%%", res.getPercentage()) : "—";
                        String submittedAtStr = res.getSubmittedAt() != null ? res.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "—";
                        String maxAttemptsStr = res.getMaxAttempts() == null || res.getMaxAttempts() == 0 ? "Unlimited" : String.valueOf(res.getMaxAttempts());

                        writer.writeNext(new String[]{
                            hw.getGrammarTopic(),
                            hw.getTaskType() != null ? hw.getTaskType().name() : "N/A",
                            hw.getCefrLevel() != null ? hw.getCefrLevel().name() : "N/A",
                            String.valueOf(hw.getTotalPoints()),
                            dueStr,
                            res.getStudentName(),
                            res.getEmail(),
                            res.getStatus(),
                            scoreStr,
                            scorePctStr,
                            String.valueOf(res.getAttemptsUsed()),
                            maxAttemptsStr,
                            submittedAtStr,
                            res.getTeacherComment() != null ? res.getTeacherComment() : "—",
                            res.getAiFeedback() != null ? res.getAiFeedback() : "—"
                        });
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV export failed", e);
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @brief Generates PDF bytes for a group roster and detailed homework performance table.
     * @param report GroupReportResponse report data.
     * @return Byte array of the rendered PDF document.
     */
    byte[] generateGroupPdf(GroupReportResponse report) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font subSectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallItalicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

            document.add(new Paragraph("Lingua Optima - Group Performance Report", titleFont));
            document.add(new Paragraph("Group: " + report.getGroupName(), metaFont));
            document.add(new Paragraph("Educator: " + report.getTeacherName(), metaFont));
            document.add(new Paragraph("Generated: " + report.getGeneratedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), metaFont));

            String avgStr = report.getGroupAverageScore() != null ? String.format("%.1f%%", report.getGroupAverageScore()) : "0.0%";
            document.add(new Paragraph(String.format("Active Students: %d | Assigned Homeworks: %d | Total Submissions: %d | Group Avg Score: %s",
                report.getActiveStudentCount(), report.getAssignedHomeworkCount(), report.getTotalSubmissionsCount(), avgStr), metaFont));
            document.add(new Paragraph(" "));

            // --- SECTION 1: ROSTER SUMMARY ---
            document.add(new Paragraph("1. Cohort Performance Overview", sectionFont));
            document.add(new Paragraph(" "));

            PdfPTable rosterTable = new PdfPTable(6);
            rosterTable.setWidthPercentage(100);
            rosterTable.setWidths(new float[]{2.5f, 3.2f, 1.0f, 1.5f, 1.5f, 1.5f});
            rosterTable.addCell(createHeaderCell("Student Name", tableHeaderFont));
            rosterTable.addCell(createHeaderCell("Email", tableHeaderFont));
            rosterTable.addCell(createHeaderCell("CEFR", tableHeaderFont));
            rosterTable.addCell(createHeaderCell("Tasks Done", tableHeaderFont));
            rosterTable.addCell(createHeaderCell("Avg Score", tableHeaderFont));
            rosterTable.addCell(createHeaderCell("Joined Date", tableHeaderFont));

            for (GroupReportResponse.StudentSummaryDto s : report.getStudentSummaries()) {
                String avgScoreStr = s.getAverageScore() != null ? String.format("%.1f%%", s.getAverageScore()) : "N/A";
                rosterTable.addCell(createCell(s.getStudentName(), tableCellFont));
                rosterTable.addCell(createCell(s.getEmail(), tableCellFont));
                rosterTable.addCell(createCell(s.getCefrLevel(), tableCellFont));
                rosterTable.addCell(createCell(s.getTotalTasks() > 0 ? (s.getCompletedTasks() + "/" + s.getTotalTasks()) : "0/0", tableCellFont));
                rosterTable.addCell(createCell(avgScoreStr, tableCellFont));
                rosterTable.addCell(createCell(s.getJoinedDate() != null ? s.getJoinedDate().toString() : "—", tableCellFont));
            }
            document.add(rosterTable);
            document.add(new Paragraph(" "));

            // --- SECTION 2: HOMEWORK ASSIGNMENTS BREAKDOWN ---
            document.add(new Paragraph("2. Homework Assignments & Student Completion Breakdown", sectionFont));
            document.add(new Paragraph(" "));

            if (report.getHomeworkReports() == null || report.getHomeworkReports().isEmpty()) {
                document.add(new Paragraph("No homework assignments have been assigned to this cohort yet.", smallItalicFont));
            } else {
                for (int i = 0; i < report.getHomeworkReports().size(); i++) {
                    GroupReportResponse.HomeworkReportDto hw = report.getHomeworkReports().get(i);
                    String dueStr = hw.getDueDate() != null ? hw.getDueDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "No Deadline";

                    String taskHeader = String.format("Homework %d: %s (%s) · %s · Max: %d pts · Due: %s",
                        i + 1, hw.getGrammarTopic(), hw.getTaskType() != null ? hw.getTaskType().name() : "Task",
                        hw.getCefrLevel() != null ? hw.getCefrLevel().name() : "General", hw.getTotalPoints(), dueStr);

                    document.add(new Paragraph(taskHeader, subSectionFont));
                    document.add(new Paragraph(" "));

                    PdfPTable taskTable = new PdfPTable(6);
                    taskTable.setWidthPercentage(100);
                    taskTable.setWidths(new float[]{2.2f, 1.3f, 1.6f, 1.0f, 1.6f, 3.3f});
                    taskTable.addCell(createHeaderCell("Student Name", tableHeaderFont));
                    taskTable.addCell(createHeaderCell("Status", tableHeaderFont));
                    taskTable.addCell(createHeaderCell("Score", tableHeaderFont));
                    taskTable.addCell(createHeaderCell("Attempts", tableHeaderFont));
                    taskTable.addCell(createHeaderCell("Submitted At", tableHeaderFont));
                    taskTable.addCell(createHeaderCell("Teacher Comment / Feedback", tableHeaderFont));

                    for (GroupReportResponse.StudentHomeworkResultDto res : hw.getStudentResults()) {
                        String scoreStr = res.getScore() != null
                            ? String.format("%.0f / %d (%.0f%%)", res.getScore(), res.getTotalPoints(), res.getPercentage() != null ? res.getPercentage() : 0.0)
                            : "—";
                        String maxAttStr = res.getMaxAttempts() == null || res.getMaxAttempts() == 0 ? "∞" : String.valueOf(res.getMaxAttempts());
                        String attemptsStr = res.getAttemptsUsed() + "/" + maxAttStr;
                        String submittedAtStr = res.getSubmittedAt() != null ? res.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "—";
                        String feedbackStr = res.getTeacherComment() != null && !res.getTeacherComment().isBlank()
                            ? res.getTeacherComment()
                            : (res.getAiFeedback() != null && !res.getAiFeedback().isBlank() ? res.getAiFeedback() : "—");

                        if (feedbackStr.length() > 90) {
                            feedbackStr = feedbackStr.substring(0, 87) + "...";
                        }

                        taskTable.addCell(createCell(res.getStudentName(), tableCellFont));
                        taskTable.addCell(createCell(res.getStatus(), tableCellFont));
                        taskTable.addCell(createCell(scoreStr, tableCellFont));
                        taskTable.addCell(createCell(attemptsStr, tableCellFont));
                        taskTable.addCell(createCell(submittedAtStr, tableCellFont));
                        taskTable.addCell(createCell(feedbackStr, tableCellFont));
                    }

                    document.add(taskTable);
                    document.add(new Paragraph(" "));
                }
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return baos.toByteArray();
    }

    /**
     * @brief Generates CSV bytes detailing topic mastery metrics and submissions for an individual student.
     * @param student Target student user.
     * @param records List of ProgressRecord entities.
     * @param submissions List of recent Submission entities.
     * @return UTF-8 byte array of the CSV document.
     */
    private byte[] generateStudentCsv(User student, List<ProgressRecord> records, List<Submission> submissions) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Lingua Optima - Student Progress Report", student.getFullName(), student.getEmail()});
            writer.writeNext(new String[]{"Current CEFR Level", student.getCefrLevel().name()});
            writer.writeNext(new String[]{"Generated", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))});
            writer.writeNext(new String[]{""});

            writer.writeNext(new String[]{"--- GRAMMAR TOPIC MASTERY ---"});
            writer.writeNext(new String[]{"Grammar Topic", "Total Attempts", "Error Count", "Mastery Score (%)"});

            for (ProgressRecord pr : records) {
                writer.writeNext(new String[]{
                    pr.getGrammarTopic(),
                    String.valueOf(pr.getTotalAttempts()),
                    String.valueOf(pr.getErrorCount()),
                    String.format("%.1f%%", pr.getMasteryScore() * 100)
                });
            }

            writer.writeNext(new String[]{""});
            writer.writeNext(new String[]{"--- HOMEWORK & SUBMISSIONS HISTORY ---"});
            writer.writeNext(new String[]{"Homework Topic", "Type", "Score", "Score (%)", "Submitted At", "Teacher Advice / Comment", "AI Feedback"});

            for (Submission sub : submissions) {
                String topic = sub.getAssignment() != null && sub.getAssignment().getTask() != null
                    ? sub.getAssignment().getTask().getGrammarTopic()
                    : "Practice";
                String type = sub.getSubmissionType() != null ? sub.getSubmissionType().name() : "TEXT";
                int maxPts = sub.getAssignment() != null && sub.getAssignment().getTask() != null && sub.getAssignment().getTask().getTotalPoints() != null
                    ? sub.getAssignment().getTask().getTotalPoints()
                    : 100;
                double eff = sub.getEffectiveScore() != null ? sub.getEffectiveScore() : 0.0;
                String scoreStr = String.format("%.0f / %d", eff, maxPts);
                String scorePctStr = String.format("%.1f%%", maxPts > 0 ? (eff / maxPts) * 100 : eff);
                String submittedAtStr = sub.getSubmittedAt() != null ? sub.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "—";
                String teacherComment = sub.getTeacherComment() != null ? sub.getTeacherComment() : "—";
                String aiFeedback = sub.getAiFeedback() != null ? sub.getAiFeedback() : "—";

                writer.writeNext(new String[]{
                    topic,
                    type,
                    scoreStr,
                    scorePctStr,
                    submittedAtStr,
                    teacherComment,
                    aiFeedback
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV export failed", e);
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @brief Generates PDF bytes with progress tables and submissions overview for a student.
     * @param student Target student user.
     * @param records List of ProgressRecord entities.
     * @param submissions List of recent Submission entities.
     * @return Byte array of the rendered PDF document.
     */
    private byte[] generateStudentPdf(User student, List<ProgressRecord> records, List<Submission> submissions) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font tableCellFont = FontFactory.getFont(FontFactory.HELVETICA, 8);
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font smallItalicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

            document.add(new Paragraph("Lingua Optima - Student Progress Report", titleFont));
            document.add(new Paragraph("Student: " + student.getFullName() + " (" + student.getEmail() + ")", metaFont));
            document.add(new Paragraph("Current Level: " + student.getCefrLevel(), metaFont));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), metaFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("1. Grammar Topic Mastery Overview:", sectionFont));
            document.add(new Paragraph(" "));

            if (records == null || records.isEmpty()) {
                document.add(new Paragraph("No topic mastery records tracked yet.", smallItalicFont));
            } else {
                PdfPTable table = new PdfPTable(4);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{3.0f, 1.5f, 1.5f, 1.5f});
                table.addCell(createHeaderCell("Grammar Topic", tableHeaderFont));
                table.addCell(createHeaderCell("Attempts", tableHeaderFont));
                table.addCell(createHeaderCell("Errors", tableHeaderFont));
                table.addCell(createHeaderCell("Mastery (%)", tableHeaderFont));

                for (ProgressRecord pr : records) {
                    table.addCell(createCell(pr.getGrammarTopic(), tableCellFont));
                    table.addCell(createCell(String.valueOf(pr.getTotalAttempts()), tableCellFont));
                    table.addCell(createCell(String.valueOf(pr.getErrorCount()), tableCellFont));
                    table.addCell(createCell(String.format("%.1f%%", pr.getMasteryScore() * 100), tableCellFont));
                }
                document.add(table);
            }

            document.add(new Paragraph(" "));
            document.add(new Paragraph("2. Homework Assignments & Submissions History:", sectionFont));
            document.add(new Paragraph(" "));

            if (submissions == null || submissions.isEmpty()) {
                document.add(new Paragraph("No submissions recorded yet for this student.", smallItalicFont));
            } else {
                PdfPTable subTable = new PdfPTable(5);
                subTable.setWidthPercentage(100);
                subTable.setWidths(new float[]{2.5f, 1.2f, 1.5f, 1.8f, 3.0f});
                subTable.addCell(createHeaderCell("Homework Topic", tableHeaderFont));
                subTable.addCell(createHeaderCell("Type", tableHeaderFont));
                subTable.addCell(createHeaderCell("Score", tableHeaderFont));
                subTable.addCell(createHeaderCell("Submitted At", tableHeaderFont));
                subTable.addCell(createHeaderCell("Teacher Comment / Feedback", tableHeaderFont));

                for (Submission sub : submissions) {
                    String topic = sub.getAssignment() != null && sub.getAssignment().getTask() != null
                        ? sub.getAssignment().getTask().getGrammarTopic()
                        : "Practice";
                    String type = sub.getSubmissionType() != null ? sub.getSubmissionType().name() : "TEXT";
                    int maxPts = sub.getAssignment() != null && sub.getAssignment().getTask() != null && sub.getAssignment().getTask().getTotalPoints() != null
                        ? sub.getAssignment().getTask().getTotalPoints()
                        : 100;
                    double eff = sub.getEffectiveScore() != null ? sub.getEffectiveScore() : 0.0;
                    String scoreStr = String.format("%.0f / %d (%.0f%%)", eff, maxPts, maxPts > 0 ? (eff / maxPts) * 100 : eff);
                    String submittedAtStr = sub.getSubmittedAt() != null ? sub.getSubmittedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "—";
                    String feedbackStr = sub.getTeacherComment() != null && !sub.getTeacherComment().isBlank()
                        ? sub.getTeacherComment()
                        : (sub.getAiFeedback() != null && !sub.getAiFeedback().isBlank() ? sub.getAiFeedback() : "—");
                    if (feedbackStr.length() > 90) feedbackStr = feedbackStr.substring(0, 87) + "...";

                    subTable.addCell(createCell(topic, tableCellFont));
                    subTable.addCell(createCell(type, tableCellFont));
                    subTable.addCell(createCell(scoreStr, tableCellFont));
                    subTable.addCell(createCell(submittedAtStr, tableCellFont));
                    subTable.addCell(createCell(feedbackStr, tableCellFont));
                }
                document.add(subTable);
            }

            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF export failed", e);
        }
        return baos.toByteArray();
    }

    /**
     * @brief Helper creating a styled header table cell with background color and padding.
     * @param text Cell textual content.
     * @param font Font to format cell text.
     * @return Configured PdfPCell.
     */
    private PdfPCell createHeaderCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setBackgroundColor(new Color(241, 245, 249));
        cell.setPadding(4f);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }

    /**
     * @brief Helper creating a styled data table cell with padding and borders.
     * @param text Cell textual content.
     * @param font Font to format cell text.
     * @return Configured PdfPCell.
     */
    private PdfPCell createCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(4f);
        cell.setBorderColor(new Color(226, 232, 240));
        return cell;
    }
}
