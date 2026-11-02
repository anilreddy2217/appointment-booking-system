
package com.appointment.booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminAnalyticsSummary {

    private long totalAppointments;

    private long registeredPatients;

    private long totalProviders;

    private long activeProviders;

    private long cancelledAppointments;

    private long completedAppointments;

    private long bookedAppointments;

    private long missedAppointments;
}
