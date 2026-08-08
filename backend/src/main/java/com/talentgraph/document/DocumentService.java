package com.talentgraph.document;

import com.talentgraph.audit.AuditEventService;
import com.talentgraph.candidate.Application;
import com.talentgraph.candidate.ApplicationRepository;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.document.dto.CandidateDocumentResponse;
import com.talentgraph.evidence.ResumeEvidenceService;
import com.talentgraph.organization.Organization;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.parser.ResumeTextExtractor;
import com.talentgraph.storage.FileStorageService;
import com.talentgraph.auth.User;
import com.talentgraph.auth.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;

import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final CandidateDocumentRepository documentRepository;
    private final ResumeParsedContentRepository parsedContentRepository;
    private final CandidateRepository candidateRepository;
    private final ApplicationRepository applicationRepository;
    private final FileStorageService fileStorageService;
    private final List<ResumeTextExtractor> textExtractors;
    private final ResumeEvidenceService resumeEvidenceService;
    private final OrganizationAuthorizationService authorizationService;
    private final CurrentUserService currentUserService;
    private final AuditEventService auditEventService;

    @Value("${max.resume.size.mb:10}")
    private long maxResumeSizeMb;

    @Transactional
    public CandidateDocumentResponse uploadDocument(UUID candidateId, UUID applicationId, MultipartFile file, DocumentType type) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        UUID orgId = candidate.getOrganization().getId();
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Application application = null;
        if (applicationId != null) {
            application = applicationRepository.findById(applicationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Application not found: " + applicationId));
            if (!application.getCandidate().getId().equals(candidateId)) {
                throw new IllegalArgumentException("Application does not belong to candidate.");
            }
        }

        // 1. File Size Validation
        long maxSizeBytes = maxResumeSizeMb * 1024 * 1024;
        if (file.isEmpty() || file.getSize() > maxSizeBytes) {
            throw new IllegalArgumentException(String.format("File size exceeds maximum allowed limit of %d MB.", maxResumeSizeMb));
        }

        // 2. MIME Type & Filename Path Traversal Sanitization
        String mimeType = file.getContentType() != null ? file.getContentType().toLowerCase() : "";
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "document.bin";
        String sanitizedFilename = sanitizeFilename(originalFilename);

        validateMimeAndExtension(mimeType, sanitizedFilename);

        byte[] fileBytes;
        String sha256Hash;
        try {
            fileBytes = file.getBytes();
            sha256Hash = computeSha256(fileBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read uploaded file contents.", e);
        }

        // 3. Signature / Magic Bytes Validation
        validateMagicBytes(fileBytes, mimeType, sanitizedFilename);

        // 4. Store file locally using FileStorageService
        String storageKey = fileStorageService.store(new ByteArrayInputStream(fileBytes), sanitizedFilename, mimeType);

        // 5. Create CandidateDocument Record
        CandidateDocument document = CandidateDocument.builder()
                .candidate(candidate)
                .application(application)
                .documentType(type != null ? type : DocumentType.RESUME)
                .originalFilename(sanitizedFilename)
                .storedFilename(storageKey)
                .storageKey(storageKey)
                .mimeType(mimeType)
                .fileSizeBytes(file.getSize())
                .sha256Hash(sha256Hash)
                .processingStatus(ProcessingStatus.PROCESSING)
                .uploadedBy(currentUser)
                .uploadedAt(Instant.now())
                .build();

        document = documentRepository.save(document);

        Organization organization = candidate.getOrganization();

        auditEventService.logEvent(
                organization,
                currentUser,
                "CandidateDocument",
                document.getId(),
                "RESUME_UPLOADED",
                String.format("{\"filename\":\"%s\",\"sizeBytes\":%d,\"sha256\":\"%s\"}", sanitizedFilename, file.getSize(), sha256Hash)
        );

        auditEventService.logEvent(
                organization,
                currentUser,
                "CandidateDocument",
                document.getId(),
                "RESUME_PROCESSING_STARTED",
                String.format("{\"mimeType\":\"%s\"}", mimeType)
        );

        // 6. Text Extraction & Evidence Generation
        String extractedText = null;
        try {
            ResumeTextExtractor extractor = textExtractors.stream()
                    .filter(e -> e.supports(mimeType))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No text extractor registered for MIME type: " + mimeType));

            extractedText = extractor.extractText(new ByteArrayInputStream(fileBytes));

            ResumeParsedContent parsedContent = ResumeParsedContent.builder()
                    .document(document)
                    .rawText(extractedText)
                    .parserVersion("1.0.0-deterministic")
                    .characterCount(extractedText.length())
                    .build();
            parsedContentRepository.save(parsedContent);

            // Generate Evidence graph nodes
            resumeEvidenceService.generateResumeEvidence(candidate, document, extractedText);

            document.setProcessingStatus(ProcessingStatus.PROCESSED);
            document.setProcessedAt(Instant.now());
            documentRepository.save(document);

            auditEventService.logEvent(
                    organization,
                    currentUser,
                    "CandidateDocument",
                    document.getId(),
                    "RESUME_PROCESSED",
                    String.format("{\"characterCount\":%d}", extractedText.length())
            );

        } catch (Exception e) {
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setProcessingError(e.getMessage());
            documentRepository.save(document);

            auditEventService.logEvent(
                    organization,
                    currentUser,
                    "CandidateDocument",
                    document.getId(),
                    "RESUME_PROCESSING_FAILED",
                    String.format("{\"error\":\"%s\"}", e.getMessage() != null ? e.getMessage().replace("\"", "'") : "Extraction failed")
            );
        }

        return mapToResponse(document, extractedText);
    }

    @Transactional(readOnly = true)
    public List<CandidateDocumentResponse> getCandidateDocuments(UUID candidateId) {
        User currentUser = currentUserService.getCurrentUser();
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException("Candidate not found: " + candidateId));

        authorizationService.requireRole(currentUser, candidate.getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return documentRepository.findByCandidateIdOrderByUploadedAtDesc(candidateId).stream()
                .map(doc -> {
                    String text = parsedContentRepository.findByDocumentId(doc.getId())
                            .map(ResumeParsedContent::getRawText)
                            .orElse(null);
                    return mapToResponse(doc, text);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CandidateDocumentResponse getDocumentById(UUID candidateId, UUID documentId) {
        User currentUser = currentUserService.getCurrentUser();
        CandidateDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!doc.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("Document does not belong to candidate.");
        }

        authorizationService.requireRole(currentUser, doc.getCandidate().getOrganization().getId(), OrganizationRole.INTERVIEWER);

        String text = parsedContentRepository.findByDocumentId(doc.getId())
                .map(ResumeParsedContent::getRawText)
                .orElse(null);

        return mapToResponse(doc, text);
    }

    @Transactional(readOnly = true)
    public InputStream streamDocument(UUID candidateId, UUID documentId) {
        User currentUser = currentUserService.getCurrentUser();
        CandidateDocument doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));

        if (!doc.getCandidate().getId().equals(candidateId)) {
            throw new IllegalArgumentException("Document does not belong to candidate.");
        }

        authorizationService.requireRole(currentUser, doc.getCandidate().getOrganization().getId(), OrganizationRole.INTERVIEWER);

        return fileStorageService.retrieve(doc.getStorageKey());
    }

    public CandidateDocumentResponse mapToResponse(CandidateDocument doc, String rawText) {
        return CandidateDocumentResponse.builder()
                .id(doc.getId())
                .candidateId(doc.getCandidate().getId())
                .applicationId(doc.getApplication() != null ? doc.getApplication().getId() : null)
                .documentType(doc.getDocumentType())
                .originalFilename(doc.getOriginalFilename())
                .storageKey(doc.getStorageKey())
                .mimeType(doc.getMimeType())
                .fileSizeBytes(doc.getFileSizeBytes())
                .sha256Hash(doc.getSha256Hash())
                .processingStatus(doc.getProcessingStatus())
                .processingError(doc.getProcessingError())
                .rawText(rawText)
                .uploadedAt(doc.getUploadedAt())
                .processedAt(doc.getProcessedAt())
                .build();
    }

    private void validateMimeAndExtension(String mimeType, String filename) {
        boolean validPdf = "application/pdf".equalsIgnoreCase(mimeType) || filename.endsWith(".pdf");
        boolean validDocx = "application/vnd.openxmlformats-officedocument.wordprocessingml.document".equalsIgnoreCase(mimeType) || filename.endsWith(".docx");

        if (!validPdf && !validDocx) {
            throw new IllegalArgumentException("Unsupported file format. Only PDF (.pdf) and Word (.docx) documents are accepted.");
        }
    }

    private void validateMagicBytes(byte[] fileBytes, String mimeType, String filename) {
        if (fileBytes == null || fileBytes.length < 4) {
            throw new IllegalArgumentException("File content is empty or corrupt.");
        }

        boolean isPdfExt = filename.endsWith(".pdf") || "application/pdf".equalsIgnoreCase(mimeType);
        if (isPdfExt) {
            // PDF Magic Header: %PDF- (0x25 0x50 0x44 0x46)
            if (fileBytes[0] != 0x25 || fileBytes[1] != 0x50 || fileBytes[2] != 0x44 || fileBytes[3] != 0x46) {
                throw new IllegalArgumentException("Invalid PDF file header/signature.");
            }
        } else {
            // DOCX Magic Header (Zip Archive): PK (0x50 0x4B 0x03 0x04)
            if (fileBytes[0] != 0x50 || fileBytes[1] != 0x4B) {
                throw new IllegalArgumentException("Invalid DOCX file header/signature.");
            }
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String name = originalFilename.replaceAll("[\\\\/]", "_");
        return name.trim();
    }

    private String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            return HexFormat.of().formatHex(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate SHA-256 hash.", e);
        }
    }
}
