
package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.AppointmentReminder;
import com.appointment.booking_system.model.AppointmentReminder.ReminderStatus;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.repository.AppointmentReminderRepository;
import com.appointment.booking_system.repository.AppointmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentReminderStateService {

    private static final int MAX_ATTEMPTS = 3;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderRepository reminderRepository;

    public AppointmentReminderStateService(
            AppointmentRepository appointmentRepository,
            AppointmentReminderRepository reminderRepository) {
        this.appointmentRepository = appointmentRepository;
        this.reminderRepository = reminderRepository;
    }

    @Transactional
    public boolean claimReminder(Long appointmentId) {
        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElse(null);

        if (appointment == null
                || appointment.getStatus() != AppointmentStatus.BOOKED
                || appointment.getSlot() == null
                || appointment.getSlot().getStartTime() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = appointment.getSlot().getStartTime();

        // Do not send before the 24-hour reminder becomes due.
        // Overdue reminders can still be sent if the appointment has not started.
        if (!startTime.isAfter(now)
                || startTime.minusHours(24).isAfter(now)) {
            return false;
        }

        AppointmentReminder reminder = reminderRepository
                .findByAppointmentId(appointmentId)
                .orElse(null);

        if (reminder == null) {
            reminder = new AppointmentReminder(appointment);
            reminderRepository.saveAndFlush(reminder);
        }

        return reminderRepository.claimReminder(
                reminder.getId(),
                ReminderStatus.PROCESSING,
                LocalDateTime.now(),
                List.of(
                        ReminderStatus.PENDING,
                        ReminderStatus.FAILED
                ),
                MAX_ATTEMPTS
        ) == 1;
    }

    @Transactional
    public void markSent(Long appointmentId) {
        AppointmentReminder reminder = reminderRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() ->
                        new IllegalStateException("Reminder record not found"));

        reminder.setStatus(ReminderStatus.SENT);
        reminder.setSentAt(LocalDateTime.now());
        reminder.setProcessingStartedAt(null);
        reminder.setLastError(null);
    }

    @Transactional
    public void markFailed(Long appointmentId, Exception exception) {
        AppointmentReminder reminder = reminderRepository
                .findByAppointmentId(appointmentId)
                .orElseThrow(() ->
                        new IllegalStateException("Reminder record not found"));

        reminder.setStatus(ReminderStatus.FAILED);
        reminder.setProcessingStartedAt(null);

        String error = exception.getMessage();

        if (error == null || error.isBlank()) {
            error = exception.getClass().getSimpleName();
        }

        reminder.setLastError(
                error.length() > 1000
                        ? error.substring(0, 1000)
                        : error
        );
    }

    @Transactional
    public int recoverStaleReminders() {
        return reminderRepository.recoverStaleReminders(
                ReminderStatus.PROCESSING,
                ReminderStatus.PENDING,
                LocalDateTime.now().minusMinutes(10),
                MAX_ATTEMPTS,
                "Recovered stale reminder after interrupted processing"
        );
    }
}
