
package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.repository.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentReminderScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(AppointmentReminderScheduler.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentReminderStateService stateService;
    private final EmailService emailService;

    public AppointmentReminderScheduler(
            AppointmentRepository appointmentRepository,
            AppointmentReminderStateService stateService,
            EmailService emailService) {
        this.appointmentRepository = appointmentRepository;
        this.stateService = stateService;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 60000)
    public void sendAppointmentReminders() {
        int recovered = stateService.recoverStaleReminders();

        if (recovered > 0) {
            log.warn("Recovered {} stale appointment reminders", recovered);
        }
        LocalDateTime now = LocalDateTime.now();

        List<Appointment> appointments =
                appointmentRepository.findUpcomingAppointmentsForReminder(
                        AppointmentStatus.BOOKED,
                        now,
                        now.plusHours(24)
                );

        for (Appointment appointment : appointments) {
            try {
                processReminder(appointment);
            } catch (DataIntegrityViolationException ex) {
                // Another scheduler run may have created the unique record.
                log.debug(
                        "Reminder already exists for appointment {}",
                        appointment.getId()
                );
            } catch (Exception ex) {
                log.error(
                        "Could not process reminder for appointment {}",
                        appointment.getId(),
                        ex
                );
            }
        }
    }

    private void processReminder(Appointment appointment) {
        Long appointmentId = appointment.getId();

        if (!stateService.claimReminder(appointmentId)) {
            return;
        }

        try {
            // Recheck status before sending to avoid reminders after cancellation.
            Appointment current = appointmentRepository.findById(appointmentId)
                    .orElse(null);

            if (current == null
                    || current.getStatus() != AppointmentStatus.BOOKED
                    || current.getSlot().getStartTime() == null
                    || !current.getSlot().getStartTime()
                    .isAfter(LocalDateTime.now())) {
                stateService.markFailed(
                        appointmentId,
                        new IllegalStateException(
                                "Appointment is no longer eligible for a reminder")
                );
                return;
            }

            emailService.sendReminderEmail(
                    current.getPatient().getEmail(),
                    current.getPatient().getName(),
                    current.getSlot().getStartTime().toString()
            );

            stateService.markSent(appointmentId);

            log.info("Reminder sent for appointment {}", appointmentId);

        } catch (Exception ex) {
            try {
                stateService.markFailed(appointmentId, ex);
            } catch (Exception stateException) {
                log.error(
                        "Could not update reminder status for appointment {}",
                        appointmentId,
                        stateException
                );
            }

            log.error(
                    "Reminder email failed for appointment {}",
                    appointmentId,
                    ex
            );
        }
    }
}
