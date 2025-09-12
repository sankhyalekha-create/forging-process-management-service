package com.jangid.forging_process_management_service.repositories.document;

import com.jangid.forging_process_management_service.entities.document.DocumentMetadata;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentMetadataRepository extends JpaRepository<DocumentMetadata, Long> {
    
    // Soft delete all metadata entries for a specific document
    @Modifying
    @Query("UPDATE DocumentMetadata dm SET dm.deleted = true WHERE dm.document.id = :documentId")
    void softDeleteByDocumentId(@Param("documentId") Long documentId);
}
