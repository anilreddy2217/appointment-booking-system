package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.model.Slot;
import com.appointment.booking_system.model.SlotStatus;
import com.appointment.booking_system.repository.ProviderRepository;
import com.appointment.booking_system.repository.SlotRepository;
import com.appointment.booking_system.repository.UserRepository;
import com.appointment.booking_system.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public Slot createSlot(String email, LocalDateTime startTime, LocalDateTime endTime) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        Provider provider = providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider profile not found!"));

        Slot slot = new Slot();
        slot.setProvider(provider);
        slot.setStartTime(startTime);
        slot.setEndTime(endTime);
        slot.setStatus(SlotStatus.AVAILABLE);

        return slotRepository.save(slot);
    }

    public List<Slot> getAvailableSlots(Long providerId) {
        return slotRepository.findByProviderIdAndStatus(providerId, SlotStatus.AVAILABLE);
    }

    public List<Slot> getAllSlots(Long providerId) {
        return slotRepository.findByProviderId(providerId);
    }
}