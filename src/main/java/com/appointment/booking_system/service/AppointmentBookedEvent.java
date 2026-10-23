package com.appointment.booking_system.service;

public record AppointmentBookedEvent(
        String patientEmail,
        String patientName,
        String providerName,
        String appointmentTime
) {
}