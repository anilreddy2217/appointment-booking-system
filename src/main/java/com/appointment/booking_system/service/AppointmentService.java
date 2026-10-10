package com.appointment.booking_system.service;

import com.appointment.booking_system.model.*;
import com.appointment.booking_system.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.appointment.booking_system.exception.AppointmentConflictException;
import com.appointment.booking_system.exception.AppointmentForbiddenException;
import com.appointment.booking_system.exception.ResourceNotFoundException;

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

    @Transactional
    public Appointment bookAppointment(Long patientId, Long slotId, String notes) {
        User patient = userRepository.findById(patientId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Patient not found!"));

        Slot slot = slotRepository.findByIdForUpdate(slotId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Slot not found"));

        if (slot.getStatus() != SlotStatus.AVAILABLE) {
            throw new AppointmentConflictException("Slot is not available!");
        }

        Appointment appointment = appointmentRepository.findBySlotId(slotId)
                .orElseGet(Appointment::new);

        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);
        appointment.setNotes(notes);

        slot.setStatus(SlotStatus.BOOKED);
        slotRepository.save(slot);

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

    public Appointment cancelAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Appointment not found!"));

        if (!appointment.getPatient().getId().equals(patientId)) {
            throw new AppointmentForbiddenException(
                    "You are not authorized to cancel this appointment!"
            );
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only booked appointments can be cancelled."
            );
        }

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