package com.MyNicProject.hrms.controller;

import com.MyNicProject.hrms.dto.JwtResponse;
import com.MyNicProject.hrms.dto.LoginRequest;
import com.MyNicProject.hrms.entity.Role;
import com.MyNicProject.hrms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.MyNicProject.hrms.dto.ProvisionLoginRequest;


@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        JwtResponse response = authService.login(request);


        if (response == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid employee ID or password");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/provision")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> provisionLogin(@Valid @RequestBody ProvisionLoginRequest request) {
        Role role = (request.role() == null || request.role().isBlank())
                ? Role.USER
                : Role.valueOf(request.role());

        authService.provisionLogin(
                request.employeeId(), request.employeeName(), request.password(), role);

        return ResponseEntity.ok("Login provisioned for " + request.employeeId());
    }
}