
package com.appointment.booking_system.dto;

import com.appointment.booking_system.model.AppointmentStatus;
import jakarta.validation.constraints.NotNull;

public record AppointmentStatusUpdateRequest(
        @NotNull(message = "Appointment status is required")
        AppointmentStatus status
) {
}
