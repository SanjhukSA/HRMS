package com.MyNicProject.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "training_record", indexes = {
        @Index(name = "idx_training_record_employee_id", columnList = "employee_id"),
        @Index(name = "idx_training_record_module_id", columnList = "module_id"),
        @Index(name = "idx_training_record_status", columnList = "status")
})
@Getter
@Setter
public class TrainingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "record_id")
    private Long recordId;


    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "module_id", nullable = false)
    private TrainingModule module;


    @Column(name = "instructor_name")
    private String instructorName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.IN_PROGRESS;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;


    @Column(name = "certificate_number", unique = true)
    private String certificateNumber;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();



}
