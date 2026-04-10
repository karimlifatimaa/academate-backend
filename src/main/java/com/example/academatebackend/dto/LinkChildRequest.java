package com.example.academatebackend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LinkChildRequest {

    @NotBlank
    @Size(min = 6, max = 6)
    private String inviteCode;
}
