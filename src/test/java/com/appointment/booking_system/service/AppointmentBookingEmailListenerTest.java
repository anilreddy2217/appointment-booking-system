package com.appointment.booking_system.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class AppointmentBookingEmailListenerTest {

    @Test
    void handleAppointmentBooked_sendsConfirmationEmail() {
        EmailService emailService = mock(EmailService.class);

        AppointmentBookingEmailListener listener =
                new AppointmentBookingEmailListener(emailService);

        AppointmentBookedEvent event = new AppointmentBookedEvent(
                "patient@example.com",
                "Test Patient",
                "Test Provider",
                "2026-11-01T10:00"
        );

        listener.handleAppointmentBooked(event);

        verify(emailService).sendBookingConfirmation(
                "patient@example.com",
                "Test Patient",
                "Test Provider",
                "2026-11-01T10:00"
        );
    }
}