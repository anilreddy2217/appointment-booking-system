package com.appointment.booking_system.controller;

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
    private PdfService pdfService;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody Map<String, String> request) {
        Long patientId = Long.parseLong(request.get("patientId"));
        Long slotId = Long.parseLong(request.get("slotId"));
        String notes = request.get("notes");

        Appointment appointment = appointmentService.bookAppointment(patientId, slotId, notes);
        return ResponseEntity.ok("Appointment booked successfully with ID: " + appointment.getId());
    }

    @PutMapping("/cancel/{appointmentId}")
    public ResponseEntity<?> cancelAppointment(@PathVariable Long appointmentId) {
        Appointment appointment = appointmentService.cancelAppointment(appointmentId);
        return ResponseEntity.ok("Appointment cancelled successfully. Slot is now available again.");
    }

    @GetMapping("/my/{patientId}")
    public ResponseEntity<List<Appointment>> getMyAppointments(@PathVariable Long patientId) {
        List<Appointment> appointments = appointmentService.getMyAppointments(patientId);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Appointment>> getProviderAppointments(@PathVariable Long providerId) {
        List<Appointment> appointments = appointmentService.getProviderAppointments(providerId);
        return ResponseEntity.ok(appointments);
    }
    @GetMapping("/receipt/{appointmentId}")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found!"));

        byte[] pdf = pdfService.generateAppointmentReceipt(appointment);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=appointment-receipt-" + appointmentId + ".pdf")
                .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}