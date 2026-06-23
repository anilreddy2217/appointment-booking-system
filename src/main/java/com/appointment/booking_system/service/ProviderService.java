package com.appointment.booking_system.service;

import java.util.List;
import com.appointment.booking_system.model.Provider;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.ProviderRepository;
import com.appointment.booking_system.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ProviderService {

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private UserRepository userRepository;

    public Provider createProfile(String email, String specialty,
                                  String bio, String location, String phone) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (providerRepository.findByUserId(user.getId()).isPresent()) {
            throw new RuntimeException("Provider profile already exists!");
        }

        Provider provider = new Provider();
        provider.setUser(user);
        provider.setSpecialty(specialty);
        provider.setBio(bio);
        provider.setLocation(location);
        provider.setPhone(phone);

        return providerRepository.save(provider);
    }

    public Provider getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));
        return providerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Provider profile not found!"));
    }
    public List<Provider> searchBySpecialty(String specialty) {
        return providerRepository.findBySpecialtyContainingIgnoreCase(specialty);
    }

    public List<Provider> getAllProviders() {
        return providerRepository.findAll();
    }
}