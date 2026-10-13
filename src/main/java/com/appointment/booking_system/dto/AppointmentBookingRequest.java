
package com.appointment.booking_system.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class AppointmentBookingRequest {

    @NotNull(message = "slotId is required")
    @Positive(message = "slotId must be a positive number")
    private Long slotId;

    @Size(max = 500, message = "notes must not exceed 500 characters")
    private String notes;

    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
