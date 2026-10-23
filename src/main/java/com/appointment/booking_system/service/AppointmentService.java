package com.appointment.booking_system.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private ApplicationEventPublisher eventPublisher;

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
        eventPublisher.publishEvent(
                new AppointmentBookedEvent(
                        patient.getEmail(),
                        patient.getName(),
                        slot.getProvider().getUser().getName(),
                        slot.getStartTime().toString()
                )
        );


        return saved;
    }

    @Transactional
    public Appointment cancelAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
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

        String patientEmail = appointment.getPatient().getEmail();
        String patientName = appointment.getPatient().getName();
        String appointmentTime = slot.getStartTime().toString();

        eventPublisher.publishEvent(
                new AppointmentCancelledEvent(
                        appointment.getPatient().getEmail(),
                        appointment.getPatient().getName(),
                        slot.getStartTime().toString()
                )
        );

        return saved;
    }

    public Page<Appointment> getMyAppointments(Long patientId, Pageable pageable) {
        return appointmentRepository.findByPatientId(patientId, pageable);
    }

    public Page<Appointment> getProviderAppointments(Long providerId, Pageable pageable) {
        return appointmentRepository.findBySlotProviderId(providerId, pageable);
    }

    public Page<Appointment> getAllAppointments(Pageable pageable) {
        return appointmentRepository.findAll(pageable);
    }
}