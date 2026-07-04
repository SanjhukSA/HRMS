package com.MyNicProject.hrms.controller;

import com.MyNicProject.hrms.dto.ApprovalDecisionRequest;
import com.MyNicProject.hrms.dto.CompleteRecordRequest;
import com.MyNicProject.hrms.dto.TrainingRecordRequest;
import com.MyNicProject.hrms.dto.TrainingRecordResponse;
import com.MyNicProject.hrms.entity.ApprovalStatus;
import com.MyNicProject.hrms.entity.Employee;
import com.MyNicProject.hrms.entity.TrainingRecord;
import com.MyNicProject.hrms.repository.EmployeeRepository;
import com.MyNicProject.hrms.service.TrainingRecordService;
import jakarta.validation.Valid;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
public class TrainingRecordController {

    private final TrainingRecordService trainingService;
    private final EmployeeRepository employeeRepository;

    public TrainingRecordController(TrainingRecordService trainingService, EmployeeRepository employeeRepository) {
        this.trainingService = trainingService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveTrainingRecord(
            @Valid @RequestPart("data") TrainingRecordRequest request,
            @RequestPart(value = "certificateFile", required = false) MultipartFile file,
            Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));


        String targetEmployeeId = isAdmin ? request.employeeId() : authentication.getName();

        Optional<Employee> employeeOpt = employeeRepository.findByEmployeeId(targetEmployeeId);
        if (employeeOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No employee record found for " + targetEmployeeId
                            + ". Ask an administrator to provision your account first.");
        }

        try {
            TrainingRecordService.SaveResult result =
                    trainingService.saveRecord(employeeOpt.get(), request, file);

            return switch (result.outcome()) {
                case OK -> ResponseEntity.ok(TrainingRecordResponse.from(result.record()));
                case INVALID_FILE_TYPE -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF, JPEG, or PNG files are allowed");
                case CERTIFICATE_REQUIRED_FOR_COMPLETED -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("A certificate file is required to mark a record as Completed");
            };

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save certificate: " + e.getMessage());
        }
    }

    /**
     * Completes a previously IN_PROGRESS record: uploads the certificate
     * (now mandatory) and flips status to COMPLETED, ready for admin review.
     * Owner-only — an employee completes their own record.
     */
    @PatchMapping(value = "/{recordId}/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> completeRecord(
            @PathVariable Long recordId,
            @Valid @RequestPart("data") CompleteRecordRequest request,
            @RequestPart("certificateFile") MultipartFile file,
            Authentication authentication) {

        TrainingRecord record = trainingService.getRecordById(recordId);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found: " + recordId);
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = authentication.getName().equals(record.getEmployee().getEmployeeId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only complete your own training records");
        }

        try {
            TrainingRecordService.SaveResult result = trainingService.completeRecord(record, request, file);

            return switch (result.outcome()) {
                case OK -> ResponseEntity.ok(TrainingRecordResponse.from(result.record()));
                case INVALID_FILE_TYPE -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF, JPEG, or PNG files are allowed");
                case CERTIFICATE_REQUIRED_FOR_COMPLETED -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("This record can't be completed — it may already be completed, or a certificate is required");
            };
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to complete record: " + e.getMessage());
        }
    }

    /**
     *Admin approve/invalidate decision on a COMPLETED record.
     * Replaces the old delete-based workflow entirely.
     */
    @PostMapping("/{recordId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> decideApproval(@PathVariable Long recordId, @Valid @RequestBody ApprovalDecisionRequest request) {
        TrainingRecord record = trainingService.getRecordById(recordId);
        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Record not found: " + recordId);
        }

        ApprovalStatus decision = ApprovalStatus.valueOf(request.decision());
        boolean success = trainingService.decideApproval(record, decision, request.remarks());

        if (!success) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Could not apply decision — the record must be COMPLETED, and remarks are required when marking INVALID");
        }

        return ResponseEntity.ok("Record " + recordId + " marked " + request.decision());
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrainingRecordResponse>> getAllRecords() {

        List<TrainingRecordResponse> responses = trainingService.getAllRecords()
                .stream()
                .map(TrainingRecordResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getRecordsByEmployeeId(
            @PathVariable String employeeId, Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSelf = authentication.getName().equals(employeeId);

        if (!isAdmin && !isSelf) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only view your own certificates");
        }

        List<TrainingRecordResponse> responses = trainingService.getRecordsForEmployee(employeeId)
                .stream()
                .map(TrainingRecordResponse::from)
                .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Deletion is owner-only now. Admins no longer delete records — they
     * review completed ones via /approve instead.
     */
    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long recordId, Authentication authentication) {
        TrainingRecord record = trainingService.getRecordById(recordId);

        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found: " + recordId);
        }

        boolean isOwner = authentication.getName().equals(record.getEmployee().getEmployeeId());
        if (!isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You can only delete your own certificates. Admins review records via approve/invalidate instead of deleting.");
        }

        boolean deleted = trainingService.deleteRecord(recordId);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found: " + recordId);
        }
        return ResponseEntity.ok("Record deleted successfully");
    }

    @GetMapping("/download/{recordId}")
    public ResponseEntity<?> downloadCertificate(@PathVariable Long recordId, Authentication authentication) {

        TrainingRecord record = trainingService.getRecordById(recordId);

        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found: " + recordId);
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = authentication.getName().equals(record.getEmployee().getEmployeeId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only download your own certificates");
        }

        if (record.getFilePath() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No file attached to this certificate");
        }

        try {
            Path filePath = Paths.get(record.getFilePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File missing on server");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(record.getFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + record.getFileName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error reading file");
        }
    }
}