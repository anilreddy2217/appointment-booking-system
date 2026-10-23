package com.appointment.booking_system;

import com.appointment.booking_system.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.appointment.booking_system.model.Role;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.repository.UserRepository;
import com.appointment.booking_system.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingSystemApplicationTests {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private PasswordEncoder passwordEncoder;


	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private UserService userService;

	@Test
	void testRegisterUser_Success() {
		when(userRepository.existsByEmail("test@gmail.com")).thenReturn(false);
		when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArguments()[0]);

		User user = userService.registerUser(
				"Test User",
				"test@gmail.com",
				"password123",
				Role.PATIENT
		);

		assertNotNull(user);
		assertEquals("Test User", user.getName());
		assertEquals("test@gmail.com", user.getEmail());
		assertEquals(Role.PATIENT, user.getRole());
		verify(userRepository, times(1)).save(any(User.class));
	}

	@Test
	void testRegisterUser_EmailAlreadyExists() {
		when(userRepository.existsByEmail("test@gmail.com")).thenReturn(true);

		RuntimeException exception = assertThrows(RuntimeException.class, () ->
				userService.registerUser(
						"Test User",
						"test@gmail.com",
						"password123",
						Role.PATIENT
				)
		);

		assertEquals("Email already registered!", exception.getMessage());
		verify(userRepository, never()).save(any(User.class));
	}

    @Test
    void testLoginUser_Success() {
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@gmail.com");
        mockUser.setRole(Role.PATIENT);
        mockUser.setPassword("$2a$10$encodedpassword");

        when(userRepository.findByEmail("test@gmail.com"))
                .thenReturn(Optional.of(mockUser));

        when(passwordEncoder.matches(
                "password123", "$2a$10$encodedpassword"))
                .thenReturn(true);

        when(jwtUtil.generateToken(
                "test@gmail.com", "PATIENT", 1L))
                .thenReturn("mock-jwt-token");

        String token = userService.loginUser(
                "test@gmail.com", "password123");

        assertNotNull(token);
        assertEquals("mock-jwt-token", token);

        verify(userRepository).findByEmail("test@gmail.com");
        verify(passwordEncoder).matches(
                "password123", "$2a$10$encodedpassword");
    }

	@Test
	void testLoginUser_UserNotFound() {
		when(userRepository.findByEmail("notexist@gmail.com"))
				.thenReturn(Optional.empty());

		RuntimeException exception = assertThrows(RuntimeException.class, () ->
				userService.loginUser("notexist@gmail.com", "password123")
		);

		assertEquals("User not found!", exception.getMessage());
	}
}