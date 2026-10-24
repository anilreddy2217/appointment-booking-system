package com.appointment.booking_system.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientProfileDTO {

    private String name;

    private String email;

    private String mobileNumber;

    private LocalDate dateOfBirth;

    private String gender;
}