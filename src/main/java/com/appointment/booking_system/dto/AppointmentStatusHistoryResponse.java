
package com.appointment.booking_system.dto;

import com.appointment.booking_system.model.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentStatusHistoryResponse(
        Long id,
        AppointmentStatus previousStatus,
        AppointmentStatus newStatus,
        Long changedByUserId,
        LocalDateTime changedAt
) {
}
