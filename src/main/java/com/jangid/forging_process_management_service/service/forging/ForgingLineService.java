package com.jangid.forging_process_management_service.service.forging;

import com.jangid.forging_process_management_service.assemblers.forging.ForgingLineAssembler;
import com.jangid.forging_process_management_service.entities.Tenant;
import com.jangid.forging_process_management_service.entities.forging.ForgingLine;
import com.jangid.forging_process_management_service.entitiesRepresentation.forging.ForgingLineRepresentation;
import com.jangid.forging_process_management_service.exception.ResourceNotFoundException;
import com.jangid.forging_process_management_service.repositories.forging.ForgingLineRepository;
import com.jangid.forging_process_management_service.service.TenantService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ForgingLineService {

  @Autowired
  private ForgingLineRepository forgingLineRepository;

  @Autowired
  private TenantService tenantService;

  public List<ForgingLine> getAllForgingLinesByTenant(long tenantId) {
    return forgingLineRepository.findByTenantIdAndDeletedIsFalseOrderByCreatedAtDesc(tenantId);
  }

  public Optional<ForgingLine> getForgingLineById(Long id) {
    return forgingLineRepository.findById(id);
  }

  public ForgingLineRepresentation createForgingLine(Long tenantId, ForgingLineRepresentation forgingLineRepresentation) {
    ForgingLine forgingLine = ForgingLineAssembler.assemble(forgingLineRepresentation);
    Tenant tenant = tenantService.getTenantById(tenantId);
    forgingLine.setTenant(tenant);
    ForgingLine createdForgingLine = forgingLineRepository.save(forgingLine);
    return ForgingLineAssembler.dissemble(createdForgingLine);
  }

  public ForgingLine updateForgingLine(Long id, ForgingLine forgingLineDetails) {
    ForgingLine forgingLine = forgingLineRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("ForgingLine not found with id " + id));

    // Update fields
    forgingLine.setForgingLineName(forgingLineDetails.getForgingLineName());
    forgingLine.setForgingDetails(forgingLineDetails.getForgingDetails());
    forgingLine.setForgingStatus(forgingLineDetails.getForgingStatus());

    return forgingLineRepository.save(forgingLine);
  }

  public void deleteForgingLine(Long id) {
    forgingLineRepository.deleteById(id);
  }
}
