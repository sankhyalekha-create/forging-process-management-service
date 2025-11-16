package com.jangid.forging_process_management_service.dto.gst;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for cancelling a delivery challan
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CancelChallanRequest {

  @NotBlank(message = "Cancelled by is required")
  @Size(max = 100, message = "Cancelled by cannot exceed 100 characters")
  private String cancelledBy;

  @NotBlank(message = "Cancellation reason is required")
  @Size(max = 500, message = "Cancellation reason cannot exceed 500 characters")
  private String cancellationReason;

  @NotNull(message = "Cancellation date is required")
  private LocalDateTime cancelledAt;
}


