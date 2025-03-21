package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgingLineAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineRepresentation;
import com.jangid.forging_process_management_service.exception.forging.ForgingLineNotFoundException;
import com.jangid.forging_process_management_service.repositories.forging.ForgingLineRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ForgingLineService {

  @Autowired
  private ForgingLineRepository forgingLineRepository;

  @Autowired
  private TenantService tenantService;

  public Page<ForgingLineRepresentation> getAllForgingLinesByTenant(long tenantId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<ForgingLine> forgingLinePage = forgingLineRepository.findByTenantIdAndDeletedIsFalseOrderByUpdatedAtDesc(tenantId, pageable);
    return forgingLinePage.map(ForgingLineAssembler::dissemble);
  }

  public List<ForgingLine> getAllForgingLinesByTenant(long tenantId) {
    return forgingLineRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
  }

  @Transactional
  public ForgingLineRepresentation createForgingLine(Long tenantId, ForgingLineRepresentation forgingLineRepresentation) {
    ForgingLine forgingLine = ForgingLineAssembler.assemble(forgingLineRepresentation);
    Tenant tenant = tenantService.getTenantById(tenantId);
    forgingLine.setTenant(tenant);
    ForgingLine createdForgingLine = forgingLineRepository.save(forgingLine);
    return ForgingLineAssembler.dissemble(createdForgingLine);
  }

  @Transactional
  public ForgingLine saveForgingLine(ForgingLine forgingLine) {
    return forgingLineRepository.save(forgingLine);
  }

  @Transactional
  public ForgingLine updateForgingLine(Long tenantLongId, Long forgingLineIdLongValue, ForgingLineRepresentation forgingLineRepresentation) {
    Tenant tenant = tenantService.getTenantById(tenantLongId);
    ForgingLine forgingLine = getForgingLineByIdAndTenantId(forgingLineIdLongValue, tenantLongId);

    // Update fields
    forgingLine.setForgingLineName(forgingLineRepresentation.getForgingLineName());
    forgingLine.setForgingDetails(forgingLineRepresentation.getForgingDetails());
    forgingLine.setTenant(tenant);
    return forgingLineRepository.save(forgingLine);
  }

  public ForgingLine getForgingLineByIdAndTenantId(long forgingLineIdLongValue, long tenantLongId) {
    Optional<ForgingLine> forgingLineOptional = forgingLineRepository.findByIdAndTenantIdAndDeletedFalse(forgingLineIdLongValue, tenantLongId);
    if (forgingLineOptional.isEmpty()) {
      log.error("ForgingLine with id=" + forgingLineIdLongValue + " having " + tenantLongId + " not found!");
      throw new ForgingLineNotFoundException("ForgingLine with id=" + forgingLineIdLongValue + " having " + tenantLongId + " not found!");
    }
    return forgingLineOptional.get();
  }

  public boolean isForgingLineByTenantExists(long tenantId) {
    return forgingLineRepository.existsByTenantIdAndDeletedFalse(tenantId);
  }

  @Transactional
  public void deleteForgingLine(long forgingLineId, long tenantId) {
    // Validate tenant exists
    tenantService.isTenantExists(tenantId);

    // Validate forgingLine exists
    ForgingLine forgingLine = getForgingLineByIdAndTenantId(forgingLineId, tenantId);

    // Validate forgingLine status
    if (forgingLine.getForgingLineStatus() != ForgingLine.ForgingLineStatus.FORGE_NOT_APPLIED) {
        log.error("Cannot delete forgingLine as it is not in FORGE_NOT_APPLIED status!");
        throw new IllegalStateException("Cannot delete forgingLine as it is not in FORGE_NOT_APPLIED status!");
    }

    // Soft delete the forgingLine
    forgingLine.setDeleted(true);
    forgingLine.setDeletedAt(LocalDateTime.now());
    forgingLineRepository.save(forgingLine);

    log.info("Successfully deleted forgingLine with id={} for tenant={}", forgingLineId, tenantId);
  }
}

//  public ForgingLine getForgingLineByIdAndTenantId(long forgingLineIdLongValue, long tenantid){
//    Optional<ForgingLine> forgingLineOptional = forgingLineRepository.findByIdAndTenantIdAndDeletedFalse(forgingLineIdLongValue, tenantid);
//    if (forgingLineOptional.isEmpty()){
//      log.error("ForgingLine with id="+forgingLineIdLongValue+" not found!");
//      throw new ResourceNotFoundException("ForgingLine with id="+forgingLineIdLongValue+" not found!");
//    }
//    return forgingLineOptional.get();
//  }
//}
