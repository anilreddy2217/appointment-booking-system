
package com.appointment.booking_system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AppointmentTrendResponse {

    private String period;

    private long appointmentCount;
}
