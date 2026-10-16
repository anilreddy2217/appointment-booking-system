
package com.appointment.booking_system.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void resourceNotFoundReturns404() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleNotFound(
                        new ResourceNotFoundException("Slot not found"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Slot not found", response.getBody().get("error"));
        assertEquals(404, response.getBody().get("status"));
    }

    @Test
    void appointmentConflictReturns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleConflict(
                        new AppointmentConflictException(
                                "Slot is not available!"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    void appointmentForbiddenReturns403() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleForbidden(
                        new AppointmentForbiddenException(
                                "You are not authorized to cancel this appointment!"));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(403, response.getBody().get("status"));
    }

    @Test
    void illegalStateReturns409() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleIllegalState(
                        new IllegalStateException(
                                "Only booked appointments can be cancelled."));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().get("status"));
    }

    @Test
    void runtimeExceptionReturns400() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleRuntimeException(
                        new RuntimeException("Invalid request"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid request", response.getBody().get("error"));
    }


    @Test
    void unexpectedExceptionReturns500WithoutExposingInternalDetails() {
        ResponseEntity<Map<String, Object>> response =
                handler.handleException(
                        new Exception("Sensitive database connection details"));

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                response.getStatusCode());

        assertEquals(500, response.getBody().get("status"));
        assertEquals(
                "Something went wrong!",
                response.getBody().get("error"));

        assertEquals(true, response.getBody().containsKey("timestamp"));

        assertEquals(
                false,
                response.getBody().get("error")
                        .toString()
                        .contains("Sensitive database connection details"));
    }

}
