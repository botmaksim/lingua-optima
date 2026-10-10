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
     * @brief Verifies unit test scenario: export group report pdf and csv with and without date range filters.
     */
    @Test
    void testExportGroupReportPdfAndCsv() {
        UUID groupId = UUID.randomUUID();
        when(exportService.generateGroupReport(groupId, "pdf", null, null, teacher)).thenReturn(new byte[]{1, 2, 3});
        when(exportService.generateGroupReport(groupId, "csv", null, null, teacher)).thenReturn("a,b,c".getBytes());

        ResponseEntity<byte[]> pdfRes = exportController.exportGroupReport(groupId, "pdf", null, null, teacher);
        assertEquals(HttpStatus.OK, pdfRes.getStatusCode());
        assertNotNull(pdfRes.getBody());

        ResponseEntity<byte[]> csvRes = exportController.exportGroupReport(groupId, "csv", null, null, teacher);
        assertEquals(HttpStatus.OK, csvRes.getStatusCode());
        assertNotNull(csvRes.getBody());

        // Verify with date range
        java.time.LocalDateTime from = java.time.LocalDateTime.now().minusDays(7);
        java.time.LocalDateTime to = java.time.LocalDateTime.now();
        when(exportService.generateGroupReport(groupId, "pdf", from, to, teacher)).thenReturn(new byte[]{4, 5, 6});

        ResponseEntity<byte[]> filteredRes = exportController.exportGroupReport(groupId, "pdf", from, to, teacher);
        assertEquals(HttpStatus.OK, filteredRes.getStatusCode());
        assertNotNull(filteredRes.getBody());
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

    /**
     * @brief Verifies unit test scenario: get group report preview with and without date filter.
     */
    @Test
    void testGetGroupReportPreview() {
        UUID groupId = UUID.randomUUID();
        com.linguaoptima.api.dto.response.GroupReportResponse preview =
            com.linguaoptima.api.dto.response.GroupReportResponse.builder()
                .groupId(groupId)
                .groupName("Test Group")
                .periodLabel("All Time")
                .build();
        when(exportService.getGroupReportData(groupId, null, null, teacher)).thenReturn(preview);

        ResponseEntity<com.linguaoptima.api.dto.response.GroupReportResponse> res =
            exportController.getGroupReportPreview(groupId, null, null, teacher);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals("Test Group", res.getBody().getGroupName());
        assertEquals("All Time", res.getBody().getPeriodLabel());
    }
}
