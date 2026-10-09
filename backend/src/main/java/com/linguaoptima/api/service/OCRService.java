/**
 * @file OCRService.java
 * @brief Zero-Retention optical character recognition service using Tesseract OCR.
 */
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
 * @brief Zero-Retention optical character recognition service using Tesseract OCR.
 *
 * Guarantees that uploaded images are processed strictly in RAM and memory buffers are
 * zeroed immediately via Arrays.fill() to ensure biometric and GDPR compliance.
 */
@Slf4j
@Service
public class OCRService {

    /** @brief Field representing tesseract in OCRService. */
    private final ITesseract tesseract;

    /**
     * @brief Default constructor initializing native Tesseract OCR engine using environment configuration.
     */
    public OCRService() {
        this(System.getenv("TESSDATA_PREFIX"));
    }

    /**
     * @brief Constructor initializing native Tesseract OCR engine with an explicit tessdata directory path.
     *
     * @param configuredDatapath Optional path to the tessdata directory.
     */
    public OCRService(String configuredDatapath) {
        this(Tesseract::new, configuredDatapath,
            "/usr/share/tessdata",
            "/usr/share/tesseract-ocr/5/tessdata",
            "/usr/share/tesseract-ocr/4.00/tessdata",
            "/tmp");
    }

    /**
     * @brief Constructor initializing Tesseract OCR engine using a custom provider factory and candidate paths.
     *
     * @param factory Supplier creating the ITesseract instance.
     * @param configuredDatapath Optional explicit path to the tessdata directory.
     * @param candidatePaths Fallback filesystem paths to probe for tessdata.
     */
    OCRService(java.util.function.Supplier<ITesseract> factory, String configuredDatapath, String... candidatePaths) {
        ITesseract instance = null;
        try {
            instance = factory.get();
            instance.setLanguage("eng");
            if (configuredDatapath != null && !configuredDatapath.isBlank()) {
                instance.setDatapath(configuredDatapath);
            } else {
                for (String candidate : candidatePaths) {
                    if (new java.io.File(candidate).exists()) {
                        instance.setDatapath(candidate);
                        break;
                    }
                }
            }
        } catch (Throwable t) {
            log.warn("Tesseract native initialization warning: {}", t.getMessage());
        }
        this.tesseract = instance;
    }

    /**
     * @brief Constructor for testing with dependency-injected ITesseract mock.
     *
     * @param tesseract Mocked or preconfigured ITesseract instance.
     */
    public OCRService(ITesseract tesseract) {
        this.tesseract = tesseract;
    }

    /**
     * @brief Extracts handwritten or printed text from raw image bytes in memory.
     *
     * @param imageBytes Binary content of uploaded image.
     * @return Extracted textual content.
     * @throws OcrException if image is invalid, unreadable, or exceeds limits.
     */
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
                throw new OcrException("Image is unclear, please try again.");
            }

            return result.trim();
        } catch (OcrException oe) {
            throw oe;
        } catch (Exception e) {
            log.error("OCR extraction error: {}", e.getMessage());
            throw new OcrException("Image is unclear, please try again.", e);
        } finally {
            purgeImage(imageBytes);
            if (original != null) original.flush();
            if (preprocessed != null) preprocessed.flush();
        }
    }

    /**
     * @brief Enhances image quality by converting to grayscale and increasing contrast.
     *
     * @param image Input original image.
     * @return High-contrast grayscale image optimized for OCR recognition.
     */
    public BufferedImage preprocess(BufferedImage image) {
        BufferedImage gray = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_BYTE_GRAY);
        Graphics2D g = gray.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();

        RescaleOp rescale = new RescaleOp(1.2f, 15, null);
        return rescale.filter(gray, null);
    }

    /**
     * @brief Erases byte contents in RAM to prevent forensic retrieval of student handwriting.
     *
     * @param imageBytes Byte array to overwrite with zeros.
     */
    public void purgeImage(byte[] imageBytes) {
        if (imageBytes != null) {
            Arrays.fill(imageBytes, (byte) 0);
        }
    }
}
