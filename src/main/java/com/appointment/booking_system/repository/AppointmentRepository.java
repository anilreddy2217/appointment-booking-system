
package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Appointment;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findBySlotProviderId(Long providerId, Pageable pageable);

    Optional<Appointment> findBySlotId(Long slotId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Appointment a WHERE a.id = :appointmentId")
    Optional<Appointment> findByIdForUpdate(
            @Param("appointmentId") Long appointmentId
    );
}
