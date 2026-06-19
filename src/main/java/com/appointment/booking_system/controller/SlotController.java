package com.appointment.booking_system.controller;

import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.service.SlotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/slots")
public class SlotController {

    @Autowired
    private SlotService slotService;

    @PostMapping("/create")
    public ResponseEntity<?> createSlot(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        LocalDateTime startTime = LocalDateTime.parse(request.get("startTime"));
        LocalDateTime endTime = LocalDateTime.parse(request.get("endTime"));

        Slot slot = slotService.createSlot(email, startTime, endTime);
        return ResponseEntity.ok("Slot created successfully with ID: " + slot.getId());
    }

    @GetMapping("/available/{providerId}")
    public ResponseEntity<List<Slot>> getAvailableSlots(@PathVariable Long providerId) {
        List<Slot> slots = slotService.getAvailableSlots(providerId);
        return ResponseEntity.ok(slots);
    }

    @GetMapping("/all/{providerId}")
    public ResponseEntity<List<Slot>> getAllSlots(@PathVariable Long providerId) {
        List<Slot> slots = slotService.getAllSlots(providerId);
        return ResponseEntity.ok(slots);
    }
}