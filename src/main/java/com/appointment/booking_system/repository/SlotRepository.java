
package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SlotRepository extends JpaRepository<Slot, Long> {

    List<Slot> findByProviderId(Long providerId);

    List<Slot> findByProviderIdAndStatus(
            Long providerId,
            SlotStatus status
    );

    List<Slot> findByProviderIdAndStatusAndEndTimeAfter(
            Long providerId,
            SlotStatus status,
            LocalDateTime currentTime
    );

    List<Slot> findByProviderIdAndStartTimeBetween(
            Long providerId,
            LocalDateTime start,
            LocalDateTime end
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Slot s WHERE s.id = :slotId")
    Optional<Slot> findByIdForUpdate(
            @Param("slotId") Long slotId
    );

    @Query("""
        SELECT CASE WHEN COUNT(s) > 0 THEN true ELSE false END
        FROM Slot s
        WHERE s.provider.id = :providerId
          AND s.startTime < :endTime
          AND s.endTime > :startTime
        """)
    boolean existsOverlappingSlot(
            @Param("providerId") Long providerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
