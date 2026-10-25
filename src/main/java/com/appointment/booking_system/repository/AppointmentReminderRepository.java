
package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.AppointmentReminder;
import com.appointment.booking_system.model.AppointmentReminder.ReminderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import java.util.Collection;
import java.util.List;

@Repository
public interface AppointmentReminderRepository
        extends JpaRepository<AppointmentReminder, Long> {

    boolean existsByAppointmentId(Long appointmentId);

    @Modifying
    @Query("""
    UPDATE AppointmentReminder r
    SET r.status = :processing,
        r.processingStartedAt = :processingStartedAt,
        r.attempts = r.attempts + 1,
        r.lastError = null
    WHERE r.id = :id
      AND r.status IN :claimableStatuses
      AND r.attempts < :maxAttempts
    """)
    int claimReminder(
            @Param("id") Long id,
            @Param("processing") ReminderStatus processing,
            @Param("processingStartedAt") LocalDateTime processingStartedAt,
            @Param("claimableStatuses") Collection<ReminderStatus> claimableStatuses,
            @Param("maxAttempts") int maxAttempts
    );

    @Query("""
        SELECT r FROM AppointmentReminder r
        WHERE r.status IN :statuses
          AND r.attempts < :maxAttempts
        """)
    List<AppointmentReminder> findRetryableReminders(
            @Param("statuses") Collection<ReminderStatus> statuses,
            @Param("maxAttempts") int maxAttempts
    );
    Optional<AppointmentReminder> findByAppointmentId(Long appointmentId);

    @Modifying
    @Query("""
    UPDATE AppointmentReminder r
    SET r.status = :pending,
        r.processingStartedAt = null,
        r.lastError = :error
    WHERE r.status = :processing
      AND r.processingStartedAt < :cutoff
      AND r.attempts < :maxAttempts
    """)
    int recoverStaleReminders(
            @Param("processing") ReminderStatus processing,
            @Param("pending") ReminderStatus pending,
            @Param("cutoff") LocalDateTime cutoff,
            @Param("maxAttempts") int maxAttempts,
            @Param("error") String error
    );
}
