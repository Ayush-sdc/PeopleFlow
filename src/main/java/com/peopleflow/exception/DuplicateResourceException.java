package com.peopleflow.exception;

/**
 * Thrown when attempting to persist an entity that violates unique constraints
 * (e.g., duplicate employee email or duplicate department name).
 * Mapped to HTTP 409 Conflict by GlobalExceptionHandler.
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}