
package com.appointment.booking_system.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AppointmentCancellationEmailListenerTest {

    @Test
    void handleAppointmentCancelled_sendsCancellationEmail() {
        EmailService emailService = mock(EmailService.class);
        AppointmentCancellationEmailListener listener =
                new AppointmentCancellationEmailListener(emailService);

        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                "patient@example.com",
                "Test Patient",
                "2026-11-01T10:00"
        );

        listener.handleAppointmentCancelled(event);

        verify(emailService).sendCancellationEmail(
                "patient@example.com",
                "Test Patient",
                "2026-11-01T10:00"
        );
    }
}
