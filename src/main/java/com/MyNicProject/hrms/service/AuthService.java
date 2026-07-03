package com.MyNicProject.hrms.service;

import com.MyNicProject.hrms.dto.JwtResponse;
import com.MyNicProject.hrms.dto.LoginRequest;
import com.MyNicProject.hrms.dto.PendingEmployeeResponse;
import com.MyNicProject.hrms.entity.AccountStatus;
import com.MyNicProject.hrms.entity.Department;
import com.MyNicProject.hrms.entity.Employee;
import com.MyNicProject.hrms.entity.Role;
import com.MyNicProject.hrms.repository.DepartmentRepository;
import com.MyNicProject.hrms.repository.EmployeeRepository;
import com.MyNicProject.hrms.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    public AuthService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtUtil jwtUtil) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtUtil = jwtUtil;
    }


    public JwtResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.employeeId(), request.password()));
        } catch (org.springframework.security.core.AuthenticationException e) {
            // Covers bad credentials AND accounts that are PENDING/REJECTED
            // (CustomUserDetailsService throws UsernameNotFoundException for those,
            // which Spring's DaoAuthenticationProvider surfaces as an AuthenticationException here).
            return null;
        }
        // Any other exception (e.g. DB connectivity) is allowed to propagate
        // so it surfaces as a 500 instead of being misreported as bad credentials.

        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(request.employeeId());
        if (employeeOpt.isEmpty()) {
            return null;
        }

        Employee employee = employeeOpt.get();
        String token = jwtUtil.generateToken(employee.getEmployeeId(), employee.getRole().name());
        return new JwtResponse(token, employee.getEmployeeId(), employee.getEmployeeName(), employee.getRole().name());
    }

    /**
     * Self-service registration. Creates the account in PENDING status with
     * no role — it cannot log in until an admin approves it via approve().
     * Rejects if the employeeId already exists in any state, so a pending/rejected/
     * approved account can't be silently overwritten by a new registration attempt.
     */
    @Transactional
    public boolean registerEmployee(String employeeId, String employeeName, String departmentName, String rawPassword) {
        if (employeeRepository.findByEmployeeId(employeeId).isPresent()) {
            return false;
        }

        Department department = departmentRepository.findByDepartmentName(departmentName)
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setDepartmentName(departmentName);
                    return departmentRepository.save(d);
                });

        Employee employee = new Employee();
        employee.setEmployeeId(employeeId);
        employee.setEmployeeName(employeeName);
        employee.setDepartment(department);
        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        employee.setAccountStatus(AccountStatus.PENDING);
        employee.setRole(null);
        employeeRepository.save(employee);
        return true;
    }

    @Transactional(readOnly = true)
    public List<PendingEmployeeResponse> listPending() {
        return employeeRepository.findByAccountStatus(AccountStatus.PENDING)
                .stream()
                .map(PendingEmployeeResponse::from)
                .toList();
    }

    /**
     * Admin approves a pending account and assigns its role.
     * Returns false if the employee doesn't exist or isn't currently PENDING
     * (prevents re-approving an already-approved/rejected account by accident).
     */
    @Transactional
    public boolean approveEmployee(String employeeId, Role role) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return false;
        }
        Employee employee = employeeOpt.get();
        if (employee.getAccountStatus() != AccountStatus.PENDING) {
            return false;
        }
        employee.setRole(role);
        employee.setAccountStatus(AccountStatus.APPROVED);
        employeeRepository.save(employee);
        return true;
    }

    @Transactional
    public boolean rejectEmployee(String employeeId) {
        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(employeeId);
        if (employeeOpt.isEmpty()) {
            return false;
        }
        Employee employee = employeeOpt.get();
        if (employee.getAccountStatus() != AccountStatus.PENDING) {
            return false;
        }
        employee.setAccountStatus(AccountStatus.REJECTED);
        employeeRepository.save(employee);
        return true;
    }

    /**
     * Existing admin shortcut: directly create/update a login with a role,
     * bypassing the pending-approval flow entirely (e.g. for bootstrapping
     * the first admin, or provisioning known staff without self-registration).
     */
    @Transactional
    public boolean provisionLogin(String employeeId, String employeeName, String rawPassword, Role role) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseGet(() -> {

                    Employee newEmployee = new Employee();
                    newEmployee.setEmployeeId(employeeId);
                    newEmployee.setEmployeeName(
                            (employeeName == null || employeeName.isBlank()) ? employeeId : employeeName
                    );
                    return newEmployee;
                });

        employee.setPasswordHash(passwordEncoder.encode(rawPassword));
        employee.setRole(role != null ? role : Role.USER);
        employee.setAccountStatus(AccountStatus.APPROVED);
        employeeRepository.save(employee);
        return true;
    }

}