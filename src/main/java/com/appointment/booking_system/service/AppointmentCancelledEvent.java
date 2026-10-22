package com.appointment.booking_system.service;

public record AppointmentCancelledEvent(
        String patientEmail,
        String patientName,
        String appointmentTime
) {
}