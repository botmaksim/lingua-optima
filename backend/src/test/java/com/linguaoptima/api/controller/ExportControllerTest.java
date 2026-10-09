/**
 * @file ExportControllerTest.java
 * @brief Unit and slice test suite for ExportController.
 */
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

/**
 * @brief Unit and slice test suite for ExportController.
 */
@ExtendWith(MockitoExtension.class)
class ExportControllerTest {

    /** @brief Test fixture or mock dependency for export service. */
    @Mock
    private ExportService exportService;

    /** @brief Test fixture or mock dependency for export controller. */
    @InjectMocks
    private ExportController exportController;

    /** @brief Test fixture or mock dependency for teacher. */
    private User teacher;

    /**
     * @brief Initializes test fixtures and mock state before each test in ExportControllerTest.
     */
    @BeforeEach
    void setUp() {
        teacher = User.builder().id(UUID.randomUUID()).build();
    }

    /**
     * @brief Verifies unit test scenario: export group report pdf and csv.
     */
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

    /**
     * @brief Verifies unit test scenario: export student report pdf and csv.
     */
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
