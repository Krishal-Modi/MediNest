package com.example.MediNest.exceptions;

import lombok.Data;

@Data
public class AuthorizationException extends RuntimeException {
    public AuthorizationException(String message) {
        super(message);
    }
}
