package com.jangid.forging_process_management_service.service.gst;

import com.jangid.forging_process_management_service.dto.gst.EwayBillData;
import com.jangid.forging_process_management_service.dto.gst.gsp.GspEwbGenerateResponse;
import com.jangid.forging_process_management_service.entities.gst.DeliveryChallan;
import com.jangid.forging_process_management_service.entities.gst.Invoice;
import com.jangid.forging_process_management_service.entities.gst.TransportationMode;
import com.jangid.forging_process_management_service.repositories.gst.DeliveryChallanRepository;
import com.jangid.forging_process_management_service.repositories.gst.InvoiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Service for E-Way Bill generation operations
 * Handles business logic, retries, and entity updates for both Invoice and DeliveryChallan
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EwayBillGenerationService {

    private final GspEwayBillService gspEwayBillService;
    private final InvoiceRepository invoiceRepository;
    private final DeliveryChallanRepository challanRepository;

    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000; // 1 second between retries
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm:ss a", Locale.ENGLISH);

    /**
     * Strategy interface for updating entities with E-Way Bill details
     * Allows generic handling of Invoice and DeliveryChallan
     */
    private interface EwayBillEntityUpdater<T> {
        String getDocumentNumber(T entity);
        void updateEwayBillDetails(T entity, EwayBillData request, GspEwbGenerateResponse response);
        void save(T entity);
    }

    /**
     * Invoice updater implementation
     */
    private class InvoiceUpdater implements EwayBillEntityUpdater<Invoice> {
        @Override
        public String getDocumentNumber(Invoice invoice) {
            return invoice.getInvoiceNumber();
        }

        @Override
        public void updateEwayBillDetails(Invoice invoice, EwayBillData request, GspEwbGenerateResponse response) {
            log.info("Updating invoice {} with E-Way Bill details: {}", 
                    invoice.getInvoiceNumber(), response.getEwayBillNo());

            // Update E-Way Bill response fields
            invoice.setEwayBillNumber(String.valueOf(response.getEwayBillNo()));
            
            if (response.getAlert() != null && !response.getAlert().isEmpty()) {
                invoice.setEwayBillAlertMessage(response.getAlert());
            }

            // Parse and set dates
            parseDates(invoice::setEwayBillDate, invoice::setEwayBillValidUntil, response);

            // Update E-Way Bill form fields from request (Part A)
            invoice.setEwayBillSupplyType(request.getSupplyType());
            invoice.setEwayBillSubSupplyType(request.getSubSupplyType());
            invoice.setEwayBillDocType(request.getDocType());
            invoice.setEwayBillTransactionType(String.valueOf(request.getTransactionType()));
            
            // Update Transport Details from request (Part B)
            if (request.getTransporterId() != null) {
                invoice.setTransporterId(request.getTransporterId());
            }
            if (request.getTransporterName() != null) {
                invoice.setTransporterName(request.getTransporterName());
            }
            if (request.getTransMode() != null) {
                invoice.setTransportationMode(mapTransportMode(request.getTransMode()));
            }
            if (request.getVehicleNo() != null) {
                invoice.setVehicleNumber(request.getVehicleNo());
            }
            if (request.getTransDocNo() != null) {
                invoice.setTransportDocumentNumber(request.getTransDocNo());
            }
            if (request.getTransDocDate() != null) {
                invoice.setTransportDocumentDate(parseTransportDocumentDate(request.getTransDocDate()));
            }
        }

        @Override
        public void save(Invoice invoice) {
            invoiceRepository.save(invoice);
        }
    }

    /**
     * DeliveryChallan updater implementation
     */
    private class ChallanUpdater implements EwayBillEntityUpdater<DeliveryChallan> {
        @Override
        public String getDocumentNumber(DeliveryChallan challan) {
            return challan.getChallanNumber();
        }

        @Override
        public void updateEwayBillDetails(DeliveryChallan challan, EwayBillData request, GspEwbGenerateResponse response) {
            log.info("Updating challan {} with E-Way Bill details: {}", 
                    challan.getChallanNumber(), response.getEwayBillNo());

            // Update E-Way Bill response fields
            challan.setEwayBillNumber(String.valueOf(response.getEwayBillNo()));
            
            if (response.getAlert() != null && !response.getAlert().isEmpty()) {
                challan.setEwayBillAlertMessage(response.getAlert());
            }

            // Parse and set dates
            parseDates(challan::setEwayBillDate, challan::setEwayBillValidUntil, response);

            // Update E-Way Bill form fields from request (Part A)
            challan.setEwayBillSupplyType(request.getSupplyType());
            challan.setEwayBillSubSupplyType(request.getSubSupplyType());
            challan.setEwayBillDocType(request.getDocType());
            challan.setEwayBillTransactionType(String.valueOf(request.getTransactionType()));
            
            // Update Transport Details from request (Part B)
            if (request.getTransporterId() != null) {
                challan.setTransporterId(request.getTransporterId());
            }
            if (request.getTransporterName() != null) {
                challan.setTransporterName(request.getTransporterName());
            }
            if (request.getTransMode() != null) {
                challan.setTransportationMode(mapTransportMode(request.getTransMode()));
            }
            if (request.getVehicleNo() != null) {
                challan.setVehicleNumber(request.getVehicleNo());
            }
            if (request.getTransDocNo() != null) {
                challan.setTransportDocumentNumber(request.getTransDocNo());
            }
            if (request.getTransDocDate() != null) {
                challan.setTransportDocumentDate(parseTransportDocumentDate(request.getTransDocDate()));
            }
        }

        @Override
        public void save(DeliveryChallan challan) {
            challanRepository.save(challan);
        }
    }

    /**
     * Generate E-Way Bill via GSP API with retry logic for Invoice
     * 
     * @param tenantId Tenant ID
     * @param invoice Invoice entity
     * @param request E-Way Bill data
     * @param sessionToken Session token for authentication
     * @param gspServerId GSP Server ID selected by user
     * @return GSP API response
     * @throws IllegalArgumentException if invoice not found or validation fails
     * @throws RuntimeException if generation fails after retries
     */
    @Transactional
    public GspEwbGenerateResponse generateEwayBillWithRetry(Long tenantId, Invoice invoice, EwayBillData request, 
                                                            String sessionToken, String gspServerId) {
        return generateEwayBillWithRetry(tenantId, invoice, request, new InvoiceUpdater(), sessionToken, gspServerId);
    }

    /**
     * Generate E-Way Bill via GSP API with retry logic for DeliveryChallan
     * 
     * @param tenantId Tenant ID
     * @param challan DeliveryChallan entity
     * @param request E-Way Bill data
     * @param sessionToken Session token for authentication
     * @param gspServerId GSP Server ID selected by user
     * @return GSP API response
     * @throws IllegalArgumentException if challan not found or validation fails
     * @throws RuntimeException if generation fails after retries
     */
    @Transactional
    public GspEwbGenerateResponse generateEwayBillWithRetry(Long tenantId, DeliveryChallan challan, EwayBillData request,
                                                            String sessionToken, String gspServerId) {
        return generateEwayBillWithRetry(tenantId, challan, request, new ChallanUpdater(), sessionToken, gspServerId);
    }

    /**
     * Generic E-Way Bill generation with retry logic
     * Works for both Invoice and DeliveryChallan using strategy pattern
     * 
     * @param tenantId Tenant ID
     * @param entity Invoice or DeliveryChallan entity
     * @param request E-Way Bill data
     * @param updater Entity updater strategy
     * @param sessionToken Session token for authentication
     * @param gspServerId GSP Server ID selected by user
     * @return GSP API response
     */
    private <T> GspEwbGenerateResponse generateEwayBillWithRetry(
            Long tenantId, T entity, EwayBillData request, EwayBillEntityUpdater<T> updater, String sessionToken, String gspServerId) {
        
        String documentNumber = updater.getDocumentNumber(entity);
        log.info("Starting E-Way Bill generation for document: {}, tenant: {}", documentNumber, tenantId);

        // Ensure DocNo matches document number
        if (!documentNumber.equals(request.getDocNo())) {
            log.warn("DocNo mismatch: expected {}, got {}. Using document number.", 
                    documentNumber, request.getDocNo());
            request.setDocNo(documentNumber);
        }

        // Attempt to generate E-Way Bill with retries
        GspEwbGenerateResponse response = generateWithRetry(tenantId, request, sessionToken, gspServerId);

        // Update entity if successful
        if (response.isSuccess()) {
            updateEntityWithEwayBillDetails(entity, request, response, updater);
        }

        return response;
    }

    /**
     * Generate E-Way Bill with retry logic
     * 
     * @param tenantId Tenant ID
     * @param request E-Way Bill data
     * @param sessionToken Session token for authentication
     * @param gspServerId GSP Server ID selected by user
     * @return GSP API response
     */
    private GspEwbGenerateResponse generateWithRetry(Long tenantId, EwayBillData request, String sessionToken, String gspServerId) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRY_ATTEMPTS; attempt++) {
            try {
                log.info("E-Way Bill generation attempt {} of {} for DocNo: {}", 
                        attempt, MAX_RETRY_ATTEMPTS, request.getDocNo());

                GspEwbGenerateResponse response = gspEwayBillService.generateEwayBill(tenantId, request, sessionToken, gspServerId);

                if (response.isSuccess()) {
                    log.info("E-Way Bill generated successfully on attempt {}: {}", 
                            attempt, response.getEwayBillNo());
                    return response;
                } else {
                    // GSP API returned error
                    String errorMessage = response.getErrorMessage() != null ? 
                            response.getErrorMessage() : "Unknown error from GSP API";
                    log.warn("GSP API returned error on attempt {}: {}", attempt, errorMessage);
                    
                    // If it's a validation error, don't retry
                    if (isValidationError(errorMessage)) {
                        log.error("Validation error from GSP, not retrying: {}", errorMessage);
                        throw new IllegalArgumentException(errorMessage);
                    }

                    lastException = new RuntimeException(errorMessage);
                }

            } catch (IllegalArgumentException e) {
                // Don't retry validation errors or 400 Bad Request
                log.error("Validation error on attempt {}: {}", attempt, e.getMessage());
                throw e;
            } catch (Exception e) {
                log.error("Error on attempt {} of {}: {}", attempt, MAX_RETRY_ATTEMPTS, e.getMessage(), e);
                
                // Check if it's a 400 Bad Request error - don't retry these
                if (isBadRequestError(e)) {
                    log.error("400 Bad Request error, not retrying: {}", e.getMessage());
                    throw new IllegalArgumentException("Bad Request: " + e.getMessage(), e);
                }
                
                lastException = e;
            }

            // Wait before retrying (except on last attempt)
            if (attempt < MAX_RETRY_ATTEMPTS) {
                try {
                    log.debug("Waiting {}ms before retry...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("E-Way Bill generation interrupted", ie);
                }
            }
        }

        // All attempts failed
        String errorMessage = lastException != null ? lastException.getMessage() : 
                "E-Way Bill generation failed after " + MAX_RETRY_ATTEMPTS + " attempts";
        log.error("E-Way Bill generation failed after {} attempts for DocNo: {}", 
                MAX_RETRY_ATTEMPTS, request.getDocNo());
        throw new RuntimeException(errorMessage, lastException);
    }

    /**
     * Check if error is a validation error (don't retry these)
     */
    private boolean isValidationError(String errorMessage) {
        if (errorMessage == null) {
            return false;
        }

        String lowerError = errorMessage.toLowerCase();
        return lowerError.contains("invalid") ||
               lowerError.contains("validation") ||
               lowerError.contains("required") ||
               lowerError.contains("format") ||
               lowerError.contains("duplicate") ||
               lowerError.contains("already exists");
    }

    /**
     * Check if error is a 400 Bad Request (don't retry these)
     */
    private boolean isBadRequestError(Exception exception) {
        if (exception == null) {
            return false;
        }

        String errorMessage = exception.getMessage();
        if (errorMessage == null) {
            return false;
        }

        String lowerError = errorMessage.toLowerCase();
        // Check for HTTP 400 Bad Request indicators
        return lowerError.contains("400") ||
               lowerError.contains("bad request") ||
               lowerError.contains("status code 400") ||
               lowerError.contains("status_cd") && lowerError.contains("0");
    }

    /**
     * Update entity with E-Way Bill details using strategy pattern
     */
    private <T> void updateEntityWithEwayBillDetails(
            T entity, EwayBillData request, GspEwbGenerateResponse response, EwayBillEntityUpdater<T> updater) {
        try {
            updater.updateEwayBillDetails(entity, request, response);
            updater.save(entity);
            log.info("Entity {} successfully updated with E-Way Bill details", updater.getDocumentNumber(entity));
        } catch (Exception e) {
            log.error("Failed to update entity with E-Way Bill details: {}", e.getMessage(), e);
            // Log error but don't fail the E-Way Bill generation
            // The E-Way Bill was generated successfully, just the local update failed
        }
    }

    /**
     * Parse and set E-Way Bill dates using functional interfaces
     */
    private void parseDates(
            Consumer<LocalDateTime> setEwayBillDate,
            Consumer<LocalDateTime> setEwayBillValidUntil,
            GspEwbGenerateResponse response) {
        try {
            if (response.getEwayBillDate() != null && !response.getEwayBillDate().isEmpty()) {
                setEwayBillDate.accept(LocalDateTime.parse(response.getEwayBillDate(), DATE_FORMATTER));
            }
            if (response.getValidUpto() != null && !response.getValidUpto().isEmpty()) {
                setEwayBillValidUntil.accept(LocalDateTime.parse(response.getValidUpto(), DATE_FORMATTER));
            }
        } catch (Exception e) {
            log.warn("Could not parse E-Way Bill dates: {}", e.getMessage());
            // Continue even if date parsing fails
        }
    }

    /**
     * Map E-Way Bill transport mode integer to TransportationMode enum
     * 
     * @param transMode Transport mode from E-Way Bill (1=Road, 2=Rail, 3=Air, 4=Ship)
     * @return TransportationMode enum
     */
    private TransportationMode mapTransportMode(Integer transMode) {
        if (transMode == null) {
            return TransportationMode.ROAD; // Default
        }
        
        switch (transMode) {
            case 1:
                return TransportationMode.ROAD;
            case 2:
                return TransportationMode.RAIL;
            case 3:
                return TransportationMode.AIR;
            case 4:
                return TransportationMode.SHIP;
            default:
                log.warn("Unknown transport mode: {}, defaulting to ROAD", transMode);
                return TransportationMode.ROAD;
        }
    }

    /**
     * Parse transport document date from DD/MM/YYYY format to LocalDate
     * 
     * @param dateString Date in DD/MM/YYYY format
     * @return LocalDate or null if parsing fails
     */
    private LocalDate parseTransportDocumentDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        
        try {
            // Expected format: DD/MM/YYYY
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            return java.time.LocalDate.parse(dateString, formatter);
        } catch (Exception e) {
            log.warn("Could not parse transport document date: {}, error: {}", dateString, e.getMessage());
            return null;
        }
    }
}
