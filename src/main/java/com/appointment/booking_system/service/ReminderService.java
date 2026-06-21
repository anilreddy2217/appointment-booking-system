package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Appointment;
import com.appointment.booking_system.model.AppointmentStatus;
import com.appointment.booking_system.repository.AppointmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ReminderService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private EmailService emailService;

    // Runs every hour automatically
    @Scheduled(fixedRate = 3600000)
    public void sendAppointmentReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next24Hours = now.plusHours(24);

        List<Appointment> appointments = appointmentRepository.findAll();

        for (Appointment appointment : appointments) {
            if (appointment.getStatus() == AppointmentStatus.BOOKED) {
                LocalDateTime appointmentTime = appointment.getSlot().getStartTime();

                if (appointmentTime.isAfter(now) && appointmentTime.isBefore(next24Hours)) {
                    emailService.sendReminderEmail(
                            appointment.getPatient().getEmail(),
                            appointment.getPatient().getName(),
                            appointmentTime.toString()
                    );
                }
            }
        }
    }
}