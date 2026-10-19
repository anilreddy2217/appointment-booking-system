
package com.appointment.booking_system.security;

import com.appointment.booking_system.service.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AppointmentAuthorizationTest.MockConfig.class)
class AppointmentAuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AppointmentService appointmentService;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        AppointmentService appointmentServiceMock() {
            return mock(AppointmentService.class);
        }
    }

    @Test
    void patientCannotAccessAllAppointments() throws Exception {
        String token = jwtUtil.generateToken(
                "patient@example.com", "PATIENT", 1L);

        mockMvc.perform(get("/api/appointments/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void providerCannotAccessAllAppointments() throws Exception {
        String token = jwtUtil.generateToken(
                "provider@example.com", "PROVIDER", 2L);

        mockMvc.perform(get("/api/appointments/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAccessAllAppointments() throws Exception {
        when(appointmentService.getAllAppointments()).thenReturn(List.of());

        String token = jwtUtil.generateToken(
                "admin@example.com", "ADMIN", 3L);

        mockMvc.perform(get("/api/appointments/all")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
