package com.jangid.forging_process_management_service.exception.document;

/**
 * Exception thrown when document deletion operations fail.
 * This is a checked exception to ensure proper transaction rollback and error handling.
 */
public class DocumentDeletionException extends Exception {
    
    public DocumentDeletionException(String message) {
        super(message);
    }
    
    public DocumentDeletionException(String message, Throwable cause) {
        super(message, cause);
    }
}
