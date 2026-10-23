package com.appointment.booking_system.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class AppointmentBookingEmailListener {

    private final EmailService emailService;

    public AppointmentBookingEmailListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleAppointmentBooked(AppointmentBookedEvent event) {
        emailService.sendBookingConfirmation(
                event.patientEmail(),
                event.patientName(),
                event.providerName(),
                event.appointmentTime()
        );
    }
}