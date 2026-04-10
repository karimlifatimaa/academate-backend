package com.example.academatebackend.dto;

import com.example.academatebackend.enums.ContentStatus;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateContentRequest {

    @Size(max = 500)
    private String title;

    private String description;

    private Boolean isFree;

    private ContentStatus status;
}
