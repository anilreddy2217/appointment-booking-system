
package com.appointment.booking_system.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppointmentBookingRequestTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        validatorFactory.close();
    }

    @Test
    void validateRequest_whenSlotIdIsNull_shouldRejectRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();

        var violations = validator.validate(request);

        assertEquals(1, violations.stream()
                .filter(v -> v.getPropertyPath().toString().equals("slotId"))
                .filter(v -> v.getMessage().equals("slotId is required"))
                .count());
    }

    @Test
    void validateRequest_whenSlotIdIsZero_shouldRejectRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(0L);

        var violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("slotId")
                        && v.getMessage().equals(
                        "slotId must be a positive number")));
    }

    @Test
    void validateRequest_whenSlotIdIsNegative_shouldRejectRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(-1L);

        var violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("slotId")
                        && v.getMessage().equals(
                        "slotId must be a positive number")));
    }

    @Test
    void validateRequest_whenSlotIdIsPositive_shouldAcceptRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(1L);
        request.setNotes("Regular appointment");

        assertTrue(validator.validate(request).isEmpty());
    }

    @Test
    void validateRequest_whenNotesExceed500Characters_shouldRejectRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(1L);
        request.setNotes("a".repeat(501));

        var violations = validator.validate(request);

        assertTrue(violations.stream()
                .anyMatch(v -> v.getPropertyPath().toString().equals("notes")
                        && v.getMessage().equals(
                        "notes must not exceed 500 characters")));
    }

    @Test
    void validateRequest_whenNotesContain500Characters_shouldAcceptRequest() {
        AppointmentBookingRequest request = new AppointmentBookingRequest();
        request.setSlotId(1L);
        request.setNotes("a".repeat(500));

        assertTrue(validator.validate(request).isEmpty());
    }
}
