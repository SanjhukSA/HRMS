package com.MyNicProject.hrms.dto;

import com.MyNicProject.hrms.entity.TrainingRecord;

import java.time.LocalDate;


public record TrainingRecordResponse(
        Long recordId,
        String employeeId,
        String employeeName,
        String department,
        String moduleName,
        String trainingType,
        String instructorName,
        String status,
        LocalDate issueDate,
        String certificateNumber,
        String remarks,
        String fileName,
        String fileType
) {

    public static TrainingRecordResponse from(TrainingRecord r) {
        return new TrainingRecordResponse(
                r.getRecordId(),
                r.getEmployee().getEmployeeId(),
                r.getEmployee().getEmployeeName(),
                r.getEmployee().getDepartment() != null ? r.getEmployee().getDepartment().getDepartmentName() : null,
                r.getModule().getModuleName(),
                r.getModule().getTrainingType(),
                r.getInstructorName(),
                r.getStatus().name(),
                r.getIssueDate(),
                r.getCertificateNumber(),
                r.getRemarks(),
                r.getFileName(),
                r.getFileType()
        );
    }
}