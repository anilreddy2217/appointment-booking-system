
package com.appointment.booking_system.controller;

import com.appointment.booking_system.dto.AppointmentBookingRequest;
import org.springframework.http.ResponseEntity;
import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.AppointmentRepository;
import com.appointment.booking_system.repository.UserRepository;
import com.appointment.booking_system.service.AppointmentService;
import com.appointment.booking_system.service.PdfService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PdfService pdfService;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentService appointmentService;

    @InjectMocks
    private AppointmentController appointmentController;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(String email, String role) {
        var authentication = new UsernamePasswordAuthenticationToken(
                email,
                "token",
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }

    @Test
    void downloadReceipt_whenPatientOwnsAppointment_returnsPdf() {
        authenticate("patient@example.com", "PATIENT");

        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@example.com");

        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);

        byte[] expectedPdf = {1, 2, 3};

        when(userRepository.findByEmail("patient@example.com"))
                .thenReturn(Optional.of(patient));
        when(appointmentRepository.findById(50L))
                .thenReturn(Optional.of(appointment));
        when(pdfService.generateAppointmentReceipt(appointment))
                .thenReturn(expectedPdf);

        var response = appointmentController.downloadReceipt(50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(expectedPdf, response.getBody());
        verify(pdfService).generateAppointmentReceipt(appointment);
    }

    @Test
    void downloadReceipt_whenPatientDoesNotOwnAppointment_returnsNotFound() {
        authenticate("other@example.com", "PATIENT");

        User otherPatient = new User();
        otherPatient.setId(2L);
        otherPatient.setEmail("other@example.com");

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(owner);

        when(userRepository.findByEmail("other@example.com"))
                .thenReturn(Optional.of(otherPatient));
        when(appointmentRepository.findById(50L))
                .thenReturn(Optional.of(appointment));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> appointmentController.downloadReceipt(50L)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(pdfService);
    }

    @Test
    void downloadReceipt_whenAdminRequestsAppointment_returnsPdf() {
        authenticate("admin@example.com", "ADMIN");

        User admin = new User();
        admin.setId(3L);
        admin.setEmail("admin@example.com");

        User owner = new User();
        owner.setId(1L);
        owner.setEmail("owner@example.com");

        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(owner);

        byte[] expectedPdf = {4, 5, 6};

        when(userRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(appointmentRepository.findById(50L))
                .thenReturn(Optional.of(appointment));
        when(pdfService.generateAppointmentReceipt(appointment))
                .thenReturn(expectedPdf);

        var response = appointmentController.downloadReceipt(50L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertArrayEquals(expectedPdf, response.getBody());
        verify(pdfService).generateAppointmentReceipt(appointment);
    }


    @Test
    void bookAppointment_usesAuthenticatedPatientAndRequestDetails() {
        authenticate("patient@example.com", "PATIENT");

        User patient = new User();
        patient.setId(1L);
        patient.setEmail("patient@example.com");

        Appointment appointment = new Appointment();
        appointment.setId(100L);

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(25L);
        request.setNotes("Follow-up consultation");

        when(userRepository.findByEmail("patient@example.com"))
                .thenReturn(Optional.of(patient));

        when(appointmentService.bookAppointment(
                1L, 25L, "Follow-up consultation"))
                .thenReturn(appointment);

        ResponseEntity<?> response =
                appointmentController.bookAppointment(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(
                "Appointment booked successfully with ID: 100",
                response.getBody());

        verify(appointmentService).bookAppointment(
                1L, 25L, "Follow-up consultation");
    }

    @Test
    void bookAppointment_usesAuthenticatedPatientIdNotRequestIdentity() {
        authenticate("patient@example.com", "PATIENT");

        User patient = new User();
        patient.setId(7L);
        patient.setEmail("patient@example.com");

        Appointment appointment = new Appointment();
        appointment.setId(101L);

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(30L);
        request.setNotes("General consultation");

        when(userRepository.findByEmail("patient@example.com"))
                .thenReturn(Optional.of(patient));

        when(appointmentService.bookAppointment(
                7L, 30L, "General consultation"))
                .thenReturn(appointment);

        ResponseEntity<?> response =
                appointmentController.bookAppointment(request);

        assertEquals(HttpStatus.OK, response.getStatusCode());

        verify(appointmentService).bookAppointment(
                eq(7L), eq(30L), eq("General consultation"));
    }

    @Test
    void bookAppointment_whenAuthenticatedUserIsMissing_throwsException() {
        authenticate("missing@example.com", "PATIENT");

        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(25L);
        request.setNotes("Consultation");

        when(userRepository.findByEmail("missing@example.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> appointmentController.bookAppointment(request));

        assertEquals("Patient not found!", exception.getMessage());

        verify(appointmentService, never())
                .bookAppointment(any(), any(), any());
    }

}
