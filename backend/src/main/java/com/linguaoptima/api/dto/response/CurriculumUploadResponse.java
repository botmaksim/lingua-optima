/**
 * @file CurriculumUploadResponse.java
 * @brief DTO returned upon uploading custom grammar rules or vocabulary lists to server storage.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CurriculumFileType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Response DTO containing metadata and extracted text from uploaded curriculum files.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurriculumUploadResponse {

    /** @brief Original name of the uploaded file. */
    private String fileName;

    /** @brief Modality of the curriculum material (RULE or VOCABULARY). */
    private CurriculumFileType fileType;

    /** @brief Size of the uploaded file in bytes. */
    private Long fileSize;

    /** @brief Brief textual preview snippet of the extracted file content. */
    private String contentSnippet;

    /** @brief Full UTF-8 textual content parsed from the uploaded curriculum file. */
    private String fullContent;

    /** @brief Server-side file path where the curriculum material is persisted. */
    private String serverPath;

    /** @brief Associated grammar topic or custom syllabus focus area, if specified. */
    private String topic;
}
