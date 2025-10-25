package com.jangid.forging_process_management_service.repositories.transporter;

import com.jangid.forging_process_management_service.entities.transporter.Transporter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Transporter entity.
 * Provides CRUD operations and custom query methods.
 */
@Repository
public interface TransporterRepository extends CrudRepository<Transporter, Long> {
  
  /**
   * Find a transporter by ID and tenant ID where deleted is false.
   *
   * @param id the transporter ID
   * @param tenantId the tenant ID
   * @return Optional containing the transporter if found
   */
  Optional<Transporter> findByIdAndTenantIdAndDeletedFalse(long id, long tenantId);
  
  /**
   * Find a transporter by ID where deleted is false.
   *
   * @param id the transporter ID
   * @return Optional containing the transporter if found
   */
  Optional<Transporter> findByIdAndDeletedFalse(long id);
  
  /**
   * Find all active transporters for a tenant with pagination, ordered by creation date.
   *
   * @param tenantId the tenant ID
   * @param pageable pagination information
   * @return Page of transporters
   */
  Page<Transporter> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId, Pageable pageable);
  
  /**
   * Find all active transporters for a tenant without pagination, ordered by creation date.
   *
   * @param tenantId the tenant ID
   * @return List of transporters
   */
  List<Transporter> findByTenantIdAndDeletedFalseOrderByCreatedAtDesc(long tenantId);
  
  /**
   * Search transporters by name (case-insensitive partial match).
   *
   * @param transporterName the transporter name to search
   * @param tenantId the tenant ID
   * @return List of matching transporters
   */
  List<Transporter> findByTransporterNameContainingIgnoreCaseAndTenantIdAndDeletedFalse(String transporterName, long tenantId);
  
  /**
   * Search transporters by GSTIN (exact match).
   *
   * @param gstin the GSTIN to search
   * @param tenantId the tenant ID
   * @return List of matching transporters
   */
  List<Transporter> findByGstinAndTenantIdAndDeletedFalse(String gstin, long tenantId);
  
  /**
   * Search transporters by Transporter ID Number (exact match).
   *
   * @param transporterIdNumber the transporter ID number to search
   * @param tenantId the tenant ID
   * @return List of matching transporters
   */
  List<Transporter> findByTransporterIdNumberAndTenantIdAndDeletedFalse(String transporterIdNumber, long tenantId);
  
  /**
   * Check if an active transporter exists with the given name.
   *
   * @param transporterName the transporter name
   * @param tenantId the tenant ID
   * @return true if exists, false otherwise
   */
  boolean existsByTransporterNameAndTenantIdAndDeletedFalse(String transporterName, long tenantId);
  
  /**
   * Check if an active transporter exists with the given GSTIN.
   *
   * @param gstin the GSTIN
   * @param tenantId the tenant ID
   * @return true if exists, false otherwise
   */
  boolean existsByGstinAndTenantIdAndDeletedFalse(String gstin, long tenantId);
  
  /**
   * Check if an active transporter exists with the given Transporter ID Number.
   *
   * @param transporterIdNumber the transporter ID number
   * @param tenantId the tenant ID
   * @return true if exists, false otherwise
   */
  boolean existsByTransporterIdNumberAndTenantIdAndDeletedFalse(String transporterIdNumber, long tenantId);
  
  /**
   * Find a deleted transporter by name for reactivation.
   *
   * @param transporterName the transporter name
   * @param tenantId the tenant ID
   * @return Optional containing the deleted transporter if found
   */
  Optional<Transporter> findByTransporterNameAndTenantIdAndDeletedTrue(String transporterName, long tenantId);
}

