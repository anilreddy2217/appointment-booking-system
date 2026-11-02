
package com.appointment.booking_system.service;

import com.appointment.booking_system.dto.AdminAnalyticsSummary;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.model.Role;
import com.appointment.booking_system.model.SlotStatus;
import com.appointment.booking_system.repository.AnalyticsRepository;
import org.springframework.stereotype.Service;
import com.appointment.booking_system.dto.AppointmentTrendResponse;
import java.util.List;
import java.util.ArrayList;

import java.time.LocalDateTime;

@Service
public class AdminAnalyticsService {

    private final AnalyticsRepository analyticsRepository;

    public AdminAnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public AdminAnalyticsSummary getAnalyticsSummary() {

        long totalAppointments =
                analyticsRepository.countTotalAppointments();

        long registeredPatients =
                analyticsRepository.countUsersByRole(Role.PATIENT);

        long totalProviders =
                analyticsRepository.countTotalProviders();

        long activeProviders =
                analyticsRepository.countActiveProviders(
                        SlotStatus.AVAILABLE,
                        LocalDateTime.now()
                );

        long cancelledAppointments =
                analyticsRepository.countAppointmentsByStatus(
                        AppointmentStatus.CANCELLED
                );

        long completedAppointments =
                analyticsRepository.countAppointmentsByStatus(
                        AppointmentStatus.COMPLETED
                );

        long bookedAppointments =
                analyticsRepository.countAppointmentsByStatus(
                        AppointmentStatus.BOOKED
                );

        long missedAppointments =
                analyticsRepository.countAppointmentsByStatus(
                        AppointmentStatus.MISSED
                );

        return new AdminAnalyticsSummary(
                totalAppointments,
                registeredPatients,
                totalProviders,
                activeProviders,
                cancelledAppointments,
                completedAppointments,
                bookedAppointments,
                missedAppointments
        );
    }

    public List<AppointmentTrendResponse> getMonthlyAppointmentTrends() {

        List<Object[]> results =
                analyticsRepository.getMonthlyAppointmentTrends();

        List<AppointmentTrendResponse> trends = new ArrayList<>();

        for (Object[] row : results) {

            String period = String.valueOf(row[0]);

            long appointmentCount = ((Number) row[1]).longValue();

            trends.add(
                    new AppointmentTrendResponse(
                            period,
                            appointmentCount
                    )
            );
        }

        return trends;
    }
}
