
package com.appointment.booking_system.service;

import com.appointment.booking_system.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.appointment.booking_system.service.AppointmentBookedEvent;
import org.mockito.ArgumentCaptor;
import com.appointment.booking_system.service.AppointmentCancelledEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import com.appointment.booking_system.exception.AppointmentConflictException;
import com.appointment.booking_system.exception.AppointmentForbiddenException;
import com.appointment.booking_system.exception.ResourceNotFoundException;
import com.appointment.booking_system.model.*;
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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private SlotRepository slotRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AppointmentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private ProviderRepository providerRepository;

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
    void bookAppointment_whenSlotAvailable_savesAppointmentAndPublishesEvent() {
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

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertInstanceOf(AppointmentBookedEvent.class, eventCaptor.getValue());

        AppointmentBookedEvent event =
                (AppointmentBookedEvent) eventCaptor.getValue();

        assertEquals("patient@example.com", event.patientEmail());
        assertEquals("Test Patient", event.patientName());
        assertEquals("Test Provider", event.providerName());
        assertEquals(slot.getStartTime().toString(), event.appointmentTime());

        verifyNoInteractions(emailService);
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
        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);

        Appointment result = appointmentService.cancelAppointment(50L, 1L);

        assertEquals(AppointmentStatus.CANCELLED, result.getStatus());
        assertEquals(SlotStatus.AVAILABLE, slot.getStatus());

        verify(slotRepository).save(slot);
        verify(appointmentRepository).save(appointment);

        ArgumentCaptor<Object> eventCaptor =
                ArgumentCaptor.forClass(Object.class);

        verify(eventPublisher).publishEvent(eventCaptor.capture());

        assertInstanceOf(
                AppointmentCancelledEvent.class,
                eventCaptor.getValue()
        );

        AppointmentCancelledEvent event =
                (AppointmentCancelledEvent) eventCaptor.getValue();

        assertEquals("patient@example.com", event.patientEmail());
        assertEquals("Test Patient", event.patientName());
        assertEquals(
                slot.getStartTime().toString(),
                event.appointmentTime()
        );

        verifyNoInteractions(emailService);
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

        Pageable pageable = PageRequest.of(0, 10);

        Page<Appointment> expected =
                new PageImpl<>(List.of(first, second), pageable, 2);

        when(appointmentRepository.findByPatientId(1L, pageable))
                .thenReturn(expected);

        Page<Appointment> result =
                appointmentService.getMyAppointments(1L, pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(appointmentRepository).findByPatientId(1L, pageable);
    }

    @Test
    void getProviderAppointments_returnsAppointmentsForProvider() {
        Appointment appointment = new Appointment();
        appointment.setId(10L);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Appointment> expected =
                new PageImpl<>(List.of(appointment), pageable, 1);

        when(appointmentRepository.findBySlotProviderId(20L, pageable))
                .thenReturn(expected);

        Page<Appointment> result =
                appointmentService.getProviderAppointments(20L, pageable);

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().get(0).getId());

        verify(appointmentRepository)
                .findBySlotProviderId(20L, pageable);
    }

    @Test
    void getAllAppointments_returnsAllAppointments() {
        Appointment first = new Appointment();
        first.setId(1L);

        Appointment second = new Appointment();
        second.setId(2L);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Appointment> expected =
                new PageImpl<>(List.of(first, second), pageable, 2);

        when(appointmentRepository.findAll(pageable))
                .thenReturn(expected);

        Page<Appointment> result =
                appointmentService.getAllAppointments(pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(appointmentRepository).findAll(pageable);
    }

    @Test
    void getProviderAppointments_whenProviderHasNoAppointments_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(appointmentRepository.findBySlotProviderId(20L, pageable))
                .thenReturn(Page.empty(pageable));

        Page<Appointment> result =
                appointmentService.getProviderAppointments(20L, pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository)
                .findBySlotProviderId(20L, pageable);
    }

    @Test
    void getAllAppointments_whenNoAppointmentsExist_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);

        when(appointmentRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        Page<Appointment> result =
                appointmentService.getAllAppointments(pageable);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(appointmentRepository).findAll(pageable);
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


    @Test
    void updateAppointmentStatus_whenBookedAndEnded_marksMissedAndSavesHistory() {
        providerUser.setRole(Role.PROVIDER);

        LocalDateTime pastEndTime = LocalDateTime.now().minusMinutes(30);
        slot.setEndTime(pastEndTime);

        Appointment appointment = new Appointment();
        appointment.setId(36L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(userRepository.findByEmail("provider@example.com"))
                .thenReturn(Optional.of(providerUser));
        when(providerRepository.findByUserId(2L))
                .thenReturn(Optional.of(provider));
        when(appointmentRepository.findByIdForUpdate(36L))
                .thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(appointment))
                .thenReturn(appointment);

        Appointment result = appointmentService.updateAppointmentStatus(
                36L, "provider@example.com", AppointmentStatus.MISSED);

        assertEquals(AppointmentStatus.MISSED, result.getStatus());

        ArgumentCaptor<AppointmentStatusHistory> historyCaptor =
                ArgumentCaptor.forClass(AppointmentStatusHistory.class);

        verify(statusHistoryRepository).save(historyCaptor.capture());

        AppointmentStatusHistory history = historyCaptor.getValue();
        assertSame(appointment, history.getAppointment());
        assertEquals(AppointmentStatus.BOOKED, history.getPreviousStatus());
        assertEquals(AppointmentStatus.MISSED, history.getNewStatus());
        assertSame(providerUser, history.getChangedBy());
    }

    @Test
    void updateAppointmentStatus_whenCancelled_throwsIllegalState() {
        providerUser.setRole(Role.PROVIDER);

        slot.setEndTime(LocalDateTime.now().minusMinutes(30));

        Appointment appointment = new Appointment();
        appointment.setId(36L);
        appointment.setPatient(patient);
        appointment.setSlot(slot);
        appointment.setStatus(AppointmentStatus.CANCELLED);

        when(userRepository.findByEmail("provider@example.com"))
                .thenReturn(Optional.of(providerUser));
        when(providerRepository.findByUserId(2L))
                .thenReturn(Optional.of(provider));
        when(appointmentRepository.findByIdForUpdate(36L))
                .thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.updateAppointmentStatus(
                        36L, "provider@example.com", AppointmentStatus.MISSED));

        verify(appointmentRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }

    @Test
    void updateAppointmentStatus_whenAnotherProviderOwnsAppointment_throwsForbidden() {
        providerUser.setRole(Role.PROVIDER);

        User anotherProviderUser = new User();
        anotherProviderUser.setId(3L);
        anotherProviderUser.setEmail("another-provider@example.com");
        anotherProviderUser.setRole(Role.PROVIDER);

        Provider anotherProvider = new Provider();
        anotherProvider.setId(20L);
        anotherProvider.setUser(anotherProviderUser);

        Slot anotherSlot = new Slot();
        anotherSlot.setId(200L);
        anotherSlot.setProvider(anotherProvider);
        anotherSlot.setEndTime(LocalDateTime.now().minusMinutes(30));

        Appointment appointment = new Appointment();
        appointment.setId(36L);
        appointment.setPatient(patient);
        appointment.setSlot(anotherSlot);
        appointment.setStatus(AppointmentStatus.BOOKED);

        when(userRepository.findByEmail("provider@example.com"))
                .thenReturn(Optional.of(providerUser));
        when(providerRepository.findByUserId(2L))
                .thenReturn(Optional.of(provider));
        when(appointmentRepository.findByIdForUpdate(36L))
                .thenReturn(Optional.of(appointment));

        assertThrows(AppointmentForbiddenException.class,
                () -> appointmentService.updateAppointmentStatus(
                        36L, "provider@example.com", AppointmentStatus.MISSED));

        verify(appointmentRepository, never()).save(any());
        verify(statusHistoryRepository, never()).save(any());
    }


}
