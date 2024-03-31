package com.example.BankingSystem.enums;

public enum Role {
    ADMIN, CUSTOMER;
    public static Role fromString(String role) {
        for (Role userRole : Role.values()) {
            if (userRole.toString().equalsIgnoreCase(role)) {
                return userRole;
            }
        }
        throw new IllegalArgumentException("No enum value " + role);
    }
}