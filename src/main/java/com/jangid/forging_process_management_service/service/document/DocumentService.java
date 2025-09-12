package com.jangid.forging_process_management_service.service.document;

import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.document.Document;
import com.jangid.forging_process_management_service.entities.document.DocumentCategory;
import com.jangid.forging_process_management_service.entities.document.DocumentType;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.document.DocumentMetadata;
import com.jangid.forging_process_management_service.entities.document.TenantStorageQuota;
import com.jangid.forging_process_management_service.repositories.TenantRepository;
import com.jangid.forging_process_management_service.repositories.document.DocumentRepository;
import com.jangid.forging_process_management_service.repositories.document.DocumentLinkRepository;
import com.jangid.forging_process_management_service.repositories.document.DocumentMetadataRepository;
import com.jangid.forging_process_management_service.repositories.document.TenantStorageQuotaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentLinkRepository documentLinkRepository;
    private final DocumentMetadataRepository documentMetadataRepository;
    private final TenantStorageQuotaRepository quotaRepository;
    private final TenantRepository tenantRepository;

    @Value("${fopmas.documents.storage.base-path:/var/fopmas/documents}")
    private String basePath;

    @Value("${fopmas.documents.upload.max-file-size:10485760}")
    private long maxFileSizeBytes;

    @Value("${fopmas.documents.compression.enabled:true}")
    private boolean compressionEnabled;

    @Value("${fopmas.documents.compression.min-file-size:1024}")
    private long compressionMinFileSize; // Minimum file size to compress (1KB)

    @Value("${fopmas.documents.compression.excluded-types:image/jpeg,image/png,image/gif,video/,audio/}")
    private String compressionExcludedTypes; // MIME types to exclude from compression

    /**
     * Attach documents to a specific entity
     */
    @Transactional
    public List<Document> attachDocumentsToEntity(
            Long tenantId,
            DocumentLink.EntityType entityType,
            Long entityId,
            List<MultipartFile> files,
            DocumentCategory category,
            String title,
            String description,
            String tags) {

        // Validate tenant
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        // Check storage quota
        checkStorageQuota(tenantId, files);

        List<Document> uploadedDocuments = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // Create and save document
                Document document = createDocument(tenant, file, category, title, description, tags);
                Document savedDocument = documentRepository.save(document);

                // Create entity link
                DocumentLink link = DocumentLink.builder()
                        .document(savedDocument)
                        .entityType(entityType)
                        .entityId(entityId)
                        .linkType(DocumentLink.LinkType.ATTACHED)
                        .build();
                documentLinkRepository.save(link);

                // Update storage quota with actual stored file size
                updateStorageUsage(tenantId, savedDocument.getEffectiveFileSize());

                uploadedDocuments.add(savedDocument);
                log.info("Document attached: {} to {}:{}", savedDocument.getOriginalFileName(), entityType, entityId);

            } catch (Exception e) {
                log.error("Failed to upload file: {}", file.getOriginalFilename(), e);
                throw new RuntimeException("Failed to upload file: " + file.getOriginalFilename(), e);
            }
        }

        return uploadedDocuments;
    }

    /**
     * Get all documents for a specific entity
     */
    public List<Document> getDocumentsForEntity(Long tenantId, DocumentLink.EntityType entityType, Long entityId) {
        return documentRepository.findByLinkedEntity(tenantId, entityType, entityId);
    }

    /**
     * Get document by ID
     */
    public Document getDocumentByIdAndTenantId(Long documentId, Long tenantId) {
        return documentRepository.findByIdAndTenantIdAndDeletedFalse(documentId, tenantId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId + " and tenant: " + tenantId));
    }

    /**
     * Delete document
     */
    @Transactional
    public void deleteDocument(Long documentId, Long tenantId) {
        Document document = getDocumentByIdAndTenantId(documentId, tenantId);
        
        // Soft delete document
        document.setDeleted(true);
        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);

        // Soft delete links
        documentLinkRepository.softDeleteByDocumentId(documentId);

        // Soft delete metadata entries
        documentMetadataRepository.softDeleteByDocumentId(documentId);

        // Update storage quota with actual stored file size
        updateStorageUsage(document.getTenant().getId(), -document.getEffectiveFileSize());

        // Delete physical file from file system
        deletePhysicalFile(document);
        
        log.info("Document deleted: {}", document.getOriginalFileName());
    }

    /**
     * Get documents count for entity (with tenant isolation)
     */
    public long getDocumentCountForEntity(Long tenantId, DocumentLink.EntityType entityType, Long entityId) {
        return documentLinkRepository.getDocumentCountForEntity(entityType, entityId, tenantId);
    }

    /**
     * Get documents for entity grouped by category
     */
    public Map<DocumentCategory, List<Document>> getDocumentsForEntityGroupedByCategory(
            Long tenantId, DocumentLink.EntityType entityType, Long entityId) {
        
        List<Document> documents = getDocumentsForEntity(tenantId, entityType, entityId);
        
        return documents.stream()
                .collect(Collectors.groupingBy(
                    doc -> doc.getDocumentCategory() != null ? doc.getDocumentCategory() : DocumentCategory.OTHER
                ));
    }

    /**
     * Get all documents for a tenant by category
     */
    public List<Document> getDocumentsByCategory(Long tenantId, DocumentCategory category) {
        return documentRepository.findByTenant_IdAndDocumentCategoryAndDeletedFalse(tenantId, category);
    }

    /**
     * Create Document entity from uploaded file
     */
    private Document createDocument(Tenant tenant, MultipartFile file, DocumentCategory category, 
                                   String title, String description, String tags) throws IOException {

        // Validate file
        validateFile(file);

        // Generate unique filename and path
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String fileExtension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID().toString() + "." + fileExtension;
        
        // Create directory structure: /tenants/{tenant_id}/{category}/{YYYY-MM}/
        String yearMonth = LocalDateTime.now().toString().substring(0, 7); // YYYY-MM
        DocumentCategory effectiveCategory = (category != null ? category : DocumentCategory.OTHER);
        String categoryPath = effectiveCategory.getPathComponent();
        Path tenantDir = Paths.get(basePath, "tenants", String.valueOf(tenant.getId()), categoryPath, yearMonth);
        Files.createDirectories(tenantDir);
        
        // Save original file
        Path filePath = tenantDir.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        // Check if file should be compressed and handle compression
        boolean isCompressed = false;
        Path finalFilePath = filePath; // This will be the actual stored file path
        long finalFileSize = file.getSize();
        
        if (shouldCompressFile(file.getContentType(), file.getSize())) {
            try {
                Path compressedFilePath = generateCompressedFilePath(filePath);
                compressFile(filePath, compressedFilePath);
                long compressedSize = Files.size(compressedFilePath);
                
                // Check if compression saves significant space (>10%)
                double compressionRatio = getCompressionRatio(file.getSize(), compressedSize);
                if (compressionRatio >= 10.0) {
                    // Compression is beneficial: DELETE original, KEEP compressed
                    Files.deleteIfExists(filePath);
                    finalFilePath = compressedFilePath;
                    finalFileSize = compressedSize;
                    isCompressed = true;
                    log.info("File compressed and original deleted: {} ({}% reduction, {} -> {} bytes)", 
                        originalFilename, String.format("%.1f", compressionRatio), file.getSize(), compressedSize);
                } else {
                    // Compression not beneficial: DELETE compressed, KEEP original
                    Files.deleteIfExists(compressedFilePath);
                    // finalFilePath remains as original filePath
                    // finalFileSize remains as original file.getSize()
                    isCompressed = false;
                    log.debug("Compression ratio too low ({}%), deleted compressed file, keeping original: {}", 
                        String.format("%.1f", compressionRatio), originalFilename);
                }
            } catch (Exception e) {
                log.warn("Failed to compress file {}, keeping original: {}", originalFilename, e.getMessage());
                // finalFilePath remains as original filePath
                // finalFileSize remains as original file.getSize()
                isCompressed = false;
            }
        }
        
        // Calculate file hash
        String fileHash = calculateFileHash(file.getBytes());
        
        // Auto-detect document type
        DocumentType documentType = DocumentType.fromMimeType(file.getContentType());
        
        // Build description with title
        String fullDescription = "";
        if (StringUtils.hasText(title)) {
            fullDescription = title;
            if (StringUtils.hasText(description)) {
                fullDescription += "\n" + description;
            }
        } else if (StringUtils.hasText(description)) {
            fullDescription = description;
        }

        // Create document entity
        // Note: We store only ONE file per document (either original or compressed)
        // - If isCompressed = false: filePath points to original file, fileSizeBytes = original size
        // - If isCompressed = true: filePath points to compressed file, fileSizeBytes = compressed size
        Document document = Document.builder()
                .tenant(tenant)
                .fileName(finalFilePath.getFileName().toString()) // Actual stored filename
                .originalFileName(originalFilename) // User's original filename for download
                .filePath(finalFilePath.toString()) // Actual stored file path
                .compressedFilePath(null) // Not used in single-file approach
                .mimeType(file.getContentType()) // Always original MIME type for user experience
                .fileExtension(fileExtension)
                .fileSizeBytes(file.getSize()) // Original file size for user reference
                .compressedSizeBytes(isCompressed ? finalFileSize : null) // Actual stored size if compressed
                .fileHash(fileHash)
                .documentCategory(category != null ? category : DocumentCategory.OTHER)
                .documentType(documentType)
                .description(fullDescription)
                .tags(tags)
                .uploadSource("WEB_UI")
                .isCompressed(isCompressed)
                .isActive(true)
                .deleted(false)
                .build();

        // Create system metadata entries
        createSystemMetadata(document, file, title, description);
        
        return document;
    }

    /**
     * Check storage quota before upload
     */
    private void checkStorageQuota(Long tenantId, List<MultipartFile> files) {
        TenantStorageQuota quota = quotaRepository.findByTenant_Id(tenantId)
                .orElseGet(() -> createDefaultQuota(tenantId));

        if (!quota.getQuotaEnabled()) {
            return;
        }

        long totalFileSize = files.stream().mapToLong(MultipartFile::getSize).sum();

        // Check individual file size limits
        for (MultipartFile file : files) {
            if (file.getSize() > quota.getMaxFileSizeBytes()) {
                throw new RuntimeException(String.format(
                    "File '%s' size (%d bytes) exceeds maximum allowed size (%d bytes)",
                    file.getOriginalFilename(), file.getSize(), quota.getMaxFileSizeBytes()));
            }
        }

        // Check total storage limit
        if ((quota.getUsedStorageBytes() + totalFileSize) > quota.getMaxStorageBytes()) {
            throw new RuntimeException(String.format(
                "Upload would exceed storage quota. Available: %d bytes, Required: %d bytes",
                (quota.getMaxStorageBytes() - quota.getUsedStorageBytes()), totalFileSize));
        }
    }

    /**
     * Update storage usage
     */
    private void updateStorageUsage(Long tenantId, long bytes) {
        if (bytes > 0) {
            quotaRepository.addUsedStorage(tenantId, bytes);
        } else {
            quotaRepository.removeUsedStorage(tenantId, Math.abs(bytes));
        }
    }

    /**
     * Create default quota for tenant
     */
    private TenantStorageQuota createDefaultQuota(Long tenantId) {
        Tenant tenant = tenantRepository.findByIdAndDeletedFalse(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found: " + tenantId));

        TenantStorageQuota quota = TenantStorageQuota.builder()
                .tenant(tenant)
                .maxStorageBytes(1073741824L) // 1GB
                .usedStorageBytes(0L)
                .maxFileSizeBytes(maxFileSizeBytes) // 10MB
                .maxFilesPerEntity(100)
                .quotaEnabled(true)
                .build();

        return quotaRepository.save(quota);
    }


    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File cannot be empty");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            throw new RuntimeException("File must have a valid filename");
        }
        
        // Check file size (additional check beyond quota)
        if (file.getSize() > maxFileSizeBytes) {
            throw new RuntimeException(String.format(
                "File size (%d bytes) exceeds maximum allowed size (%d bytes)", 
                file.getSize(), maxFileSizeBytes));
        }
        
        // Check file extension  
        String extension = getFileExtension(filename);
        if (extension.equals("unknown")) {
            throw new RuntimeException("File must have a valid extension");
        }
        
        // Basic security check
        if (filename.contains("..")) {
            throw new RuntimeException("Invalid file path: " + filename);
        }
        
        // Basic content type validation
        String contentType = file.getContentType();
        if (contentType == null || contentType.trim().isEmpty()) {
            log.warn("File has no content type, allowing upload: {}", filename);
        }
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Calculate SHA-256 hash of file
     */
    private String calculateFileHash(byte[] fileBytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fileBytes);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to calculate file hash", e);
            return UUID.randomUUID().toString();
        }
    }

    /**
     * Create system-generated metadata for a document
     */
    private void createSystemMetadata(Document document, MultipartFile file, String title, String description) {
        List<DocumentMetadata> metadataList = new ArrayList<>();
        
        // File information metadata
        metadataList.add(createMetadata(document, "original_filename", file.getOriginalFilename(), DocumentMetadata.MetadataType.STRING));
        metadataList.add(createMetadata(document, "original_file_size_bytes", String.valueOf(file.getSize()), DocumentMetadata.MetadataType.NUMBER));
        metadataList.add(createMetadata(document, "stored_file_size_bytes", String.valueOf(document.getEffectiveFileSize()), DocumentMetadata.MetadataType.NUMBER));
        metadataList.add(createMetadata(document, "mime_type", file.getContentType(), DocumentMetadata.MetadataType.STRING));
        
        // Upload information metadata
        metadataList.add(createMetadata(document, "upload_timestamp", LocalDateTime.now().toString(), DocumentMetadata.MetadataType.DATE));
        metadataList.add(createMetadata(document, "upload_source", "WEB_UI", DocumentMetadata.MetadataType.STRING));
        
        // User-provided metadata
        if (StringUtils.hasText(title)) {
            metadataList.add(createMetadata(document, "title", title, DocumentMetadata.MetadataType.STRING));
        }
        if (StringUtils.hasText(description)) {
            metadataList.add(createMetadata(document, "description", description, DocumentMetadata.MetadataType.STRING));
        }
        
        // File processing metadata
        metadataList.add(createMetadata(document, "processing_status", "COMPLETED", DocumentMetadata.MetadataType.STRING));
        metadataList.add(createMetadata(document, "is_compressed", String.valueOf(document.getIsCompressed()), DocumentMetadata.MetadataType.BOOLEAN));
        
        // Compression metadata (if applicable)
        if (document.getIsCompressed()) {
            metadataList.add(createMetadata(document, "compression_ratio", String.format("%.2f", document.getCompressionRatio()), DocumentMetadata.MetadataType.NUMBER));
            metadataList.add(createMetadata(document, "compression_algorithm", "GZIP", DocumentMetadata.MetadataType.STRING));
            metadataList.add(createMetadata(document, "storage_format", "COMPRESSED", DocumentMetadata.MetadataType.STRING));
        } else {
            metadataList.add(createMetadata(document, "storage_format", "ORIGINAL", DocumentMetadata.MetadataType.STRING));
        }
        
        // Business context metadata
        metadataList.add(createMetadata(document, "document_category", document.getDocumentCategory().name(), DocumentMetadata.MetadataType.STRING));
        metadataList.add(createMetadata(document, "document_type", document.getDocumentType().name(), DocumentMetadata.MetadataType.STRING));
        
        // File security metadata
        metadataList.add(createMetadata(document, "file_hash", document.getFileHash(), DocumentMetadata.MetadataType.STRING));
        metadataList.add(createMetadata(document, "hash_algorithm", "SHA-256", DocumentMetadata.MetadataType.STRING));
        
        // Add all metadata to document
        document.getMetadata().addAll(metadataList);
        
        log.info("Created {} metadata entries for document: {}", metadataList.size(), document.getOriginalFileName());
    }

    /**
     * Helper method to create a DocumentMetadata entry
     */
    private DocumentMetadata createMetadata(Document document, String key, String value, DocumentMetadata.MetadataType type) {
        return DocumentMetadata.builder()
                .document(document)
                .metadataKey(key)
                .metadataValue(value)
                .metadataType(type)
                .deleted(false)
                .build();
    }

    /**
     * Delete physical file from file system (single file approach)
     */
    private void deletePhysicalFile(Document document) {
        String fileType = document.getIsCompressed() ? "compressed" : "original";
        deletePhysicalFileInternal(document.getFilePath(), fileType, document.getOriginalFileName());
    }

    /**
     * Internal method to delete a single physical file directly
     */
    private void deletePhysicalFileInternal(String filePath, String fileType, String originalFileName) {
        try {
            if (StringUtils.hasText(filePath)) {
                Path path = Paths.get(filePath);
                
                if (Files.exists(path)) {
                    // Delete file directly - no trash/recovery mechanism
                    Files.delete(path);
                    log.info("{} file deleted permanently: {}", fileType, filePath);
                } else {
                    log.warn("{} file not found for deletion: {}", fileType, filePath);
                }
            } else {
                log.warn("Document has no {} file path set: {}", fileType, originalFileName);
            }
        } catch (IOException e) {
            log.error("Failed to delete {} file for document {}: {}", 
                fileType, originalFileName, filePath, e);
            // Don't throw exception here as the database deletion was successful
            // The file can be cleaned up later via a maintenance job
        } catch (Exception e) {
            log.error("Unexpected error while deleting {} file for document {}: {}", 
                fileType, originalFileName, filePath, e);
        }
    }

    /**
     * Check if a file should be compressed based on configuration
     */
    private boolean shouldCompressFile(String mimeType, long fileSize) {
        if (!compressionEnabled) {
            return false;
        }
        
        if (fileSize < compressionMinFileSize) {
            return false;
        }
        
        if (mimeType == null) {
            return true; // Compress unknown types
        }
        
        // Check excluded types
        List<String> excludedTypes = Arrays.asList(compressionExcludedTypes.split(","));
        return excludedTypes.stream()
                .noneMatch(excluded -> mimeType.toLowerCase().startsWith(excluded.toLowerCase().trim()));
    }

    /**
     * Compress file using GZIP
     */
    private Path compressFile(Path originalFile, Path compressedFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(originalFile.toFile());
             FileOutputStream fos = new FileOutputStream(compressedFile.toFile());
             GZIPOutputStream gzipOut = new GZIPOutputStream(fos)) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                gzipOut.write(buffer, 0, bytesRead);
            }
            
            log.debug("File compressed: {} -> {} ({}%)", 
                originalFile.getFileName(), 
                compressedFile.getFileName(),
                String.format("%.1f", getCompressionRatio(originalFile.toFile().length(), compressedFile.toFile().length())));
            
            return compressedFile;
        }
    }

    /**
     * Decompress file using GZIP
     */
    public byte[] decompressFile(Path compressedFile) throws IOException {
        try (FileInputStream fis = new FileInputStream(compressedFile.toFile());
             GZIPInputStream gzipIn = new GZIPInputStream(fis);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = gzipIn.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            
            return baos.toByteArray();
        }
    }

    /**
     * Calculate compression ratio
     */
    private double getCompressionRatio(long originalSize, long compressedSize) {
        if (originalSize == 0) return 0.0;
        return ((double) (originalSize - compressedSize) / originalSize) * 100.0;
    }

    /**
     * Generate compressed file path
     */
    private Path generateCompressedFilePath(Path originalPath) {
        String compressedFileName = originalPath.getFileName().toString() + ".gz";
        return originalPath.getParent().resolve(compressedFileName);
    }

    /**
     * Search documents with comprehensive filtering and pagination
     */
    public Page<Document> searchDocuments(Long tenantId, DocumentSearchRequest searchRequest) {
        // Build pageable with sorting
        Pageable pageable = buildPageable(searchRequest);
        
        // Parse enum filters (only the essential ones)
        DocumentCategory category = parseDocumentCategory(searchRequest.getDocumentCategory());
        DocumentType documentType = parseDocumentType(searchRequest.getDocumentType());
        DocumentLink.EntityType entityType = parseEntityType(searchRequest.getEntityType());
        
        // Use the simplified search method with essential filters only
        return documentRepository.searchDocumentsSimple(
            tenantId,
            searchRequest.getKeyword(),
            category,
            documentType,
            searchRequest.getFileExtension(),
            entityType,
            pageable
        );
    }

    /**
     * Build pageable object with sorting
     */
    private Pageable buildPageable(DocumentSearchRequest searchRequest) {
        int page = Math.max(0, searchRequest.getPage() != null ? searchRequest.getPage() : 0);
        int size = Math.min(100, Math.max(1, searchRequest.getSize() != null ? searchRequest.getSize() : 20));
        
        Sort sort = Sort.unsorted();
        if (StringUtils.hasText(searchRequest.getSortBy())) {
            String sortField = mapSortField(searchRequest.getSortBy());
            Sort.Direction direction = "DESC".equalsIgnoreCase(searchRequest.getSortDirection()) 
                ? Sort.Direction.DESC : Sort.Direction.ASC;
            sort = Sort.by(direction, sortField);
        } else {
            // Default sort by upload date descending
            sort = Sort.by(Sort.Direction.DESC, "createdAt");
        }
        
        return PageRequest.of(page, size, sort);
    }

    /**
     * Map user-friendly sort field names to entity field names
     */
    private String mapSortField(String sortBy) {
        switch (sortBy.toLowerCase()) {
            case "uploaddate":
            case "uploaded":
            case "created":
                return "createdAt";
            case "modifieddate":
            case "modified":
            case "updated":
                return "updatedAt";
            case "filename":
            case "name":
                return "originalFileName";
            case "filesize":
            case "size":
                return "fileSizeBytes";
            case "category":
                return "documentCategory";
            case "type":
                return "documentType";
            case "mimetype":
                return "mimeType";
            case "extension":
                return "fileExtension";
            case "source":
                return "uploadSource";
            default:
                return "createdAt"; // Default fallback
        }
    }

    /**
     * Parse document category string
     */
    private DocumentCategory parseDocumentCategory(String categoryStr) {
        if (!StringUtils.hasText(categoryStr)) {
            return null;
        }
        
        try {
            return DocumentCategory.valueOf(categoryStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid document category: {}", categoryStr);
            return null;
        }
    }

    /**
     * Parse document type string
     */
    private DocumentType parseDocumentType(String typeStr) {
        if (!StringUtils.hasText(typeStr)) {
            return null;
        }
        
        try {
            return DocumentType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid document type: {}", typeStr);
            return null;
        }
    }

    /**
     * Parse entity type string
     */
    private DocumentLink.EntityType parseEntityType(String entityTypeStr) {
        if (!StringUtils.hasText(entityTypeStr)) {
            return null;
        }
        
        try {
            return DocumentLink.EntityType.valueOf(entityTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid entity type: {}", entityTypeStr);
            return null;
        }
    }

    /**
     * Document search request DTO (inner class for convenience)
     */
    public static class DocumentSearchRequest {
        // Text search
        private String keyword;
        private String fileName;
        private String description;
        private String tags;
        
        // Category and type filters
        private String documentCategory;
        private String documentType;
        private String mimeType;
        private String fileExtension;
        
        // Size filters
        private Long minFileSize;
        private Long maxFileSize;
        private Boolean isCompressed;
        private String uploadSource;
        
        // Date filters
        private String uploadedAfter;
        private String uploadedBefore;
        private String modifiedAfter;
        private String modifiedBefore;
        
        // Entity association filters
        private String entityType;
        private Long entityId;
        private String linkType;
        
        // Metadata filters
        private String metadataKey;
        private String metadataValue;
        private Boolean hasMetadata;
        
        // Pagination and sorting
        private Integer page;
        private Integer size;
        private String sortBy;
        private String sortDirection;

        // Getters and setters
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getTags() { return tags; }
        public void setTags(String tags) { this.tags = tags; }
        
        public String getDocumentCategory() { return documentCategory; }
        public void setDocumentCategory(String documentCategory) { this.documentCategory = documentCategory; }
        
        public String getDocumentType() { return documentType; }
        public void setDocumentType(String documentType) { this.documentType = documentType; }
        
        public String getMimeType() { return mimeType; }
        public void setMimeType(String mimeType) { this.mimeType = mimeType; }
        
        public String getFileExtension() { return fileExtension; }
        public void setFileExtension(String fileExtension) { this.fileExtension = fileExtension; }
        
        public Long getMinFileSize() { return minFileSize; }
        public void setMinFileSize(Long minFileSize) { this.minFileSize = minFileSize; }
        
        public Long getMaxFileSize() { return maxFileSize; }
        public void setMaxFileSize(Long maxFileSize) { this.maxFileSize = maxFileSize; }
        
        public Boolean getIsCompressed() { return isCompressed; }
        public void setIsCompressed(Boolean isCompressed) { this.isCompressed = isCompressed; }
        
        public String getUploadSource() { return uploadSource; }
        public void setUploadSource(String uploadSource) { this.uploadSource = uploadSource; }
        
        public String getUploadedAfter() { return uploadedAfter; }
        public void setUploadedAfter(String uploadedAfter) { this.uploadedAfter = uploadedAfter; }
        
        public String getUploadedBefore() { return uploadedBefore; }
        public void setUploadedBefore(String uploadedBefore) { this.uploadedBefore = uploadedBefore; }
        
        public String getModifiedAfter() { return modifiedAfter; }
        public void setModifiedAfter(String modifiedAfter) { this.modifiedAfter = modifiedAfter; }
        
        public String getModifiedBefore() { return modifiedBefore; }
        public void setModifiedBefore(String modifiedBefore) { this.modifiedBefore = modifiedBefore; }
        
        public String getEntityType() { return entityType; }
        public void setEntityType(String entityType) { this.entityType = entityType; }
        
        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }
        
        public String getLinkType() { return linkType; }
        public void setLinkType(String linkType) { this.linkType = linkType; }
        
        public String getMetadataKey() { return metadataKey; }
        public void setMetadataKey(String metadataKey) { this.metadataKey = metadataKey; }
        
        public String getMetadataValue() { return metadataValue; }
        public void setMetadataValue(String metadataValue) { this.metadataValue = metadataValue; }
        
        public Boolean getHasMetadata() { return hasMetadata; }
        public void setHasMetadata(Boolean hasMetadata) { this.hasMetadata = hasMetadata; }
        
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        
        public Integer getSize() { return size; }
        public void setSize(Integer size) { this.size = size; }
        
        public String getSortBy() { return sortBy; }
        public void setSortBy(String sortBy) { this.sortBy = sortBy; }
        
        public String getSortDirection() { return sortDirection; }
        public void setSortDirection(String sortDirection) { this.sortDirection = sortDirection; }
    }
}
