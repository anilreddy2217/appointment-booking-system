
package com.appointment.booking_system.dto;

import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;

import java.time.LocalDateTime;

public record SlotResponse(
        Long id,
        Long providerId,
        String providerName,
        String specialty,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SlotStatus status
) {
    public static SlotResponse from(Slot slot) {
        return new SlotResponse(
                slot.getId(),
                slot.getProvider().getId(),
                slot.getProvider().getUser().getName(),
                slot.getProvider().getSpecialty(),
                slot.getStartTime(),
                slot.getEndTime(),
                slot.getStatus()
        );
    }
}
