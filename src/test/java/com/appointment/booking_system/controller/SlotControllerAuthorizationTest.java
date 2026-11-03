package com.appointment.booking_system.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(SlotControllerAuthorizationTest.MockConfig.class)
class SlotControllerAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @TestConfiguration
    static class MockConfig {

        @Bean
        @Primary
        SlotService slotServiceMock() {
            return org.mockito.Mockito.mock(SlotService.class);
        }
    }

    @Test
    void unauthenticatedUserCannotAccessAllSlots() throws Exception {

        mockMvc.perform(get("/api/slots/all/10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void patientCannotAccessAllSlots() throws Exception {

        String token = jwtUtil.generateToken(
                "patient@example.com", "PATIENT", 1L);

        mockMvc.perform(get("/api/slots/all/10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }
}