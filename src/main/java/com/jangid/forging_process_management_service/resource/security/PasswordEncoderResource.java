package com.jangid.forging_process_management_service.resource.security;

import com.jangid.forging_process_management_service.service.security.InternalPasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
public class PasswordEncoderResource {

    private final InternalPasswordEncoder internalPasswordEncoder;

    @PostMapping("/encodePassword")
        public ResponseEntity<Map<String, String>> encodePassword(@RequestBody Map<String, String> request) {
        String rawPassword = request.get("password");
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Password cannot be empty"));
        }

        String encodedPassword = internalPasswordEncoder.encodePassword(rawPassword);
        return ResponseEntity.ok(Map.of("encodedPassword", encodedPassword));
    }
} 