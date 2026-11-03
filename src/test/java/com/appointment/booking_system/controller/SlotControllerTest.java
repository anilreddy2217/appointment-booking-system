package com.appointment.booking_system.controller;

import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.security.JwtUtil;
import com.appointment.booking_system.service.SlotService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SlotControllerTest.MockConfig.class)
class SlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SlotService slotService;

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        SlotService slotServiceMock() {
            return org.mockito.Mockito.mock(SlotService.class);
        }
    }

    @Test
    void getAllSlots_shouldReturnSlotDetailsWithoutSensitiveData()
            throws Exception {

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
        slot.setEndTime(
                LocalDateTime.now().plusDays(1).plusMinutes(30));
        slot.setStatus(SlotStatus.AVAILABLE);

        when(slotService.getAllSlots(10L))
                .thenReturn(List.of(slot));

        String token = jwtUtil.generateToken(
                "doctor@example.com", "PROVIDER", 1L);

        mockMvc.perform(get("/api/slots/all/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(100))
                .andExpect(jsonPath("$[0].providerId").value(10))
                .andExpect(jsonPath("$[0].providerName").value("Dr. Anil"))
                .andExpect(jsonPath("$[0].specialty").value("Cardiology"))
                .andExpect(jsonPath("$[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$[0].email").doesNotExist())
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }
}