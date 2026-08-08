package com.talentgraph.document.dto;

import com.talentgraph.document.DocumentType;
import com.talentgraph.document.ProcessingStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateDocumentResponse {

    private UUID id;
    private UUID candidateId;
    private UUID applicationId;
    private DocumentType documentType;
    private String originalFilename;
    private String storageKey;
    private String mimeType;
    private long fileSizeBytes;
    private String sha256Hash;
    private ProcessingStatus processingStatus;
    private String processingError;
    private String rawText;
    private Instant uploadedAt;
    private Instant processedAt;
}
