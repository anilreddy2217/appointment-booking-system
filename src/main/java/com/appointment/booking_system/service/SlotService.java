
package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.ProviderRepository;
import com.appointment.booking_system.repository.SlotRepository;
import com.appointment.booking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SlotService {

    @Autowired
    private SlotRepository slotRepository;

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Slot createSlot(
            String email,
            LocalDateTime startTime,
            LocalDateTime endTime) {

        // Validate the time range.
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException(
                    "Start time and end time are required."
            );
        }

        if (!startTime.isBefore(endTime)) {
            throw new IllegalArgumentException(
                    "Start time must be before end time."
            );
        }

        // Find the user.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found!"));

        // Find the provider profile.
        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() ->
                        new RuntimeException("Provider profile not found!"));

        // Reject overlapping slots for this provider.
        boolean overlapping = slotRepository.existsOverlappingSlot(
                provider.getId(),
                startTime,
                endTime
        );

        if (overlapping) {
            throw new IllegalArgumentException(
                    "This slot overlaps with an existing slot."
            );
        }

        // Create the slot.
        Slot slot = new Slot();
        slot.setProvider(provider);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setStatus(SlotStatus.AVAILABLE);

        return slotRepository.save(slot);
    }

    public List<Slot> getAvailableSlots(Long providerId) {
        return slotRepository.findByProviderIdAndStatusAndEndTimeAfter(
                providerId,
                SlotStatus.AVAILABLE,
                LocalDateTime.now()
        );
    }

    public List<Slot> getAllSlots(Long providerId) {
        return slotRepository.findByProviderId(providerId);
    }

    public List<Slot> getSlotsByDate(
            Long providerId,
            LocalDateTime date) {

        LocalDateTime startOfDay =
                date.toLocalDate().atStartOfDay();

        LocalDateTime endOfDay = startOfDay.plusDays(1);

        return slotRepository.findByProviderIdAndStartTimeBetween(
                providerId,
                startOfDay,
                endOfDay
        );
    }
}
