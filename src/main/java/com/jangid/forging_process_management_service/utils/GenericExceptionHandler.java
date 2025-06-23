package com.jangid.forging_process_management_service.utils;

import com.jangid.forging_process_management_service.entitiesRepresentation.error.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Generic exception handler utility for consistent error handling across all API resources
 */
@Slf4j
public final class GenericExceptionHandler {

    private GenericExceptionHandler() {
        throw new IllegalArgumentException("Utility class cannot be instantiated");
    }

    /**
     * Handles exceptions and returns appropriate ResponseEntity with error message
     * 
     * @param exception The exception to handle
     * @param operation The operation being performed (for logging context)
     * @return ResponseEntity with appropriate HTTP status and error message
     */
    public static ResponseEntity<?> handleException(Exception exception, String operation) {
        return handleException(exception, operation, null);
    }

    /**
     * Handles exceptions and returns appropriate ResponseEntity with error message
     * 
     * @param exception The exception to handle
     * @param operation The operation being performed (for logging context)
     * @param defaultMessage Default message to use for INTERNAL_SERVER_ERROR when exception message is null/empty
     * @return ResponseEntity with appropriate HTTP status and error message
     */
    public static ResponseEntity<?> handleException(Exception exception, String operation, String defaultMessage) {
        String exceptionName = exception.getClass().getSimpleName();
        String exceptionMessage = exception.getMessage();
        
        // Log the exception with context
        log.error("Exception in {}: {} - {}", operation, exceptionName, exceptionMessage, exception);

        // Handle specific exception types
        if (isNotFoundException(exception)) {
            return ResponseEntity.notFound().build();
        }
        
        if (isConflictException(exception)) {
            return new ResponseEntity<>(
                new ErrorResponse(getErrorMessage(exceptionMessage, "Resource conflict occurred")), 
                HttpStatus.CONFLICT
            );
        }
        
        if (isBadRequestException(exception)) {
            return new ResponseEntity<>(
                new ErrorResponse(getErrorMessage(exceptionMessage, "Invalid request data")), 
                HttpStatus.BAD_REQUEST
            );
        }
        
        if (isUnauthorizedException(exception)) {
            return new ResponseEntity<>(
                new ErrorResponse(getErrorMessage(exceptionMessage, "Unauthorized access")), 
                HttpStatus.UNAUTHORIZED
            );
        }
        
        if (isForbiddenException(exception)) {
            return new ResponseEntity<>(
                new ErrorResponse(getErrorMessage(exceptionMessage, "Access forbidden")), 
                HttpStatus.FORBIDDEN
            );
        }

        // Default to internal server error for all other exceptions
        String errorMessage = getErrorMessage(
            exceptionMessage, 
            defaultMessage != null ? defaultMessage : ("Error in " + operation)
        );
        
        return new ResponseEntity<>(
            new ErrorResponse(errorMessage), 
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * Gets the error message, using the exception message if available, otherwise the default message
     */
    private static String getErrorMessage(String exceptionMessage, String defaultMessage) {
        return (exceptionMessage != null && !exceptionMessage.trim().isEmpty()) 
            ? exceptionMessage 
            : defaultMessage;
    }

    /**
     * Checks if the exception represents a "not found" condition
     */
    private static boolean isNotFoundException(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("notfound") || 
               exceptionName.contains("resourcenotfound") ||
               exception.getMessage() != null && exception.getMessage().toLowerCase().contains("not found");
    }

    /**
     * Checks if the exception represents a conflict condition (409)
     */
    private static boolean isConflictException(Exception exception) {
        return exception instanceof IllegalStateException ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("already exists") ||
                 exception.getMessage().toLowerCase().contains("duplicate") ||
                 exception.getMessage().toLowerCase().contains("conflict")));
    }

    /**
     * Checks if the exception represents a bad request condition (400)
     */
    private static boolean isBadRequestException(Exception exception) {
        return exception instanceof IllegalArgumentException ||
               exception instanceof NumberFormatException ||
               (exception.getClass().getSimpleName().toLowerCase().contains("validation")) ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("invalid") ||
                 exception.getMessage().toLowerCase().contains("malformed") ||
                 exception.getMessage().toLowerCase().contains("bad request")));
    }

    /**
     * Checks if the exception represents an unauthorized condition (401)
     */
    private static boolean isUnauthorizedException(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("unauthorized") ||
               exceptionName.contains("authentication") ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("unauthorized") ||
                 exception.getMessage().toLowerCase().contains("authentication failed")));
    }

    /**
     * Checks if the exception represents a forbidden condition (403)
     */
    private static boolean isForbiddenException(Exception exception) {
        String exceptionName = exception.getClass().getSimpleName().toLowerCase();
        return exceptionName.contains("forbidden") ||
               exceptionName.contains("access") ||
               (exception.getMessage() != null && 
                (exception.getMessage().toLowerCase().contains("forbidden") ||
                 exception.getMessage().toLowerCase().contains("access denied")));
    }
} 