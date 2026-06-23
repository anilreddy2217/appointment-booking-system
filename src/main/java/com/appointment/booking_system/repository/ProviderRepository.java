package com.appointment.booking_system.repository;

import com.appointment.booking_system.model.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProviderRepository extends JpaRepository<Provider, Long> {
    Optional<Provider> findByUserId(Long userId);
    List<Provider> findBySpecialtyContainingIgnoreCase(String specialty);
}