package org.dev.erpautomationassisstant.model;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Base64;

/**
 * Represents an extracted image ready for Multi-Modal LLM
 */
@Data
@Builder
@Slf4j
public class TaskImage {

    private String imageId;           // UUID for reference
    private String fileName;          // Original filename
    private String storedPath;        // File system path
    private byte[] imageData;         // Raw bytes for LLM
    private String mimeType;          // image/png
    private Integer taskNumber;       // Which task (1-10)
    private Integer positionInDoc;    // Position in document
    private Long fileSize;            // Size in bytes

    /**
     * Convert to base64 for Multi-Modal LLM API
     */
    public String toBase64() {
        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Create data URI for API calls
     */
    public String toDataUri() {
        return "data:" + mimeType + ";base64," + toBase64();
    }
}