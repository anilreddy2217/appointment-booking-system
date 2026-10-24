package com.appointment.booking_system.service;

import com.appointment.booking_system.dto.PatientProfileDTO;
import java.util.Locale;
import com.appointment.booking_system.model.Role;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.UserRepository;
import com.appointment.booking_system.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(String name, String email, String password, Role role) {
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already registered!");
        }
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        return userRepository.save(user);
    }
    public String loginUser(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found!"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password!");
        }

        return jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getId());
    }

    public PatientProfileDTO getPatientProfile(String authenticatedEmail) {

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PATIENT) {
            throw new RuntimeException("Only patients can access this profile");
        }

        return mapToPatientProfileDTO(user);
    }

    public PatientProfileDTO updatePatientProfile(
            String authenticatedEmail,
            PatientProfileDTO profileDTO) {

        User user = userRepository.findByEmail(authenticatedEmail)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PATIENT) {
            throw new RuntimeException("Only patients can update this profile");
        }

        if (profileDTO.getName() == null || profileDTO.getName().isBlank()) {
            throw new RuntimeException("Name cannot be empty");
        }

        if (profileDTO.getEmail() == null || profileDTO.getEmail().isBlank()) {
            throw new RuntimeException("Email cannot be empty");
        }

        String updatedEmail = profileDTO.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        userRepository.findByEmail(updatedEmail)
                .filter(existingUser -> !existingUser.getId().equals(user.getId()))
                .ifPresent(existingUser -> {
                    throw new RuntimeException("Email is already registered");
                });

        user.setName(profileDTO.getName().trim());
        user.setEmail(updatedEmail);
        user.setMobileNumber(profileDTO.getMobileNumber());
        user.setDateOfBirth(profileDTO.getDateOfBirth());
        user.setGender(profileDTO.getGender());

        User savedUser = userRepository.save(user);

        return mapToPatientProfileDTO(savedUser);
    }

    private PatientProfileDTO mapToPatientProfileDTO(User user) {

        PatientProfileDTO dto = new PatientProfileDTO();

        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setMobileNumber(user.getMobileNumber());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());

        return dto;
    }

    public String generatePatientToken(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getRole() != Role.PATIENT) {
            throw new RuntimeException("Only patients can use this operation");
        }

        return jwtUtil.generateToken(
                user.getEmail(),
                user.getRole().name(),
                user.getId()
        );
    }
}