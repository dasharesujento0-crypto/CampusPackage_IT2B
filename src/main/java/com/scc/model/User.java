package com.scc.model;

public record User(int id, String username, String password, String fullName, String role, String accountStatus) {

    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - %s - Status: %s", id, username, fullName, role, accountStatus);
    }
}