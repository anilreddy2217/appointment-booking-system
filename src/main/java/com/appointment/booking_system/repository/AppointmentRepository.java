package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByPatientId(Long patientId);
    List<Appointment> findBySlotProviderId(Long providerId);
    Optional<Appointment> findBySlotId(Long slotId);
}