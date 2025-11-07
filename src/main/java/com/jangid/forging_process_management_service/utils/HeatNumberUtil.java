package com.jangid.forging_process_management_service.utils;

import com.jangid.forging_process_management_service.dto.HeatInfoDTO;
import com.jangid.forging_process_management_service.service.workflow.ItemWorkflowService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for heat number operations
 * Provides common functionality for fetching and formatting heat numbers
 */
@Slf4j
@Component
public class HeatNumberUtil {

  private final ItemWorkflowService itemWorkflowService;

  public HeatNumberUtil(ItemWorkflowService itemWorkflowService) {
    this.itemWorkflowService = itemWorkflowService;
  }

  /**
   * Fetch and format heat numbers from ItemWorkflow
   * 
   * @param itemWorkflowId The ItemWorkflow ID
   * @return Comma-separated heat numbers (e.g., "HT-001, HT-002"), or null if no heats found
   */
  public String getHeatNumbersFromItemWorkflow(Long itemWorkflowId) {
    if (itemWorkflowId == null) {
      return null;
    }

    try {
      // Get available heats from the first operation of the item workflow
      List<HeatInfoDTO> heats = itemWorkflowService.getAvailableHeatsFromFirstOperation(itemWorkflowId);
      
      if (heats == null || heats.isEmpty()) {
        log.debug("No heats found for itemWorkflowId: {}", itemWorkflowId);
        return null;
      }

      // Extract heat numbers and format as comma-separated string
      String heatNumbers = heats.stream()
        .map(HeatInfoDTO::getHeatNumber)
        .filter(heatNumber -> heatNumber != null && !heatNumber.trim().isEmpty())
        .collect(Collectors.joining(", "));

      log.debug("Found heat numbers for itemWorkflowId {}: {}", itemWorkflowId, heatNumbers);
      return heatNumbers.isEmpty() ? null : heatNumbers;

    } catch (Exception e) {
      log.error("Error fetching heat numbers for itemWorkflowId {}: {}", itemWorkflowId, e.getMessage());
      // Return null instead of throwing exception to prevent display failures
      return null;
    }
  }
}

