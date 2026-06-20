package com.MyNicProject.hrms.dto;

import com.MyNicProject.hrms.entity.TrainingRecord;

import java.time.LocalDate;


public record TrainingRecordResponse(
        Long recordId,
        String employeeId,
        String employeeName,
        String moduleName,
        String status,
        LocalDate issueDate,
        String certificateNumber,
        String fileName,
        String fileType
) {

    public static TrainingRecordResponse from(TrainingRecord r) {
        return new TrainingRecordResponse(
                r.getRecordId(),
                r.getEmployee().getEmployeeId(),
                r.getEmployee().getEmployeeName(),
                r.getModule().getModuleName(),
                r.getStatus().name(),
                r.getIssueDate(),
                r.getCertificateNumber(),
                r.getFileName(),
                r.getFileType()
        );
    }
}