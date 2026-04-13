package com.example.academatebackend.controller;

import com.example.academatebackend.service.ZoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Profile("dev")
public class TestController {

    private final ZoomService zoomService;

    @GetMapping("/zoom")
    public ResponseEntity<?> testZoom() {
        String link = zoomService.createMeeting(
                "Academate — Test Dərsi",
                LocalDateTime.now().plusHours(1),
                60
        );

        if (link == null) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("success", false, "error", "Zoom linki yaradıla bilmədi"));
        }

        return ResponseEntity.ok(Map.of("success", true, "zoomLink", link));
    }
}
