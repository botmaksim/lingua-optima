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
 * @file OCRServiceTest.java
 * @brief Unit and slice test suite for OCRService.
 */
@ExtendWith(MockitoExtension.class)
class OCRServiceTest {

    @Mock
    private ITesseract tesseract;

    private OCRService ocrService;
    private byte[] sampleImageBytes;

    @BeforeEach
    void setUp() throws Exception {
        ocrService = new OCRService(tesseract);

        BufferedImage img = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        sampleImageBytes = baos.toByteArray();
    }

    @Test
    void testExtractTextSuccess() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("Hello English Student!");

        String result = ocrService.extractText(sampleImageBytes);
        assertEquals("Hello English Student!", result);
    }

    @Test
    void testExtractTextNullOrEmptyThrows() {
        assertThrows(OcrException.class, () -> ocrService.extractText(null));
        assertThrows(OcrException.class, () -> ocrService.extractText(new byte[0]));
    }

    @Test
    void testExtractTextOversizedThrows() {
        byte[] oversized = new byte[11 * 1024 * 1024];
        assertThrows(OcrException.class, () -> ocrService.extractText(oversized));
    }

    @Test
    void testExtractTextInvalidFormatThrows() {
        byte[] corrupted = new byte[]{1, 2, 3, 4, 5};
        assertThrows(OcrException.class, () -> ocrService.extractText(corrupted));
    }

    @Test
    void testExtractTextEmptyResultThrows() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenReturn("   ");
        assertThrows(OcrException.class, () -> ocrService.extractText(sampleImageBytes));
    }

    @Test
    void testDefaultConstructor() {
        OCRService svc = new OCRService();
        assertNotNull(svc);
    }

    @Test
    void testNullTesseractThrows() {
        OCRService svc = new OCRService(null);
        assertThrows(OcrException.class, () -> svc.extractText(sampleImageBytes));
    }

    @Test
    void testTesseractThrowsExceptionHandled() throws Exception {
        when(tesseract.doOCR(any(BufferedImage.class))).thenThrow(new RuntimeException("OCR engine failed"));
        assertThrows(OcrException.class, () -> ocrService.extractText(sampleImageBytes));
    }

    @Test
    void testPurgeImageZeroRetention() {
        byte[] bytes = new byte[]{5, 10, 15};
        ocrService.purgeImage(bytes);
        assertEquals(0, bytes[0]);
        assertEquals(0, bytes[1]);
        assertEquals(0, bytes[2]);
    }
}
