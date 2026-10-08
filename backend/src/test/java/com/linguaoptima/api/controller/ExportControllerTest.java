package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.service.ExportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    @Mock
    private ExportService exportService;

    @InjectMocks
    private ExportController exportController;

    private User teacher;

    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).build();
    }

    @Test
    void testExportGroupReportPdfAndCsv() {
        UUID groupId = UUID.randomUUID();
        when(exportService.generateGroupReport(groupId, "pdf", teacher)).thenReturn(new byte[]{1, 2, 3});
        when(exportService.generateGroupReport(groupId, "csv", teacher)).thenReturn("a,b,c".getBytes());

        ResponseEntity<byte[]> pdfRes = exportController.exportGroupReport(groupId, "pdf", teacher);
        assertEquals(HttpStatus.OK, pdfRes.getStatusCode());
        assertNotNull(pdfRes.getBody());

        ResponseEntity<byte[]> csvRes = exportController.exportGroupReport(groupId, "csv", teacher);
        assertEquals(HttpStatus.OK, csvRes.getStatusCode());
        assertNotNull(csvRes.getBody());
    }

    @Test
    void testExportStudentReportPdfAndCsv() {
        UUID studentId = UUID.randomUUID();
        when(exportService.generateStudentReport(studentId, "pdf", teacher)).thenReturn(new byte[]{1, 2, 3});
        when(exportService.generateStudentReport(studentId, "csv", teacher)).thenReturn("a,b,c".getBytes());

        ResponseEntity<byte[]> pdfRes = exportController.exportStudentReport(studentId, "pdf", teacher);
        assertEquals(HttpStatus.OK, pdfRes.getStatusCode());

        ResponseEntity<byte[]> csvRes = exportController.exportStudentReport(studentId, "csv", teacher);
        assertEquals(HttpStatus.OK, csvRes.getStatusCode());
    }
}
