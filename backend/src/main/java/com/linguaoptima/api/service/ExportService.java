/**
 * @file ExportService.java
 * @brief Report generation service exporting student and class performance to CSV and PDF formats.
 */
package com.linguaoptima.api.service;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.opencsv.CSVWriter;
import com.linguaoptima.api.domain.*;
import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.repository.GroupRepository;
import com.linguaoptima.api.repository.GroupStudentRepository;
import com.linguaoptima.api.repository.ProgressRecordRepository;
import com.linguaoptima.api.repository.SubmissionRepository;
import com.linguaoptima.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

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
    /** @brief Field representing progress record repository in ExportService. */
    private final ProgressRecordRepository progressRecordRepository;
    /** @brief Field representing user repository in ExportService. */
    private final UserRepository userRepository;

    /**
     * @brief Generates an export report for a group in either CSV or PDF format.
     * @param groupId Unique identifier of the group.
     * @param format Desired export format ("csv" or "pdf").
     * @param teacher Educator requesting the report.
     * @return Byte array containing raw report file bytes.
     * @throws ResourceNotFoundException if group is not found.
     * @throws ForbiddenException if teacher does not own the group.
     */
    @Transactional(readOnly = true)
    public byte[] generateGroupReport(UUID groupId, String format, User teacher) {
        Group group = groupRepository.findById(groupId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

        if (!group.getTeacher().getId().equals(teacher.getId())) {
            throw new ForbiddenException("Access denied: You are not the teacher of this group.");
        }

        List<GroupStudent> activeStudents = groupStudentRepository.findByGroupIdAndIsActiveTrue(groupId);

        if ("csv".equalsIgnoreCase(format)) {
            return generateGroupCsv(group, activeStudents);
        } else {
            return generateGroupPdf(group, activeStudents);
        }
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

        List<ProgressRecord> records = progressRecordRepository.findByStudent(student);
        List<Submission> submissions = submissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentId);

        if ("csv".equalsIgnoreCase(format)) {
            return generateStudentCsv(student, records);
        } else {
            return generateStudentPdf(student, records, submissions);
        }
    }

    /**
     * @brief Generates CSV bytes for a group roster.
     * @param group Target Group entity.
     * @param students List of enrolled GroupStudent entities.
     * @return UTF-8 byte array of the CSV document.
     */
    private byte[] generateGroupCsv(Group group, List<GroupStudent> students) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Group Report", group.getName(), "Generated: " + LocalDateTime.now()});
            writer.writeNext(new String[]{"Student Name", "Email", "CEFR Level", "Joined Date"});

            for (GroupStudent gs : students) {
                User s = gs.getStudent();
                writer.writeNext(new String[]{
                    s.getFullName(),
                    s.getEmail(),
                    s.getCefrLevel().name(),
                    gs.getJoinedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("CSV export failed", e);
        }
        return sw.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * @brief Generates PDF bytes for a group roster and performance table.
     * @param group Target Group entity.
     * @param students List of enrolled GroupStudent entities.
     * @return Byte array of the rendered PDF document.
     */
    private byte[] generateGroupPdf(Group group, List<GroupStudent> students) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            Document document = new Document();
            PdfWriter.getInstance(document, baos);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            document.add(new Paragraph("Lingua Optima - Group Performance Report", titleFont));
            document.add(new Paragraph("Group: " + group.getName()));
            document.add(new Paragraph("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(4);
            table.addCell("Student Name");
            table.addCell("Email");
            table.addCell("CEFR Level");
            table.addCell("Joined Date");

            for (GroupStudent gs : students) {
                User s = gs.getStudent();
                table.addCell(s.getFullName());
                table.addCell(s.getEmail());
                table.addCell(s.getCefrLevel().name());
                table.addCell(gs.getJoinedAt().format(DateTimeFormatter.ISO_LOCAL_DATE));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF generation failed", e);
        }
        return baos.toByteArray();
    }

    /**
     * @brief Generates CSV bytes detailing topic mastery metrics for an individual student.
     * @param student Target student user.
     * @param records List of ProgressRecord entities.
     * @return UTF-8 byte array of the CSV document.
     */
    private byte[] generateStudentCsv(User student, List<ProgressRecord> records) {
        StringWriter sw = new StringWriter();
        try (CSVWriter writer = new CSVWriter(sw)) {
            writer.writeNext(new String[]{"Student Progress Report", student.getFullName(), student.getEmail()});
            writer.writeNext(new String[]{"Grammar Topic", "Total Attempts", "Error Count", "Mastery Score (%)"});

            for (ProgressRecord pr : records) {
                writer.writeNext(new String[]{
                    pr.getGrammarTopic(),
                    String.valueOf(pr.getTotalAttempts()),
                    String.valueOf(pr.getErrorCount()),
                    String.valueOf(pr.getMasteryScore() * 100)
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
            document.add(new Paragraph("Lingua Optima - Student Progress Report", titleFont));
            document.add(new Paragraph("Student: " + student.getFullName() + " (" + student.getEmail() + ")"));
            document.add(new Paragraph("Current Level: " + student.getCefrLevel()));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Topic Mastery:"));
            PdfPTable table = new PdfPTable(4);
            table.addCell("Grammar Topic");
            table.addCell("Attempts");
            table.addCell("Errors");
            table.addCell("Mastery (%)");

            for (ProgressRecord pr : records) {
                table.addCell(pr.getGrammarTopic());
                table.addCell(String.valueOf(pr.getTotalAttempts()));
                table.addCell(String.valueOf(pr.getErrorCount()));
                table.addCell(String.format("%.1f%%", pr.getMasteryScore() * 100));
            }

            document.add(table);
            document.close();
        } catch (Exception e) {
            throw new RuntimeException("PDF export failed", e);
        }
        return baos.toByteArray();
    }
}
