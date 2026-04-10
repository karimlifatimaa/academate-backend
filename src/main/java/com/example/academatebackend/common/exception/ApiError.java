package com.example.academatebackend.common.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private int status;
    private String title;
    private String detail;
    private String traceId;
    private Instant timestamp;
    private List<FieldError> errors;

    @Getter
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
