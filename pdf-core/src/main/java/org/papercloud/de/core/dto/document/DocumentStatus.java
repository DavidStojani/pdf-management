package org.papercloud.de.core.dto.document;

public enum DocumentStatus {
    UPLOADED,
    OCR_IN_PROGRESS,
    OCR_COMPLETED,
    ENRICHING_IN_PROGRESS,
    INDEXING_IN_PROGRESS,
    INDEXING_COMPLETED,
    COMPLETED
}
