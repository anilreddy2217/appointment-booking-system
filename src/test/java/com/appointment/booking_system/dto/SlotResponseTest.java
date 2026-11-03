package com.appointment.booking_system.dto;

import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import com.appointment.booking_system.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SlotResponseTest {

    @Test
    void shouldReturnOnlyExpectedSlotFields() {

        User user = new User();
        user.setId(1L);
        user.setName("Dr. Anil");
        user.setEmail("doctor@example.com");
        user.setPassword("secretPassword123");

        Provider provider = new Provider();
        provider.setId(10L);
        provider.setUser(user);
        provider.setSpecialty("Cardiology");

        Slot slot = new Slot();
        slot.setId(100L);
        slot.setProvider(provider);
        slot.setStartTime(LocalDateTime.now().plusDays(1));
        slot.setEndTime(LocalDateTime.now().plusDays(1).plusMinutes(30));
        slot.setStatus(SlotStatus.AVAILABLE);

        SlotResponse response = SlotResponse.from(slot);

        assertEquals(100L, response.id());
        assertEquals(10L, response.providerId());
        assertEquals("Dr. Anil", response.providerName());
        assertEquals("Cardiology", response.specialty());
        assertEquals(SlotStatus.AVAILABLE, response.status());

        assertFalse(response.toString().contains("secretPassword123"));
        assertFalse(response.toString().contains("doctor@example.com"));
    }
}