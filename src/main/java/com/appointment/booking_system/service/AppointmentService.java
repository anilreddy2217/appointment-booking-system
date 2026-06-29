package com.appointment.booking_system.service;

import com.appointment.booking_system.model.*;
import com.appointment.booking_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    public Appointment bookAppointment(Long patientId, Long slotId, String notes) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() -> new RuntimeException("Patient not found!"));

        Slot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new RuntimeException("Slot not found!"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new RuntimeException("Slot is not available!");
        }

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

        Appointment appointment = new Appointment();
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes(notes);

        Appointment saved = appointmentRepository.save(appointment);

        // Send confirmation email
        emailService.sendBookingConfirmation(
                patient.getEmail(),
                patient.getName(),
                slot.getProvider().getUser().getName(),
                slot.getStartTime().toString()
        );

        return saved;
    }

    public Appointment cancelAppointment(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new RuntimeException("Appointment not found!"));

        appointment.setStatus(AppointmentStatus.CANCELLED);

        Slot slot = appointment.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        Appointment saved = appointmentRepository.save(appointment);

        // Send cancellation email
        emailService.sendCancellationEmail(
                appointment.getPatient().getEmail(),
                appointment.getPatient().getName(),
                slot.getStartTime().toString()
        );

        return saved;
    }

    public List<Appointment> getMyAppointments(Long patientId) {
        return appointmentRepository.findByPatientId(patientId);
    }

    public List<Appointment> getProviderAppointments(Long providerId) {
        return appointmentRepository.findBySlotProviderId(providerId);
    }
    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }
}