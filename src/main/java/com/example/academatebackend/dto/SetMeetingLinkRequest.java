package com.example.academatebackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SetMeetingLinkRequest {
    @NotBlank
    private String meetingLink;
}
