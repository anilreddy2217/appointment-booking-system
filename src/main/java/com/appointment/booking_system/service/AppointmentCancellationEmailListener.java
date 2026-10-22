package com.appointment.booking_system.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AppointmentCancellationEmailListener {

    private final EmailService emailService;

    public AppointmentCancellationEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentCancelled(AppointmentCancelledEvent event) {
        emailService.sendCancellationEmail(
                event.patientEmail(),
                event.patientName(),
                event.appointmentTime()
        );
    }
}