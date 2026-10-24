package com.appointment.booking_system.controller;

import com.appointment.booking_system.dto.PatientProfileDTO;
import com.appointment.booking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patient/profile")
public class PatientProfileController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<PatientProfileDTO> getProfile(
            Authentication authentication) {

        PatientProfileDTO profile =
                userService.getPatientProfile(authentication.getName());

        return ResponseEntity.ok(profile);
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(
            @RequestBody PatientProfileDTO profileDTO,
            Authentication authentication) {

        PatientProfileDTO updatedProfile =
                userService.updatePatientProfile(
                        authentication.getName(),
                        profileDTO
                );

        String newToken =
                userService.generatePatientToken(updatedProfile.getEmail());

        return ResponseEntity.ok(Map.of(
                "profile", updatedProfile,
                "token", newToken
        ));
    }
}