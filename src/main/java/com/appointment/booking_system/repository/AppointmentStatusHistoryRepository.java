
package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.AppointmentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentStatusHistoryRepository
        extends JpaRepository<AppointmentStatusHistory, Long> {

    List<AppointmentStatusHistory>
    findByAppointmentIdOrderByChangedAtDesc(Long appointmentId);
}
