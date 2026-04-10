package com.example.academatebackend.common.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "Not Found", resource + " not found: " + id);
    }

    public ResourceNotFoundException(String detail) {
        super(HttpStatus.NOT_FOUND, "Not Found", detail);
    }
}
