package com.appointment.booking_system.controller;

import java.util.List;
import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.service.ProviderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/providers")
public class ProviderController {

    @Autowired
    private ProviderService providerService;

    @PostMapping("/profile")
    public ResponseEntity<?> createProfile(@RequestHeader("Authorization") String authHeader,
                                           @RequestBody Map<String, String> request) {
        String email = request.get("email");
        String specialty = request.get("specialty");
        String bio = request.get("bio");
        String location = request.get("location");
        String phone = request.get("phone");

        Provider provider = providerService.createProfile(email, specialty, bio, location, phone);
        return ResponseEntity.ok("Provider profile created for: " + provider.getUser().getName());
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestParam String email) {
        Provider provider = providerService.getProfile(email);
        return ResponseEntity.ok(provider);
    }
    @GetMapping("/search")
    public ResponseEntity<List<Provider>> searchProviders(@RequestParam String specialty) {
        List<Provider> providers = providerService.searchBySpecialty(specialty);
        return ResponseEntity.ok(providers);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Provider>> getAllProviders() {
        List<Provider> providers = providerService.getAllProviders();
        return ResponseEntity.ok(providers);
    }
}