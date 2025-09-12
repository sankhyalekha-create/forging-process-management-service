package com.jangid.forging_process_management_service.repositories.document;

import com.jangid.forging_process_management_service.entities.document.Document;
import com.jangid.forging_process_management_service.entities.document.DocumentCategory;
import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.document.DocumentType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {
    
    // Basic CRUD operations
    Optional<Document> findByIdAndDeletedFalse(Long id);

    Optional<Document> findByIdAndTenantIdAndDeletedFalse(Long id, Long tenantId);

    // Find by various criteria
    List<Document> findByTenant_IdAndDocumentCategoryAndDeletedFalse(Long tenantId, DocumentCategory category);

    // Find documents by linked entity
    @Query("SELECT DISTINCT d FROM Document d JOIN d.documentLinks dl WHERE " +
           "d.tenant.id = :tenantId AND d.deleted = false AND dl.deleted = false AND " +
           "dl.entityType = :entityType AND dl.entityId = :entityId")
    List<Document> findByLinkedEntity(@Param("tenantId") Long tenantId, 
                                     @Param("entityType") DocumentLink.EntityType entityType, 
                                     @Param("entityId") Long entityId);
    
    // Basic search methods using JPA query methods
    Page<Document> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(Long tenantId, Pageable pageable);
    

    // Simplified search query with essential filters only
    // Supports: keyword, category, documentType, fileExtension, entityType
    // Using JPQL with explicit casting to avoid PostgreSQL bytea issues
    @Query("SELECT DISTINCT d FROM Document d " +
           "LEFT JOIN d.documentLinks dl " +
           "WHERE d.tenant.id = :tenantId AND d.deleted = false " +
           "AND (:keyword IS NULL OR d.originalFileName LIKE %:keyword% OR " +
           "COALESCE(d.description, '') LIKE %:keyword% OR " +
           "COALESCE(d.tags, '') LIKE %:keyword%) " +
           "AND (:category IS NULL OR d.documentCategory = :category) " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:fileExtension IS NULL OR d.fileExtension = :fileExtension) " +
           "AND (:entityType IS NULL OR (dl.deleted = false AND dl.entityType = :entityType))")
    Page<Document> searchDocumentsSimple(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("category") DocumentCategory category,
            @Param("documentType") DocumentType documentType,
            @Param("fileExtension") String fileExtension,
            @Param("entityType") DocumentLink.EntityType entityType,
            Pageable pageable);

    // Simplified count query that matches the simplified search query
    // Using JPQL with explicit casting to avoid PostgreSQL bytea issues
    @Query("SELECT COUNT(DISTINCT d) FROM Document d " +
           "LEFT JOIN d.documentLinks dl " +
           "WHERE d.tenant.id = :tenantId AND d.deleted = false " +
           "AND (:keyword IS NULL OR " +
           "    LOWER(d.originalFileName) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
           "AND (:category IS NULL OR d.documentCategory = :category) " +
           "AND (:documentType IS NULL OR d.documentType = :documentType) " +
           "AND (:fileExtension IS NULL OR d.fileExtension = :fileExtension) " +
           "AND (:entityType IS NULL OR (dl.deleted = false AND dl.entityType = :entityType))")
    Long countSearchResultsSimple(
            @Param("tenantId") Long tenantId,
            @Param("keyword") String keyword,
            @Param("category") DocumentCategory category,
            @Param("documentType") com.jangid.forging_process_management_service.entities.document.DocumentType documentType,
            @Param("fileExtension") String fileExtension,
            @Param("entityType") DocumentLink.EntityType entityType);

}
