package com.MyNicProject.hrms.controller;

import com.MyNicProject.hrms.dto.TrainingRecordRequest;
import com.MyNicProject.hrms.dto.TrainingRecordResponse;
import com.MyNicProject.hrms.entity.TrainingRecord;
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

@RestController
@RequestMapping("/api/certificates")
public class TrainingRecordController {

    private final TrainingRecordService trainingService;

    public TrainingRecordController(TrainingRecordService trainingService) {
        this.trainingService = trainingService;
    }

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> saveTrainingRecord(
            @Valid @RequestPart("data") TrainingRecordRequest request,
            @RequestPart(value = "certificateFile", required = false) MultipartFile file) {

        try {
            TrainingRecord saved = trainingService.saveRecord(request, file);


            if (saved == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("Only PDF, JPEG, or PNG files are allowed");
            }

            return ResponseEntity.ok(TrainingRecordResponse.from(saved));

        } catch (Exception e) {

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to save certificate: " + e.getMessage());
        }
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

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TrainingRecordResponse>> getAllRecords() {
        List<TrainingRecordResponse> responses = trainingService.getAllRecords()
                .stream()
                .map(TrainingRecordResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{recordId}")
    public ResponseEntity<?> deleteRecord(@PathVariable Long recordId, Authentication authentication) {
        TrainingRecord record = trainingService.getRecordById(recordId);

        if (record == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Certificate not found: " + recordId);
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = authentication.getName().equals(record.getEmployee().getEmployeeId());

        if (!isAdmin && !isOwner) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You can only delete your own certificates");
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
