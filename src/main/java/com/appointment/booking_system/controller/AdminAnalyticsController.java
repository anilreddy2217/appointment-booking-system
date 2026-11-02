
package com.appointment.booking_system.controller;

import com.appointment.booking_system.dto.AdminAnalyticsSummary;
import com.appointment.booking_system.service.AdminAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.appointment.booking_system.dto.AppointmentTrendResponse;
import java.util.List;

@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    public AdminAnalyticsController(
            AdminAnalyticsService adminAnalyticsService) {
        this.adminAnalyticsService = adminAnalyticsService;
    }

    @GetMapping("/summary")
    public AdminAnalyticsSummary getAnalyticsSummary() {
        return adminAnalyticsService.getAnalyticsSummary();
    }

    @GetMapping("/trends")
    public List<AppointmentTrendResponse> getMonthlyAppointmentTrends() {
        return adminAnalyticsService.getMonthlyAppointmentTrends();
    }
}
