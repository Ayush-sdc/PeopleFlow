package com.peopleflow.exception;

/**
 * Thrown when a requested resource (Employee, Department) is not found in the database.
 * Mapped to HTTP 404 Not Found by GlobalExceptionHandler.
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}