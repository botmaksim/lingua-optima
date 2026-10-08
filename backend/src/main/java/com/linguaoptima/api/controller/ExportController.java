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

@RestController
@RequestMapping({"/api/export", "/api/exports"})
@RequiredArgsConstructor
public class ExportController {

    private final ExportService exportService;

    @GetMapping("/report/group/{id}")
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

    @GetMapping("/report/student/{id}")
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
}
