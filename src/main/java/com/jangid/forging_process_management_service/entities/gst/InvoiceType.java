package com.jangid.forging_process_management_service.entities.gst;

import lombok.Getter;

/**
 * Invoice types as per GST compliance requirements.
 * These values must match the database CHECK constraint in the invoice table.
 */
@Getter
public enum InvoiceType {
    TAX_INVOICE("Tax Invoice"),                    // Standard GST invoice
    BILL_OF_SUPPLY("Bill of Supply"),              // For composition dealers or exempt goods
    EXPORT_INVOICE("Export Invoice"),              // For export transactions
    REVISED_INVOICE("Revised Invoice"),            // For amendments/corrections
    CREDIT_NOTE("Credit Note"),                    // For returns or reductions
    DEBIT_NOTE("Debit Note");                      // For additional charges

    private final String displayName;

    InvoiceType(String displayName) {
        this.displayName = displayName;
    }
}
