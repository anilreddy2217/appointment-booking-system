
package com.appointment.booking_system.dto;

import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.model.SlotStatus;

import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        LocalDateTime createdAt,
        String notes,
        AppointmentStatus status,
        Long patientId,
        String patientName,
        Long providerId,
        String providerName,
        Long slotId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SlotStatus slotStatus
) {

    public static AppointmentResponse from(Appointment appointment) {
        var patient = appointment.getPatient();
        var slot = appointment.getSlot();
        var provider = slot.getProvider();

        return new AppointmentResponse(
                appointment.getId(),
                appointment.getCreatedAt(),
                appointment.getNotes(),
                appointment.getStatus(),
                patient.getId(),
                patient.getName(),
                provider.getId(),
                provider.getUser().getName(),
                slot.getId(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
