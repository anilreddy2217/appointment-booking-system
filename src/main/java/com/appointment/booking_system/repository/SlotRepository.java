package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByProviderId(Long providerId);

    List<Slot> findByProviderIdAndStatus(Long providerId, SlotStatus status);

    List<Slot> findByProviderIdAndStartTimeBetween(Long providerId,
                                                   LocalDateTime start, LocalDateTime end);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :slotId")
    Optional<Slot> findByIdForUpdate(@Param("slotId") Long slotId);
}