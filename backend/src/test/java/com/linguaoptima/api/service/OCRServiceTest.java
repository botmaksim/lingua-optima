/**
 * @file OCRServiceTest.java
 * @brief Unit and slice test suite for OCRService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.exception.OcrException;
import net.sourceforge.tess4j.ITesseract;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * @brief Unit and slice test suite for OCRService.
 */
@ExtendWith(MockitoExtension.class)
class OCRServiceTest {

    /** @brief Test fixture or mock dependency for tesseract. */
    @Mock
    private ITesseract tesseract;

    /** @brief Test fixture or mock dependency for ocr service. */
    private OCRService ocrService;
    /** @brief Test fixture or mock dependency for sample image bytes. */
    private byte[] sampleImageBytes;

    /**
     * @brief Initializes test fixtures and mock state before each test in OCRServiceTest.
     */
    @BeforeEach
    void setUp() throws Exception {
        ocrService = new OCRService(tesseract);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        sampleImageBytes = baos.toByteArray();
    }

    /**
     * @brief Verifies unit test scenario: extract text success.
     */
    @Test
    void testExtractTextSuccess() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello English Student!");

        String result = ocrService.extractText(sampleImageBytes);
        assertEquals("Hello English Student!", result);
    }

    /**
     * @brief Verifies unit test scenario: extract text null or empty throws.
     */
    @Test
    void testExtractTextNullOrEmptyThrows() {
        assertThrows(OcrException.class, () -> ocrService.extractText(null));
        assertThrows(OcrException.class, () -> ocrService.extractText(new byte[0]));
    }

    /**
     * @brief Verifies unit test scenario: extract text oversized throws.
     */
    @Test
    void testExtractTextOversizedThrows() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        assertThrows(OcrException.class, () -> ocrService.extractText(oversized));
    }

    /**
     * @brief Verifies unit test scenario: extract text invalid format throws.
     */
    @Test
    void testExtractTextInvalidFormatThrows() {
        byte[] corrupted = new byte[]{1, 2, 3, 4, 5};
        assertThrows(OcrException.class, () -> ocrService.extractText(corrupted));
    }

    /**
     * @brief Verifies unit test scenario: extract text empty result throws.
     */
    @Test
    void testExtractTextEmptyResultThrows() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("   ");
        assertThrows(OcrException.class, () -> ocrService.extractText(sampleImageBytes));
    }

    /**
     * @brief Verifies unit test scenario: default, datapath, missing candidate paths, and native UnsatisfiedLinkError resilience.
     */
    @Test
    void testDefaultConstructor() {
        OCRService svc = new OCRService();
        assertNotNull(svc);
        OCRService explicitPathSvc = new OCRService("/tmp");
        assertNotNull(explicitPathSvc);
        OCRService blankPathSvc = new OCRService("   ");
        assertNotNull(blankPathSvc);
        OCRService nullPathNoCandidates = new OCRService(() -> tesseract, null, "/nonexistent/path/1", "/nonexistent/path/2");
        assertNotNull(nullPathNoCandidates);
        OCRService linkageFailureSvc = new OCRService(() -> {
            throw new UnsatisfiedLinkError("libtesseract.so missing");
        }, null);
        assertNotNull(linkageFailureSvc);
        assertThrows(OcrException.class, () -> linkageFailureSvc.extractText(sampleImageBytes.clone()));
    }

    /**
     * @brief Verifies unit test scenario: null tesseract throws and still zeroes RAM buffer.
     */
    @Test
    void testNullTesseractThrows() {
        OCRService svc = new OCRService((ITesseract) null);
        byte[] bufferCopy = sampleImageBytes.clone();
        assertThrows(OcrException.class, () -> svc.extractText(bufferCopy));
        for (byte b : bufferCopy) {
            assertEquals(0, b, "Zero-retention invariant: RAM buffer must be zeroed even when OCR fails");
        }
    }

    /**
     * @brief Verifies unit test scenario: tesseract throws exception handled and zeroes RAM buffer.
     */
    @Test
    void testTesseractThrowsExceptionHandled() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(new RuntimeException("OCR engine failed"));
        byte[] bufferCopy = sampleImageBytes.clone();
        assertThrows(OcrException.class, () -> ocrService.extractText(bufferCopy));
        for (byte b : bufferCopy) {
            assertEquals(0, b, "Zero-retention invariant: RAM buffer must be zeroed in finally block after exception");
        }
    }

    /**
     * @brief Verifies unit test scenario: preprocess runtime exception handled by outer catch block.
     */
    @Test
    void testPreprocessExceptionHandledByOuterCatch() {
        OCRService failingPreprocessSvc = new OCRService(tesseract) {
            @Override
            public BufferedImage preprocess(BufferedImage image) {
                throw new RuntimeException("Simulated imaging failure");
            }
        };
        assertThrows(OcrException.class, () -> failingPreprocessSvc.extractText(sampleImageBytes));
    }

    /**
     * @brief Verifies unit test scenario: purge image zero retention.
     */
    @Test
    void testPurgeImageZeroRetention() {
        byte[] bytes = new byte[]{5, 10, 15};
        ocrService.purgeImage(bytes);
        assertEquals(0, bytes[0]);
        assertEquals(0, bytes[1]);
        assertEquals(0, bytes[2]);
        ocrService.purgeImage(null);
    }
}
