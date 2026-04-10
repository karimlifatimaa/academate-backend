package com.example.academatebackend.dto;

import com.example.academatebackend.enums.ContentType;
import com.example.academatebackend.enums.Subject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateContentRequest {

    @NotBlank
    @Size(max = 500)
    private String title;

    private String description;

    @NotNull
    private Subject subject;

    @NotNull
    private Short grade;

    private UUID topicId;

    @NotNull
    private ContentType contentType;

    private Boolean isFree = true;
}
