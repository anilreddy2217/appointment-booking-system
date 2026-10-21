
package com.appointment.booking_system.service;

import com.appointment.booking_system.exception.AppointmentConflictException;
import com.appointment.booking_system.exception.AppointmentForbiddenException;
import com.appointment.booking_system.exception.ResourceNotFoundException;
import com.appointment.booking_system.model.*;
import com.appointment.booking_system.repository.AppointmentRepository;
import com.appointment.booking_system.repository.SlotRepository;
import com.appointment.booking_system.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AppointmentService appointmentService;

    private User patient;
    private User providerUser;
    private Provider provider;
    private Slot slot;

    @BeforeEach
    void setUp() {
        patient = new User();
        patient.setId(1L);
        patient.setName("Test Patient");
        patient.setEmail("patient@example.com");

        providerUser = new User();
        providerUser.setId(2L);
        providerUser.setName("Test Provider");
        providerUser.setEmail("provider@example.com");

        provider = new Provider();
        provider.setId(10L);
        provider.setUser(providerUser);

        slot = new Slot();
        slot.setId(100L);
        slot.setProvider(provider);
        slot.setStartTime(LocalDateTime.of(2026, 11, 1, 10, 0));
        slot.setEndTime(LocalDateTime.of(2026, 11, 1, 10, 30));
        slot.setStatus(SlotStatus.AVAILABLE);
    }

    @Test
    void bookAppointment_whenSlotAvailable_savesAppointmentAndSendsEmail() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findBySlotId(100L)).thenReturn(Optional.empty());
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result =
                appointmentService.bookAppointment(1L, 100L, "Initial consultation");

        assertNotNull(result);
        assertSame(patient, result.getPatient());
        assertSame(slot, result.getSlot());
        assertEquals(AppointmentStatus.BOOKED, result.getStatus());
        assertEquals("Initial consultation", result.getNotes());
        assertEquals(SlotStatus.BOOKED, slot.getStatus());

        verify(slotRepository).save(slot);
        verify(appointmentRepository).save(result);
        verify(emailService).sendBookingConfirmation(
                "patient@example.com",
                "Test Patient",
                "Test Provider",
                slot.getStartTime().toString()
        );
    }

    @Test
    void bookAppointment_whenCancelledAppointmentExists_reusesIt() {
        Appointment existing = new Appointment();
        existing.setId(50L);
        existing.setStatus(AppointmentStatus.CANCELLED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));
        when(appointmentRepository.findBySlotId(100L))
                .thenReturn(Optional.of(existing));
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Appointment result =
                appointmentService.bookAppointment(1L, 100L, "Rebooked");

        assertSame(existing, result);
        assertEquals(50L, result.getId());
        assertEquals(AppointmentStatus.BOOKED, result.getStatus());
        assertEquals("Rebooked", result.getNotes());
        verify(appointmentRepository).save(existing);
    }

    @Test
    void bookAppointment_whenPatientMissing_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(1L, 100L, null));

        verifyNoInteractions(slotRepository, appointmentRepository, emailService);
    }

    @Test
    void bookAppointment_whenSlotMissing_throwsNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findByIdForUpdate(100L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.bookAppointment(1L, 100L, null));

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void bookAppointment_whenSlotUnavailable_throwsConflict() {
        slot.setStatus(SlotStatus.BOOKED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(patient));
        when(slotRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(slot));

        assertThrows(AppointmentConflictException.class,
                () -> appointmentService.bookAppointment(1L, 100L, null));

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void cancelAppointment_whenBooked_cancelsAndReopensSlot() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);
        slot.setStatus(SlotStatus.BOOKED);

        when(appointmentRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment)).thenReturn(appointment);

        Appointment result = appointmentService.cancelAppointment(50L, 1L);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());

        verify(slotRepository).save(slot);
        verify(appointmentRepository).save(appointment);
        verify(emailService).sendCancellationEmail(
                "patient@example.com",
                "Test Patient",
                slot.getStartTime().toString()
        );
    }

    @Test
    void cancelAppointment_whenAppointmentMissing_throwsNotFound() {
        when(appointmentRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> appointmentService.cancelAppointment(50L, 1L));

        verifyNoInteractions(slotRepository, emailService);
    }

    @Test
    void cancelAppointment_whenDifferentPatient_throwsForbidden() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(appointmentRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(appointment));

        assertThrows(AppointmentForbiddenException.class,
                () -> appointmentService.cancelAppointment(50L, 999L));

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(slotRepository, emailService);
    }

    @Test
    void cancelAppointment_whenNotBooked_throwsIllegalState() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(appointmentRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.cancelAppointment(50L, 1L));

        verify(appointmentRepository, never()).save(any());
        verifyNoInteractions(slotRepository, emailService);
    }


    @Test
    void getMyAppointments_returnsAppointmentsForPatient() {
        Appointment first = new Appointment();
        first.setId(1L);

        Appointment second = new Appointment();
        second.setId(2L);

        List<Appointment> expected = List.of(first, second);

        when(appointmentRepository.findByPatientId(1L))
                .thenReturn(expected);

        List<Appointment> result =
                appointmentService.getMyAppointments(1L);

        assertEquals(2, result.size());
        assertSame(expected, result);
        verify(appointmentRepository).findByPatientId(1L);
    }

    @Test
    void getProviderAppointments_returnsAppointmentsForProvider() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);

        List<Appointment> expected = List.of(appointment);

        when(appointmentRepository.findBySlotProviderId(20L))
                .thenReturn(expected);

        List<Appointment> result =
                appointmentService.getProviderAppointments(20L);

        assertEquals(1, result.size());
        assertSame(expected, result);
        verify(appointmentRepository).findBySlotProviderId(20L);
    }

    @Test
    void getAllAppointments_returnsAllAppointments() {
        Appointment first = new Appointment();
        first.setId(1L);

        Appointment second = new Appointment();
        second.setId(2L);

        List<Appointment> expected = List.of(first, second);

        when(appointmentRepository.findAll()).thenReturn(expected);

        List<Appointment> result =
                appointmentService.getAllAppointments();

        assertEquals(2, result.size());
        assertSame(expected, result);
        verify(appointmentRepository).findAll();
    }


    @Test
    void getMyAppointments_whenPatientHasNoAppointments_returnsEmptyList() {
        when(appointmentRepository.findByPatientId(1L))
                .thenReturn(List.of());

        List<Appointment> result =
                appointmentService.getMyAppointments(1L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findByPatientId(1L);
    }

    @Test
    void getProviderAppointments_whenProviderHasNoAppointments_returnsEmptyList() {
        when(appointmentRepository.findBySlotProviderId(20L))
                .thenReturn(List.of());

        List<Appointment> result =
                appointmentService.getProviderAppointments(20L);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findBySlotProviderId(20L);
    }

    @Test
    void getAllAppointments_whenNoAppointmentsExist_returnsEmptyList() {
        when(appointmentRepository.findAll()).thenReturn(List.of());

        List<Appointment> result =
                appointmentService.getAllAppointments();

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findAll();
    }



    @Test
    void cancelAppointment_whenCompleted_throwsIllegalState() {
        Appointment appointment = new Appointment();
        appointment.setId(50L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.COMPLETED);

        when(appointmentRepository.findByIdForUpdate(50L))
                .thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.cancelAppointment(50L, 1L));

        verify(appointmentRepository, never()).save(any());
        verify(slotRepository, never()).save(any());
        verifyNoInteractions(emailService);
    }

}
