package com.MyNicProject.hrms.service;

import com.MyNicProject.hrms.dto.JwtResponse;
import com.MyNicProject.hrms.dto.LoginRequest;
import com.MyNicProject.hrms.entity.Employee;
import com.MyNicProject.hrms.entity.Role;
import com.MyNicProject.hrms.repository.EmployeeRepository;
import com.MyNicProject.hrms.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }


    public JwtResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.employeeId(), request.password()));
        } catch (Exception e) {

            return null;
        }

        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(request.employeeId());
        if (employeeOpt.isEmpty()) {
            return null;
        }

        Employee employee = employeeOpt.get();
        String token = jwtUtil.generateToken(employee.getEmployeeId(), employee.getRole().name());
        return new JwtResponse(token, employee.getEmployeeId(), employee.getRole().name());
    }


    @Transactional
    public boolean provisionLogin(String employeeId, String rawPassword, Role role) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);

        if (employeeOpt.isEmpty()) {
            return false;
        }

        Employee employee = employeeOpt.get();
        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        employee.setRole(role);
        employee.setCanLogin(true);
        employeeRepository.save(employee);
        return true;
    }
}