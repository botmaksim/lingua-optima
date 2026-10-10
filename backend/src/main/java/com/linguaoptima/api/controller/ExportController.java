/**
 * @file ExportController.java
 * @brief REST controller for exporting student and cohort progress reports.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.service.ExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * @brief REST controller for exporting student and cohort progress reports.
 *
 * Generates downloadable reports formatted as PDF documents or CSV spreadsheets.
 */
@RestController
@RequestMapping({"/api/export", "/api/exports"})
@RequiredArgsConstructor
public class ExportController {

    /** @brief Field representing export service in ExportController. */
    private final ExportService exportService;

    /**
     * @brief Generates and downloads a cohort group progress report.
     *
     * @param groupId Unique identifier of the group.
     * @param format Export document format, defaults to pdf.
     * @param teacher Authenticated teacher requesting the report.
     * @return Binary attachment containing PDF or CSV document bytes.
     */
    @GetMapping({"/report/group/{id}", "/group/{id}"})
    public ResponseEntity<byte[]> exportGroupReport(
        @PathVariable("id") UUID groupId,
        @RequestParam(defaultValue = "pdf") String format,
        @AuthenticationPrincipal User teacher
    ) {
        byte[] data = exportService.generateGroupReport(groupId, format, teacher);
        String filename = "group-report-" + groupId + "." + format.toLowerCase();
        MediaType mediaType = "csv".equalsIgnoreCase(format) ?
            MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(mediaType)
            .body(data);
    }

    /**
     * @brief Generates and downloads an individual student academic report.
     *
     * @param studentId Unique identifier of the student.
     * @param format Export document format, defaults to pdf.
     * @param teacher Authenticated teacher requesting the report.
     * @return Binary attachment containing PDF or CSV document bytes.
     */
    @GetMapping({"/report/student/{id}", "/student/{id}"})
    public ResponseEntity<byte[]> exportStudentReport(
        @PathVariable("id") UUID studentId,
        @RequestParam(defaultValue = "pdf") String format,
        @AuthenticationPrincipal User teacher
    ) {
        byte[] data = exportService.generateStudentReport(studentId, format, teacher);
        String filename = "student-report-" + studentId + "." + format.toLowerCase();
        MediaType mediaType = "csv".equalsIgnoreCase(format) ?
            MediaType.parseMediaType("text/csv") : MediaType.APPLICATION_PDF;

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
            .contentType(mediaType)
            .body(data);
    }

    /**
     * @brief Retrieves detailed group performance report data for preview.
     *
     * @param groupId Unique identifier of the group.
     * @param teacher Authenticated teacher requesting the report preview.
     * @return HTTP 200 with GroupReportResponse.
     */
    @GetMapping({"/report/group/{id}/preview", "/group/{id}/preview"})
    public ResponseEntity<com.linguaoptima.api.dto.response.GroupReportResponse> getGroupReportPreview(
        @PathVariable("id") UUID groupId,
        @AuthenticationPrincipal User teacher
    ) {
        return ResponseEntity.ok(exportService.getGroupReportData(groupId, teacher));
    }
}
