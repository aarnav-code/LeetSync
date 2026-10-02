package com.leetsync.backend.controller;

import com.leetsync.backend.dto.AuthValidationResponse;
import com.leetsync.backend.service.AuthService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Validated
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/github/validate")
    public AuthValidationResponse validate(@RequestHeader("Authorization") @NotBlank String authorization) {
        return authService.validate(extractBearer(authorization));
    }

    private String extractBearer(String authorization) {
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return authorization;
        }
        return authorization.substring(7).trim();
    }
}
