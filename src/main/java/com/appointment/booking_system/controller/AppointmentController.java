package com.appointment.booking_system.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.appointment.booking_system.dto.AppointmentBookingRequest;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.service.AppointmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.appointment.booking_system.repository.AppointmentRepository;
import com.appointment.booking_system.service.PdfService;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PdfService pdfService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;



    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(
            @Valid @RequestBody AppointmentBookingRequest request) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User patient = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient not found!"));

        Appointment appointment = appointmentService.bookAppointment(
                patient.getId(),
                request.getSlotId(),
                request.getNotes()
        );

        return ResponseEntity.ok(
                "Appointment booked successfully with ID: " + appointment.getId()
        );
    }



    @PutMapping("/cancel/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User patient = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient not found!"));

        Appointment appointment =
                appointmentService.cancelAppointment(
                        appointmentId,
                        patient.getId()
                );

        return ResponseEntity.ok(
                "Appointment cancelled successfully. Slot is now available again."
        );
    }

    @GetMapping("/my")
    public ResponseEntity<List<Appointment>> getMyAppointments() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User patient = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Patient not found!"));

        List<Appointment> appointments =
                appointmentService.getMyAppointments(patient.getId());

        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Appointment>> getProviderAppointments(@PathVariable Long providerId) {
        List<Appointment> appointments = appointmentService.getProviderAppointments(providerId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/receipt/{appointmentId}")
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable Long appointmentId) {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Appointment not found"));

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN"));

        boolean isOwner = appointment.getPatient().getId()
                .equals(currentUser.getId());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Appointment not found");
        }

        byte[] pdf = pdfService.generateAppointmentReceipt(appointment);

        return ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=appointment-receipt-"
                                + appointmentId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Appointment>> getAllAppointments() {
        List<Appointment> appointments = appointmentService.getAllAppointments();
        return ResponseEntity.ok(appointments);
    }
}