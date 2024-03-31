package com.example.Database.exceptions;

public class VersionMismatchException extends Exception {
    public VersionMismatchException() {
        super("Version mismatch");
    }
}
