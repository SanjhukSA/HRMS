package com.MyNicProject.hrms.security;

import com.MyNicProject.hrms.entity.Employee;
import com.MyNicProject.hrms.repository.EmployeeRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public CustomUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String employeeId) {
        Employee employee = employeeRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> new UsernameNotFoundException("No employee: " + employeeId));

        if (!employee.isCanLogin() || employee.getPasswordHash() == null) {
            throw new UsernameNotFoundException("Login not enabled for: " + employeeId);
        }

        return User.builder()
                .username(employee.getEmployeeId())
                .password(employee.getPasswordHash())
                .authorities(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()))
                .build();
    }
}