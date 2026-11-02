
package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.model.Role;
import com.appointment.booking_system.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

import com.appointment.booking_system.model.User;
import java.time.LocalDateTime;

@Repository
public interface AnalyticsRepository extends JpaRepository<User, Long> {

    @Query("""
        SELECT COUNT(a)
        FROM Appointment a
        """)
    long countTotalAppointments();

    @Query("""
        SELECT COUNT(u)
        FROM User u
        WHERE u.role = :role
        """)
    long countUsersByRole(@Param("role") Role role);

    @Query("""
        SELECT COUNT(p)
        FROM Provider p
        """)
    long countTotalProviders();

    @Query("""
        SELECT COUNT(DISTINCT s.provider.id)
        FROM Slot s
        WHERE s.status = :status
          AND s.startTime > :now
        """)
    long countActiveProviders(
            @Param("status") SlotStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
        SELECT COUNT(a)
        FROM Appointment a
        WHERE a.status = :status
        """)
    long countAppointmentsByStatus(
            @Param("status") AppointmentStatus status
    );

    @Query(value = """
    SELECT
        DATE_FORMAT(created_at, '%Y-%m') AS period,
        COUNT(*) AS appointmentCount
    FROM appointments
    GROUP BY DATE_FORMAT(created_at, '%Y-%m')
    ORDER BY period ASC
    """, nativeQuery = true)
    List<Object[]> getMonthlyAppointmentTrends();
}
