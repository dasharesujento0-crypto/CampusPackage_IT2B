
package com.scc.model;

public record Recipient(
        int recipientId,
        int departmentId,
        String recipientName,
        String recipientType,
        String identificationNumber,
        String contactNumber,
        String recipientStatus
) {
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | Type: %s | ID: %s | Department ID: %d"
                        + " | Contact: %s | Status: %s",
                recipientId,
                recipientName,
                recipientType,
                identificationNumber,
                departmentId,
                contactNumber == null ? "N/A" : contactNumber,
                recipientStatus
        );
    }
}
