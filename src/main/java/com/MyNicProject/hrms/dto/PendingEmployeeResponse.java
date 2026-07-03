package com.MyNicProject.hrms.dto;

import com.MyNicProject.hrms.entity.Employee;

import java.time.LocalDateTime;

public record PendingEmployeeResponse(
        String employeeId,
        String employeeName,
        String department,
        LocalDateTime registeredAt
) {
    public static PendingEmployeeResponse from(Employee e) {
        return new PendingEmployeeResponse(
                e.getEmployeeId(),
                e.getEmployeeName(),
                e.getDepartment() != null ? e.getDepartment().getDepartmentName() : null,
                e.getCreatedAt()
        );
    }
}