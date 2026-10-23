
package com.appointment.booking_system.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        var response = handler.handleNotFound(
                new ResourceNotFoundException("Appointment not found"));

        assertErrorResponse(response, HttpStatus.NOT_FOUND, "Appointment not found");
    }

    @Test
    void handleConflict_returns409() {
        var response = handler.handleConflict(
                new AppointmentConflictException("Slot is not available"));

        assertErrorResponse(response, HttpStatus.CONFLICT, "Slot is not available");
    }

    @Test
    void handleForbidden_returns403() {
        var response = handler.handleForbidden(
                new AppointmentForbiddenException("Access denied"));

        assertErrorResponse(response, HttpStatus.FORBIDDEN, "Access denied");
    }

    @Test
    void handleIllegalState_returns409() {
        var response = handler.handleIllegalState(
                new IllegalStateException("Invalid appointment state"));

        assertErrorResponse(
                response, HttpStatus.CONFLICT, "Invalid appointment state");
    }

    @Test
    void handleRuntimeException_hidesInternalDetails() {
        var response = handler.handleRuntimeException(
                new RuntimeException("Sensitive internal detail"));

        assertErrorResponse(
                response, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong!");
        assertNotEquals("Sensitive internal detail", response.getBody().error());
    }

    @Test
    void handleException_hidesInternalDetails() {
        var response = handler.handleException(
                new Exception("Sensitive internal detail"));

        assertErrorResponse(
                response, HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong!");
    }

    @Test
    void handleValidationException_returns400() {
        MethodArgumentNotValidException exception =
                mock(MethodArgumentNotValidException.class);

        var bindingResult =
                mock(org.springframework.validation.BindingResult.class);

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of());

        var response = handler.handleValidationException(exception);

        assertErrorResponse(
                response, HttpStatus.BAD_REQUEST, "Request validation failed");
    }

    private void assertErrorResponse(
            ResponseEntity<ApiErrorResponse> response,
            HttpStatus expectedStatus,
            String expectedMessage) {

        assertEquals(expectedStatus, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNotNull(response.getBody().timestamp());
        assertEquals(expectedStatus.value(), response.getBody().status());
        assertEquals(expectedMessage, response.getBody().error());
    }
}
