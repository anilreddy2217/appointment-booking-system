package com.appointment.booking_system.controller;

import com.appointment.booking_system.model.Role;
import com.appointment.booking_system.model.User;
import com.appointment.booking_system.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        String name = request.get("name");
        String email = request.get("email");
        String password = request.get("password");
        String role = request.get("role");

        User user = userService.registerUser(name, email, password, Role.valueOf(role));
        return ResponseEntity.ok("User registered successfully: " + user.getName());
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String password = request.get("password");

        String token = userService.loginUser(email, password);
        return ResponseEntity.ok(Map.of("token", token));
    }
}