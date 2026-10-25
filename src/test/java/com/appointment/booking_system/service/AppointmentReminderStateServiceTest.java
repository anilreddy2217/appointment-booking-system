package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.AppointmentReminder;
import com.appointment.booking_system.model.AppointmentReminder.ReminderStatus;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.repository.AppointmentReminderRepository;
import com.appointment.booking_system.repository.AppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppointmentReminderStateServiceTest {

    private AppointmentRepository appointmentRepository;
    private AppointmentReminderRepository reminderRepository;
    private AppointmentReminderStateService stateService;

    @BeforeEach
    void setUp() {
        appointmentRepository = mock(AppointmentRepository.class);
        reminderRepository = mock(AppointmentReminderRepository.class);

        stateService = new AppointmentReminderStateService(
                appointmentRepository,
                reminderRepository
        );
    }

    @Test
    void shouldRejectWhenAppointmentDoesNotExist() {
        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.empty());

        assertFalse(stateService.claimReminder(1L));

        verifyNoInteractions(reminderRepository);
    }

    @Test
    void shouldRejectCancelledAppointment() {
        Appointment appointment = mock(Appointment.class);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        assertFalse(stateService.claimReminder(1L));

        verifyNoInteractions(reminderRepository);
    }

    @Test
    void shouldRejectAppointmentMoreThan24HoursAway() {
        Appointment appointment = createAppointment(
                LocalDateTime.now().plusHours(25)
        );

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));

        assertFalse(stateService.claimReminder(1L));

        verifyNoInteractions(reminderRepository);
    }

    @Test
    void shouldClaimDueReminder() {
        Appointment appointment = createAppointment(
                LocalDateTime.now().plusHours(12)
        );

        AppointmentReminder reminder = mock(AppointmentReminder.class);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.findByAppointmentId(1L))
                .thenReturn(Optional.of(reminder));
        when(reminder.getId()).thenReturn(10L);
        when(reminderRepository.claimReminder(
                eq(10L),
                eq(ReminderStatus.PROCESSING),
                any(LocalDateTime.class),
                anyCollection(),
                eq(3)
        )).thenReturn(1);

        assertTrue(stateService.claimReminder(1L));
    }

    @Test
    void shouldRejectWhenReminderIsAlreadyClaimed() {
        Appointment appointment = createAppointment(
                LocalDateTime.now().plusHours(12)
        );

        AppointmentReminder reminder = mock(AppointmentReminder.class);

        when(appointmentRepository.findById(1L))
                .thenReturn(Optional.of(appointment));
        when(reminderRepository.findByAppointmentId(1L))
                .thenReturn(Optional.of(reminder));
        when(reminder.getId()).thenReturn(10L);
        when(reminderRepository.claimReminder(
                eq(10L),
                eq(ReminderStatus.PROCESSING),
                any(LocalDateTime.class),
                anyCollection(),
                eq(3)
        )).thenReturn(0);

        assertFalse(stateService.claimReminder(1L));
    }

    @Test
    void shouldRecoverStaleReminders() {
        when(reminderRepository.recoverStaleReminders(
                eq(ReminderStatus.PROCESSING),
                eq(ReminderStatus.PENDING),
                any(LocalDateTime.class),
                eq(3),
                anyString()
        )).thenReturn(2);

        assertEquals(2, stateService.recoverStaleReminders());
    }

    private Appointment createAppointment(LocalDateTime startTime) {
        Slot slot = mock(Slot.class);
        when(slot.getStartTime()).thenReturn(startTime);

        Appointment appointment = mock(Appointment.class);
        when(appointment.getStatus()).thenReturn(AppointmentStatus.BOOKED);
        when(appointment.getSlot()).thenReturn(slot);

        return appointment;
    }
}
