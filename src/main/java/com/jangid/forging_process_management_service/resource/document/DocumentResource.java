package com.jangid.forging_process_management_service.resource.document;

import com.jangid.forging_process_management_service.configuration.security.TenantContextHolder;
import com.jangid.forging_process_management_service.entities.document.Document;
import com.jangid.forging_process_management_service.entities.document.DocumentCategory;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.service.document.DocumentService;
import com.jangid.forging_process_management_service.utils.GenericResourceUtils;
import com.jangid.forging_process_management_service.utils.GenericExceptionHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Component
public class DocumentResource {

    private final DocumentService documentService;

    @PostMapping("entities/{entityType}/{entityId}/documents")
    public ResponseEntity<?> attachDocumentsToEntity(
            @PathVariable String entityType,
            @PathVariable String entityId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam(required = false) String documentCategory,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();
            // Parse entity type
            DocumentLink.EntityType entityTypeEnum = DocumentLink.EntityType.valueOf(entityType.toUpperCase());
            
            // Parse entity ID
            Long entityIdLong = GenericResourceUtils.convertResourceIdToLong(entityId)
                    .orElseThrow(() -> new RuntimeException("Invalid entity ID: " + entityId));

            // Parse document category
            DocumentCategory category = null;
            if (StringUtils.hasText(documentCategory)) {
                try {
                    category = DocumentCategory.valueOf(documentCategory.toUpperCase());
                } catch (IllegalArgumentException e) {
                    category = DocumentCategory.OTHER;
                }
            }

            // Convert files to list
            List<MultipartFile> fileList = Arrays.asList(files);

            // Attach documents
            List<Document> documents = documentService.attachDocumentsToEntity(
                tenantIdLongValue, entityTypeEnum, entityIdLong, fileList,
                    category, title, description, tags);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Documents attached successfully");
            response.put("uploadedDocuments", documents.stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList()));

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "attachDocumentsToEntity");
        }
    }

    @GetMapping("entities/{entityType}/{entityId}/documents")
    public ResponseEntity<?> getDocumentsForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            // Parse entity type
            DocumentLink.EntityType entityTypeEnum = DocumentLink.EntityType.valueOf(entityType.toUpperCase());
            
            // Parse entity ID
            Long entityIdLong = GenericResourceUtils.convertResourceIdToLong(entityId)
                    .orElseThrow(() -> new RuntimeException("Invalid entity ID: " + entityId));

            // Get documents
            List<Document> documents = documentService.getDocumentsForEntity(tenantIdLongValue, entityTypeEnum, entityIdLong);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entityType", entityType);
            response.put("entityId", entityIdLong);
            response.put("documents", documents.stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList()));
            response.put("totalDocuments", documents.size());

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getDocumentsForEntity");
        }
    }

    @GetMapping("documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable String documentId) {
        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long docId = GenericResourceUtils.convertResourceIdToLong(documentId)
                    .orElseThrow(() -> new RuntimeException("Invalid document ID: " + documentId));

            Document document = documentService.getDocumentByIdAndTenantId(docId, tenantIdLongValue);

            Resource resource;
            Path storedFilePath = Paths.get(document.getFilePath());
            
            // Check if stored file exists
            if (!Files.exists(storedFilePath)) {
                throw new RuntimeException("Stored file not found: " + document.getFilePath());
            }
            
            // Handle based on whether the stored file is compressed or not
            if (document.getIsCompressed()) {
                // Stored file is compressed, decompress before serving
                try {
                    byte[] decompressedData = documentService.decompressFile(storedFilePath);
                    resource = new ByteArrayResource(decompressedData);
                    
                    log.debug("Serving decompressed file: {} (decompressed: {} bytes, stored compressed: {} bytes)", 
                        document.getOriginalFileName(), 
                        decompressedData.length, 
                        Files.size(storedFilePath));
                        
                } catch (Exception e) {
                    log.error("Failed to decompress file {}: {}", document.getOriginalFileName(), e.getMessage());
                    throw new RuntimeException("Failed to decompress file for download", e);
                }
            } else {
                // Stored file is not compressed, serve directly
                resource = new UrlResource(storedFilePath.toUri());
                
                if (!resource.isReadable()) {
                    throw new RuntimeException("File not readable");
                }
                
                log.debug("Serving original file: {} ({} bytes)", 
                    document.getOriginalFileName(), Files.size(storedFilePath));
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(document.getMimeType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                            "attachment; filename=\"" + document.getOriginalFileName() + "\"")
                    .body(resource);

        } catch (Exception exception) {
            log.error("Failed to download document {}", documentId, exception);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    @DeleteMapping("documents/{documentId}")
    public ResponseEntity<?> deleteDocument(@PathVariable String documentId) {
        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long docId = GenericResourceUtils.convertResourceIdToLong(documentId)
                    .orElseThrow(() -> new RuntimeException("Invalid document ID: " + documentId));

            documentService.deleteDocument(docId, tenantIdLongValue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Document deleted successfully");
            response.put("documentId", docId);

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "deleteDocument");
        }
    }


    @GetMapping("entities/{entityType}/{entityId}/documents/count")
    public ResponseEntity<?> getDocumentCountForEntity(
            @PathVariable String entityType,
            @PathVariable String entityId) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            DocumentLink.EntityType entityTypeEnum = DocumentLink.EntityType.valueOf(entityType.toUpperCase());
            Long entityIdLong = GenericResourceUtils.convertResourceIdToLong(entityId)
                    .orElseThrow(() -> new RuntimeException("Invalid entity ID: " + entityId));

            long count = documentService.getDocumentCountForEntity(tenantIdLongValue, entityTypeEnum, entityIdLong);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entityType", entityType);
            response.put("entityId", entityIdLong);
            response.put("documentCount", count);

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getDocumentCountForEntity");
        }
    }


    @GetMapping("entities/{entityType}/{entityId}/documents/by-category")
    public ResponseEntity<?> getDocumentsForEntityByCategory(
            @PathVariable String entityType,
            @PathVariable String entityId) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            DocumentLink.EntityType entityTypeEnum = DocumentLink.EntityType.valueOf(entityType.toUpperCase());
            Long entityIdLong = GenericResourceUtils.convertResourceIdToLong(entityId)
                    .orElseThrow(() -> new RuntimeException("Invalid entity ID: " + entityId));

            Map<DocumentCategory, List<Document>> documentsByCategory = 
                documentService.getDocumentsForEntityGroupedByCategory(tenantIdLongValue, entityTypeEnum, entityIdLong);

            // Build response with categories
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("entityType", entityType);
            response.put("entityId", entityIdLong);
            
            Map<String, Object> categories = new HashMap<>();
            for (Map.Entry<DocumentCategory, List<Document>> entry : documentsByCategory.entrySet()) {
                List<Map<String, Object>> docs = entry.getValue().stream()
                        .map(this::mapDocumentToResponse)
                        .collect(Collectors.toList());
                categories.put(entry.getKey().name().toLowerCase(), docs);
            }
            
            response.put("documentsByCategory", categories);
            response.put("totalDocuments", documentsByCategory.values().stream()
                    .mapToInt(List::size).sum());

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getDocumentsForEntityByCategory");
        }
    }

    @GetMapping("documents/category/{category}")
    public ResponseEntity<?> getDocumentsByCategory(@PathVariable String category) {
        try {
            Long tenantIdLong = TenantContextHolder.getAuthenticatedTenantId();

            DocumentCategory categoryEnum = DocumentCategory.valueOf(category.toUpperCase());
            List<Document> documents = documentService.getDocumentsByCategory(tenantIdLong, categoryEnum);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("tenantId", tenantIdLong);
            response.put("category", category.toLowerCase());
            response.put("documents", documents.stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList()));
            response.put("totalDocuments", documents.size());

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getDocumentsByCategory");
        }
    }


    @GetMapping("documents/{documentId}/metadata")
    public ResponseEntity<?> getDocumentMetadata(@PathVariable String documentId) {
        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            Long docId = GenericResourceUtils.convertResourceIdToLong(documentId)
                    .orElseThrow(() -> new RuntimeException("Invalid document ID: " + documentId));

            Document document = documentService.getDocumentByIdAndTenantId(docId, tenantIdLongValue);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documentId", docId);
            response.put("originalFileName", document.getOriginalFileName());

            // Format metadata
            Map<String, Object> metadata = new HashMap<>();
            if (document.getMetadata() != null) {
                document.getMetadata().stream()
                    .filter(meta -> !meta.getDeleted())
                    .forEach(meta -> {
                        metadata.put(meta.getMetadataKey(), Map.of(
                            "value", meta.getMetadataValue(),
                            "type", meta.getMetadataType().name(),
                            "parsedValue", meta.getParsedValue(),
                            "id", meta.getId(),
                            "createdAt", meta.getCreatedAt()
                        ));
                    });
            }
            
            response.put("metadata", metadata);
            response.put("metadataCount", metadata.size());

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "getDocumentMetadata");
        }
    }

    @GetMapping("documents/search")
    public ResponseEntity<?> searchDocuments(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String tags,
            @RequestParam(required = false) String documentCategory,
            @RequestParam(required = false) String documentType,
            @RequestParam(required = false) String mimeType,
            @RequestParam(required = false) String fileExtension,
            @RequestParam(required = false) Long minFileSize,
            @RequestParam(required = false) Long maxFileSize,
            @RequestParam(required = false) Boolean isCompressed,
            @RequestParam(required = false) String uploadSource,
            @RequestParam(required = false) String uploadedAfter,
            @RequestParam(required = false) String uploadedBefore,
            @RequestParam(required = false) String modifiedAfter,
            @RequestParam(required = false) String modifiedBefore,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String linkType,
            @RequestParam(required = false) String metadataKey,
            @RequestParam(required = false) String metadataValue,
            @RequestParam(required = false) Boolean hasMetadata,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "20") Integer size,
            @RequestParam(required = false, defaultValue = "uploadDate") String sortBy,
            @RequestParam(required = false, defaultValue = "DESC") String sortDirection) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            // Build search request
            DocumentService.DocumentSearchRequest searchRequest = new DocumentService.DocumentSearchRequest();
            searchRequest.setKeyword(keyword);
            searchRequest.setFileName(fileName);
            searchRequest.setDescription(description);
            searchRequest.setTags(tags);
            searchRequest.setDocumentCategory(documentCategory);
            searchRequest.setDocumentType(documentType);
            searchRequest.setMimeType(mimeType);
            searchRequest.setFileExtension(fileExtension);
            searchRequest.setMinFileSize(minFileSize);
            searchRequest.setMaxFileSize(maxFileSize);
            searchRequest.setIsCompressed(isCompressed);
            searchRequest.setUploadSource(uploadSource);
            searchRequest.setUploadedAfter(uploadedAfter);
            searchRequest.setUploadedBefore(uploadedBefore);
            searchRequest.setModifiedAfter(modifiedAfter);
            searchRequest.setModifiedBefore(modifiedBefore);
            searchRequest.setEntityType(entityType);
            searchRequest.setEntityId(entityId);
            searchRequest.setLinkType(linkType);
            searchRequest.setMetadataKey(metadataKey);
            searchRequest.setMetadataValue(metadataValue);
            searchRequest.setHasMetadata(hasMetadata);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);

            // Execute search
            Page<Document> searchResults = documentService.searchDocuments(tenantIdLongValue, searchRequest);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", searchResults.getContent().stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList()));
            
            // Pagination info
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", searchResults.getNumber());
            pagination.put("totalPages", searchResults.getTotalPages());
            pagination.put("totalElements", searchResults.getTotalElements());
            pagination.put("pageSize", searchResults.getSize());
            pagination.put("hasNext", searchResults.hasNext());
            pagination.put("hasPrevious", searchResults.hasPrevious());
            pagination.put("isFirst", searchResults.isFirst());
            pagination.put("isLast", searchResults.isLast());
            response.put("pagination", pagination);

            // Search info
            Map<String, Object> searchInfo = new HashMap<>();
            searchInfo.put("searchCriteria", buildSearchCriteriaInfo(searchRequest));
            searchInfo.put("appliedFilters", countAppliedFilters(searchRequest));
            searchInfo.put("resultCount", searchResults.getContent().size());
            response.put("searchInfo", searchInfo);

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "searchDocuments");
        }
    }

    @PostMapping("documents/search")
    public ResponseEntity<?> searchDocumentsAdvanced(
            @RequestBody DocumentService.DocumentSearchRequest searchRequest) {

        try {
            Long tenantIdLongValue = TenantContextHolder.getAuthenticatedTenantId();

            // Set defaults if not provided
            if (searchRequest.getPage() == null) searchRequest.setPage(0);
            if (searchRequest.getSize() == null) searchRequest.setSize(20);
            if (searchRequest.getSortBy() == null) searchRequest.setSortBy("uploadDate");
            if (searchRequest.getSortDirection() == null) searchRequest.setSortDirection("DESC");

            // Execute search
            Page<Document> searchResults = documentService.searchDocuments(tenantIdLongValue, searchRequest);

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("documents", searchResults.getContent().stream()
                    .map(this::mapDocumentToResponse)
                    .collect(Collectors.toList()));
            
            // Pagination info
            Map<String, Object> pagination = new HashMap<>();
            pagination.put("currentPage", searchResults.getNumber());
            pagination.put("totalPages", searchResults.getTotalPages());
            pagination.put("totalElements", searchResults.getTotalElements());
            pagination.put("pageSize", searchResults.getSize());
            pagination.put("hasNext", searchResults.hasNext());
            pagination.put("hasPrevious", searchResults.hasPrevious());
            pagination.put("isFirst", searchResults.isFirst());
            pagination.put("isLast", searchResults.isLast());
            response.put("pagination", pagination);

            // Search info
            Map<String, Object> searchInfo = new HashMap<>();
            searchInfo.put("searchCriteria", buildSearchCriteriaInfo(searchRequest));
            searchInfo.put("appliedFilters", countAppliedFilters(searchRequest));
            searchInfo.put("resultCount", searchResults.getContent().size());
            response.put("searchInfo", searchInfo);

            return ResponseEntity.ok(response);

        } catch (Exception exception) {
            return GenericExceptionHandler.handleException(exception, "searchDocumentsAdvanced");
        }
    }

    /**
     * Build search criteria information for response
     */
    private Map<String, Object> buildSearchCriteriaInfo(DocumentService.DocumentSearchRequest searchRequest) {
        Map<String, Object> criteria = new HashMap<>();
        
        if (StringUtils.hasText(searchRequest.getKeyword())) {
            criteria.put("keyword", searchRequest.getKeyword());
        }
        if (StringUtils.hasText(searchRequest.getFileName())) {
            criteria.put("fileName", searchRequest.getFileName());
        }
        if (StringUtils.hasText(searchRequest.getDescription())) {
            criteria.put("description", searchRequest.getDescription());
        }
        if (StringUtils.hasText(searchRequest.getTags())) {
            criteria.put("tags", searchRequest.getTags());
        }
        if (StringUtils.hasText(searchRequest.getDocumentCategory())) {
            criteria.put("documentCategory", searchRequest.getDocumentCategory());
        }
        if (StringUtils.hasText(searchRequest.getDocumentType())) {
            criteria.put("documentType", searchRequest.getDocumentType());
        }
        if (StringUtils.hasText(searchRequest.getMimeType())) {
            criteria.put("mimeType", searchRequest.getMimeType());
        }
        if (StringUtils.hasText(searchRequest.getFileExtension())) {
            criteria.put("fileExtension", searchRequest.getFileExtension());
        }
        if (searchRequest.getMinFileSize() != null) {
            criteria.put("minFileSize", searchRequest.getMinFileSize());
        }
        if (searchRequest.getMaxFileSize() != null) {
            criteria.put("maxFileSize", searchRequest.getMaxFileSize());
        }
        if (searchRequest.getIsCompressed() != null) {
            criteria.put("isCompressed", searchRequest.getIsCompressed());
        }
        if (StringUtils.hasText(searchRequest.getUploadSource())) {
            criteria.put("uploadSource", searchRequest.getUploadSource());
        }
        if (StringUtils.hasText(searchRequest.getUploadedAfter())) {
            criteria.put("uploadedAfter", searchRequest.getUploadedAfter());
        }
        if (StringUtils.hasText(searchRequest.getUploadedBefore())) {
            criteria.put("uploadedBefore", searchRequest.getUploadedBefore());
        }
        if (StringUtils.hasText(searchRequest.getModifiedAfter())) {
            criteria.put("modifiedAfter", searchRequest.getModifiedAfter());
        }
        if (StringUtils.hasText(searchRequest.getModifiedBefore())) {
            criteria.put("modifiedBefore", searchRequest.getModifiedBefore());
        }
        if (StringUtils.hasText(searchRequest.getEntityType())) {
            criteria.put("entityType", searchRequest.getEntityType());
        }
        if (searchRequest.getEntityId() != null) {
            criteria.put("entityId", searchRequest.getEntityId());
        }
        if (StringUtils.hasText(searchRequest.getLinkType())) {
            criteria.put("linkType", searchRequest.getLinkType());
        }
        if (StringUtils.hasText(searchRequest.getMetadataKey())) {
            criteria.put("metadataKey", searchRequest.getMetadataKey());
        }
        if (StringUtils.hasText(searchRequest.getMetadataValue())) {
            criteria.put("metadataValue", searchRequest.getMetadataValue());
        }
        if (searchRequest.getHasMetadata() != null) {
            criteria.put("hasMetadata", searchRequest.getHasMetadata());
        }

        criteria.put("sortBy", searchRequest.getSortBy());
        criteria.put("sortDirection", searchRequest.getSortDirection());
        
        return criteria;
    }

    /**
     * Count the number of applied filters
     */
    private int countAppliedFilters(DocumentService.DocumentSearchRequest searchRequest) {
        int count = 0;
        
        if (StringUtils.hasText(searchRequest.getKeyword())) count++;
        if (StringUtils.hasText(searchRequest.getFileName())) count++;
        if (StringUtils.hasText(searchRequest.getDescription())) count++;
        if (StringUtils.hasText(searchRequest.getTags())) count++;
        if (StringUtils.hasText(searchRequest.getDocumentCategory())) count++;
        if (StringUtils.hasText(searchRequest.getDocumentType())) count++;
        if (StringUtils.hasText(searchRequest.getMimeType())) count++;
        if (StringUtils.hasText(searchRequest.getFileExtension())) count++;
        if (searchRequest.getMinFileSize() != null) count++;
        if (searchRequest.getMaxFileSize() != null) count++;
        if (searchRequest.getIsCompressed() != null) count++;
        if (StringUtils.hasText(searchRequest.getUploadSource())) count++;
        if (StringUtils.hasText(searchRequest.getUploadedAfter())) count++;
        if (StringUtils.hasText(searchRequest.getUploadedBefore())) count++;
        if (StringUtils.hasText(searchRequest.getModifiedAfter())) count++;
        if (StringUtils.hasText(searchRequest.getModifiedBefore())) count++;
        if (StringUtils.hasText(searchRequest.getEntityType())) count++;
        if (searchRequest.getEntityId() != null) count++;
        if (StringUtils.hasText(searchRequest.getLinkType())) count++;
        if (StringUtils.hasText(searchRequest.getMetadataKey())) count++;
        if (StringUtils.hasText(searchRequest.getMetadataValue())) count++;
        if (searchRequest.getHasMetadata() != null) count++;

        return count;
    }

    /**
     * Map Document entity to response DTO
     */
    private Map<String, Object> mapDocumentToResponse(Document document) {
        Map<String, Object> response = new HashMap<>();
        response.put("documentId", document.getId());
        response.put("fileName", document.getFileName());
        response.put("originalFileName", document.getOriginalFileName());
        response.put("documentCategory", document.getDocumentCategory());
        response.put("documentType", document.getDocumentType());
        response.put("fileSize", document.getFileSizeBytes());
        response.put("mimeType", document.getMimeType());
        response.put("title", extractTitleFromDescription(document.getDescription()));
        response.put("description", document.getDescription());
        response.put("tags", document.getTags());
        response.put("uploadedAt", document.getCreatedAt());
        response.put("downloadUrl", "/api/documents/" + document.getId() + "/download");
        
        // Include metadata if available
        if (document.getMetadata() != null && !document.getMetadata().isEmpty()) {
            Map<String, Object> metadata = new HashMap<>();
            document.getMetadata().stream()
                .filter(meta -> !meta.getDeleted())
                .forEach(meta -> {
                    metadata.put(meta.getMetadataKey(), Map.of(
                        "value", meta.getMetadataValue(),
                        "type", meta.getMetadataType().name(),
                        "parsedValue", meta.getParsedValue()
                    ));
                });
            response.put("metadata", metadata);
            response.put("metadataCount", metadata.size());
        } else {
            response.put("metadata", new HashMap<>());
            response.put("metadataCount", 0);
        }
        
        return response;
    }

    /**
     * Extract title from description (temporary until we add title field)
     */
    private String extractTitleFromDescription(String description) {
        if (description == null) return null;
        
        String[] lines = description.split("\n");
        return lines.length > 0 ? lines[0] : description;
    }
}
