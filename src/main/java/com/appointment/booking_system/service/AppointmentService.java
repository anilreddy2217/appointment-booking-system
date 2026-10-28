package com.appointment.booking_system.service;

import java.time.LocalDateTime;

import com.appointment.booking_system.dto.AppointmentStatusHistoryResponse;
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
import com.appointment.booking_system.repository.AppointmentStatusHistoryRepository;


import java.util.List;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentStatusHistoryRepository statusHistoryRepository;

    @Autowired
    private ProviderRepository providerRepository;

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

        if (slot.getEndTime() == null
                || !slot.getEndTime().isAfter(LocalDateTime.now())) {
            throw new AppointmentConflictException(
                    "This slot has expired and can no longer be booked."
            );
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

        AppointmentStatus previousStatus = appointment.getStatus();

        appointment.setStatus(AppointmentStatus.CANCELLED);

        Slot slot = appointment.getSlot();
        slot.setStatus(SlotStatus.AVAILABLE);
        slotRepository.save(slot);

        Appointment saved = appointmentRepository.save(appointment);

        saveStatusHistory(
                saved,
                previousStatus,
                AppointmentStatus.CANCELLED,
                appointment.getPatient()
        );

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



    @Transactional
    public Appointment updateAppointmentStatus(
            Long appointmentId,
            String providerEmail,
            AppointmentStatus newStatus) {

        if (newStatus != AppointmentStatus.COMPLETED
                && newStatus != AppointmentStatus.MISSED) {
            throw new IllegalArgumentException(
                    "Providers can only mark appointments as COMPLETED or MISSED."
            );
        }

        User providerUser = userRepository.findByEmail(providerEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Provider user not found."));

        if (providerUser.getRole() != Role.PROVIDER) {
            throw new AppointmentForbiddenException(
                    "Only providers can update appointment status."
            );
        }

        Provider provider = providerRepository.findByUserId(providerUser.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException("Provider profile not found."));

        Appointment appointment = appointmentRepository
                .findByIdForUpdate(appointmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Appointment not found."));

        if (!appointment.getSlot().getProvider().getId()
                .equals(provider.getId())) {
            throw new AppointmentForbiddenException(
                    "You are not authorized to update this appointment."
            );
        }

        if (appointment.getStatus() != AppointmentStatus.BOOKED) {
            throw new IllegalStateException(
                    "Only booked appointments can be completed or marked missed."
            );
        }

        LocalDateTime endTime = appointment.getSlot().getEndTime();

        if (endTime == null || endTime.isAfter(LocalDateTime.now())) {
            throw new IllegalStateException(
                    "Appointment status can only be updated after its scheduled end time."
            );
        }

        AppointmentStatus previousStatus = appointment.getStatus();
        appointment.setStatus(newStatus);

        Appointment savedAppointment = appointmentRepository.save(appointment);

        saveStatusHistory(
                savedAppointment,
                previousStatus,
                newStatus,
                providerUser
        );

        return savedAppointment;
    }

    private void saveStatusHistory(
            Appointment appointment,
            AppointmentStatus previousStatus,
            AppointmentStatus newStatus,
            User changedBy) {

        AppointmentStatusHistory history = new AppointmentStatusHistory();
        history.setAppointment(appointment);
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);
        history.setChangedBy(changedBy);

        statusHistoryRepository.save(history);
    }



    @Transactional(readOnly = true)
    public List<AppointmentStatusHistoryResponse> getAppointmentStatusHistory(
            Long appointmentId,
            String requesterEmail) {

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found."));

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Appointment not found."));

        boolean authorized = false;

        if (requester.getRole() == Role.ADMIN) {
            authorized = true;

        } else if (requester.getRole() == Role.PATIENT) {
            authorized = appointment.getPatient().getId()
                    .equals(requester.getId());

        } else if (requester.getRole() == Role.PROVIDER) {
            Provider provider = providerRepository
                    .findByUserId(requester.getId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Provider profile not found."));

            authorized = appointment.getSlot().getProvider().getId()
                    .equals(provider.getId());
        }

        if (!authorized) {
            throw new AppointmentForbiddenException(
                    "You are not authorized to view this appointment's history."
            );
        }

        return statusHistoryRepository
                .findByAppointmentIdOrderByChangedAtDesc(appointmentId)
                .stream()
                .map(history -> new AppointmentStatusHistoryResponse(
                        history.getId(),
                        history.getPreviousStatus(),
                        history.getNewStatus(),
                        history.getChangedBy().getId(),
                        history.getChangedAt()
                ))
                .toList();
    }



}