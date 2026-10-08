package com.linguaoptima.api.service;

import com.linguaoptima.api.exception.OcrException;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.io.ByteArrayInputStream;
import java.util.Arrays;

/**
 * Zero-Retention OCR Service:
 * Images are processed in RAM only and immediately nulled for Garbage Collection.
 * Strictly adheres to GDPR and handwriting biometric protection policies.
 */
@Slf4j
@Service
public class OCRService {

    private final ITesseract tesseract;

    public OCRService() {
        ITesseract instance = null;
        try {
            instance = new Tesseract();
            instance.setLanguage("eng");
            String datapath = System.getenv("TESSDATA_PREFIX");
            if (datapath != null && !datapath.isBlank()) {
                instance.setDatapath(datapath);
            }
        } catch (Throwable t) {
            log.warn("Tesseract native initialization warning: {}", t.getMessage());
        }
        this.tesseract = instance;
    }

    public OCRService(ITesseract tesseract) {
        this.tesseract = tesseract;
    }

    public String extractText(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new OcrException("No image provided. Please select a valid photo.");
        }

        if (imageBytes.length > 10 * 1024 * 1024) {
            throw new OcrException("Image size exceeds 10MB limit.");
        }

        BufferedImage original = null;
        BufferedImage preprocessed = null;
        try {
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                original = ImageIO.read(bais);
            }

            if (original == null) {
                throw new OcrException("Unsupported image format. Please upload JPEG or PNG.");
            }

            preprocessed = preprocess(original);

            String result = null;
            if (tesseract != null) {
                try {
                    result = tesseract.doOCR(preprocessed);
                } catch (Throwable t) {
                    log.warn("Native Tesseract OCR failed: {}", t.getMessage());
                }
            }

            if (result == null || result.trim().isBlank()) {
                // If OCR returned nothing, throw clear user error
                throw new OcrException("Image is unclear, please try again.");
            }

            return result.trim();
        } catch (OcrException oe) {
            throw oe;
        } catch (Exception e) {
            log.error("OCR extraction error: {}", e.getMessage());
            throw new OcrException("Image is unclear, please try again.", e);
        } finally {
            // ZERO-RETENTION: Clear in-memory references and arrays for immediate GC
            purgeImage(imageBytes);
            if (original != null) original.flush();
            if (preprocessed != null) preprocessed.flush();
        }
    }

    public BufferedImage preprocess(BufferedImage image) {
        // Convert to grayscale
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        // Enhance contrast
        RescaleOp rescale = new RescaleOp(1.2f, 15, null);
        return rescale.filter(gray, null);
    }

    public void purgeImage(byte[] imageBytes) {
        if (imageBytes != null) {
            Arrays.fill(imageBytes, (byte) 0);
        }
    }
}
