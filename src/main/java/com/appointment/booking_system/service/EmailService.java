package com.appointment.booking_system.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBookingConfirmation(String toEmail, String patientName,
                                        String providerName, String startTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Appointment Booking Confirmation");
        message.setText(
                "Dear " + patientName + ",\n\n" +
                        "Your appointment has been booked successfully!\n\n" +
                        "Provider: Dr. " + providerName + "\n" +
                        "Date & Time: " + startTime + "\n\n" +
                        "Please arrive 10 minutes before your appointment.\n\n" +
                        "Thank you,\n" +
                        "Appointment Booking System"
        );
        mailSender.send(message);
    }

    public void sendCancellationEmail(String toEmail, String patientName,
                                      String startTime) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Appointment Cancellation");
        message.setText(
                "Dear " + patientName + ",\n\n" +
                        "Your appointment scheduled at " + startTime +
                        " has been cancelled successfully.\n\n" +
                        "You can book a new appointment anytime.\n\n" +
                        "Thank you,\n" +
                        "Appointment Booking System"
        );
        mailSender.send(message);
    }
}