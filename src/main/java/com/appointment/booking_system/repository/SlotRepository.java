package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {
    List<Slot> findByProviderId(Long providerId);
    List<Slot> findByProviderIdAndStatus(Long providerId, SlotStatus status);
}