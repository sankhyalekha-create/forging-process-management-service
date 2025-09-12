package com.jangid.forging_process_management_service.repositories.document;

import com.jangid.forging_process_management_service.entities.document.DocumentLink;
import com.jangid.forging_process_management_service.entities.document.DocumentLink.EntityType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentLinkRepository extends JpaRepository<DocumentLink, Long> {
    
    // Basic CRUD operations
    Optional<DocumentLink> findByIdAndDeletedFalse(Long id);

    @Query("SELECT COUNT(dl) FROM DocumentLink dl WHERE dl.entityType = :entityType AND " +
           "dl.entityId = :entityId AND dl.document.tenant.id = :tenantId AND " +
           "dl.deleted = false AND dl.document.deleted = false")
    Long getDocumentCountForEntity(@Param("entityType") EntityType entityType, 
                                   @Param("entityId") Long entityId, 
                                   @Param("tenantId") Long tenantId);

    // Soft delete all links for a specific document
    @Modifying
    @Query("UPDATE DocumentLink dl SET dl.deleted = true WHERE dl.document.id = :documentId")
    void softDeleteByDocumentId(@Param("documentId") Long documentId);
    
}
