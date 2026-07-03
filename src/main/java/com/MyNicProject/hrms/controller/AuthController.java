package com.MyNicProject.hrms.controller;

import com.MyNicProject.hrms.dto.*;
import com.MyNicProject.hrms.entity.Role;
import com.MyNicProject.hrms.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


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

    /**
     * Public self-registration. Creates a PENDING account with no role —
     * cannot log in until an admin approves it via /approve.
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody SelfRegisterRequest request) {
        boolean created = authService.registerEmployee(
                request.employeeId(), request.employeeName(), request.department(), request.password());

        if (!created) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("An account with employee ID " + request.employeeId() + " already exists");
        }

        return ResponseEntity.ok("Registration submitted. An administrator will review your account.");
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PendingEmployeeResponse>> listPending() {
        return ResponseEntity.ok(authService.listPending());
    }

    @PostMapping("/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> approve(@Valid @RequestBody ApprovalRequest request) {
        if (request.role() == null || request.role().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("A role (USER or ADMIN) is required to approve an account");
        }

        boolean approved = authService.approveEmployee(request.employeeId(), Role.valueOf(request.role()));

        if (!approved) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No pending account found for employee ID " + request.employeeId());
        }

        return ResponseEntity.ok("Approved " + request.employeeId() + " as " + request.role());
    }

    @PostMapping("/reject/{employeeId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> reject(@PathVariable String employeeId) {
        boolean rejected = authService.rejectEmployee(employeeId);

        if (!rejected) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("No pending account found for employee ID " + employeeId);
        }

        return ResponseEntity.ok("Rejected " + employeeId);
    }

    /**
     * Existing admin-direct-provision shortcut, kept for bootstrapping and
     * for creating known-staff accounts without going through self-registration.
     */
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