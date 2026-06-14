package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByAppointmentId(Long appointmentId);
    List<Notification> findBySentFalse();
}