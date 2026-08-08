package com.talentgraph.document;

import com.talentgraph.common.ApiResponse;
import com.talentgraph.document.dto.CandidateDocumentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/candidates/{candidateId}/documents")
@RequiredArgsConstructor
public class CandidateDocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CandidateDocumentResponse>> uploadDocument(
            @PathVariable("candidateId") UUID candidateId,
            @RequestParam(name = "applicationId", required = false) UUID applicationId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "documentType", defaultValue = "RESUME") DocumentType documentType
    ) {
        CandidateDocumentResponse response = documentService.uploadDocument(candidateId, applicationId, file, documentType);
        return ResponseEntity.ok(ApiResponse.success(response, "Document uploaded and processed successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CandidateDocumentResponse>>> getCandidateDocuments(
            @PathVariable("candidateId") UUID candidateId
    ) {
        List<CandidateDocumentResponse> response = documentService.getCandidateDocuments(candidateId);
        return ResponseEntity.ok(ApiResponse.success(response, "Candidate documents retrieved successfully"));
    }

    @GetMapping("/{documentId}")
    public ResponseEntity<ApiResponse<CandidateDocumentResponse>> getDocumentById(
            @PathVariable("candidateId") UUID candidateId,
            @PathVariable("documentId") UUID documentId
    ) {
        CandidateDocumentResponse response = documentService.getDocumentById(candidateId, documentId);
        return ResponseEntity.ok(ApiResponse.success(response, "Document retrieved successfully"));
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<InputStreamResource> downloadDocument(
            @PathVariable("candidateId") UUID candidateId,
            @PathVariable("documentId") UUID documentId
    ) {
        CandidateDocumentResponse meta = documentService.getDocumentById(candidateId, documentId);
        InputStream inputStream = documentService.streamDocument(candidateId, documentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + meta.getOriginalFilename() + "\"")
                .contentType(MediaType.parseMediaType(meta.getMimeType()))
                .contentLength(meta.getFileSizeBytes())
                .body(new InputStreamResource(inputStream));
    }
}
