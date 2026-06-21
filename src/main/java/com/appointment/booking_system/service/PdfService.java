package com.appointment.booking_system.service;

import com.appointment.booking_system.model.Appointment;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfService {

    public byte[] generateAppointmentReceipt(Appointment appointment) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Document document = new Document();
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
            Paragraph title = new Paragraph("Appointment Receipt", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));

            // Patient details
            Font headingFont = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Font normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);

            document.add(new Paragraph("Patient Details", headingFont));
            document.add(new Paragraph("Name: " + appointment.getPatient().getName(), normalFont));
            document.add(new Paragraph("Email: " + appointment.getPatient().getEmail(), normalFont));

            document.add(new Paragraph(" "));

            // Provider details
            document.add(new Paragraph("Provider Details", headingFont));
            document.add(new Paragraph("Name: Dr. " + appointment.getSlot().getProvider().getUser().getName(), normalFont));
            document.add(new Paragraph("Specialty: " + appointment.getSlot().getProvider().getSpecialty(), normalFont));
            document.add(new Paragraph("Location: " + appointment.getSlot().getProvider().getLocation(), normalFont));

            document.add(new Paragraph(" "));

            // Appointment details
            document.add(new Paragraph("Appointment Details", headingFont));
            document.add(new Paragraph("Appointment ID: " + appointment.getId(), normalFont));
            document.add(new Paragraph("Date & Time: " + appointment.getSlot().getStartTime(), normalFont));
            document.add(new Paragraph("Status: " + appointment.getStatus(), normalFont));
            document.add(new Paragraph("Notes: " + appointment.getNotes(), normalFont));

            document.add(new Paragraph(" "));

            // Footer
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph footer = new Paragraph("Thank you for using Appointment Booking System", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();
            return outputStream.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage());
        }
    }
}