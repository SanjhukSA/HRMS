package com.MyNicProject.hrms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "employee", indexes = {@Index(name = "idx_employee_dept_id", columnList = "department_id")})
@Getter
@Setter
public class Employee {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "employee_name", nullable = false)
    private String employeeName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @JsonIgnore
    @Column(name ="password_hash")
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name ="role",nullable = false)
    private Role role =Role.User;

    @Column(name ="can_login", nullable = false)
    private boolean canLogin = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();


}