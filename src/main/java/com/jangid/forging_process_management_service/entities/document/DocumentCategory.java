package com.jangid.forging_process_management_service.entities.document;

import lombok.Getter;

@Getter
public enum DocumentCategory {
    // Keep it simple - just 6 basic categories
    INVOICE("Invoice & Purchase"),
    CERTIFICATE("Certificate & Quality"),
    PROCESS("Process Documentation"),
    SPECIFICATION("Specification & Drawing"),
    REPORT("Report & Log"),
    IDENTITY("Identity & Verification"),
    OTHER("Other Documents");

    private final String displayName;

    DocumentCategory(String displayName) {
        this.displayName = displayName;
    }

  public static DocumentCategory fromString(String text) {
        for (DocumentCategory category : DocumentCategory.values()) {
            if (category.name().equalsIgnoreCase(text)) {
                return category;
            }
        }
        return OTHER;
    }
    
    /**
     * Get lowercase path component for file system organization
     */
    public String getPathComponent() {
        return this.name().toLowerCase();
    }
}
