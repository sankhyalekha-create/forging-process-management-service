package com.jangid.forging_process_management_service.entities.document;

import lombok.Getter;

@Getter
public enum DocumentType {
    // Keep it simple - just basic document formats
    PDF("PDF Document"),
    IMAGE("Image File"),
    EXCEL("Excel Spreadsheet"),
    WORD("Word Document"),
    OTHER("Other Document");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

  public static DocumentType fromString(String text) {
        for (DocumentType type : DocumentType.values()) {
            if (type.name().equalsIgnoreCase(text)) {
                return type;
            }
        }
        return OTHER;
    }
    
    // Helper method to determine type from MIME type
    public static DocumentType fromMimeType(String mimeType) {
        if (mimeType == null) return OTHER;
        
        if (mimeType.equals("application/pdf")) {
            return PDF;
        } else if (mimeType.startsWith("image/")) {
            return IMAGE;
        } else if (mimeType.contains("excel") || mimeType.contains("spreadsheet")) {
            return EXCEL;
        } else if (mimeType.contains("word") || mimeType.contains("document")) {
            return WORD;
        }
        
        return OTHER;
    }
}
