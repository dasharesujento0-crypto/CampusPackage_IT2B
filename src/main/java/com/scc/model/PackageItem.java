package com.scc.model;

public record PackageItem(
        int packageId,
        int recipientId,
        String trackingNumber,
        String senderName,
        String packageDescription,
        String dateReceived,
        String packageStatus
) {
    @Override
    public String toString() {
        return String.format(
                "[%d] Tracking: %s | Recipient ID: %d | Sender: %s"
                        + "\n    Description: %s | Date Received: %s | Status: %s",
                packageId,
                trackingNumber,
                recipientId,
                senderName,
                packageDescription,
                dateReceived,
                packageStatus
        );
    }
}