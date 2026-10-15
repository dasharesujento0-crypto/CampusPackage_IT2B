
package com.scc.model;

public record Department(
        int departmentId,
        String departmentName,
        String departmentType,
        String contactNumber,
        String departmentStatus
) {
    @Override
    public String toString() {
        return String.format(
                "[%d] %s | Type: %s | Contact: %s | Status: %s",
                departmentId,
                departmentName,
                departmentType,
                contactNumber,
                departmentStatus
        );
    }
}