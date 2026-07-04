package com.MyNicProject.hrms.service;

import com.MyNicProject.hrms.dto.CompleteRecordRequest;
import com.MyNicProject.hrms.dto.TrainingRecordRequest;
import com.MyNicProject.hrms.entity.*;
import com.MyNicProject.hrms.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class TrainingRecordService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png"
    );


    public enum SaveOutcome {
        OK, INVALID_FILE_TYPE, CERTIFICATE_REQUIRED_FOR_COMPLETED
    }

    public record SaveResult(SaveOutcome outcome, TrainingRecord record) {
        public static SaveResult ok(TrainingRecord r) { return new SaveResult(SaveOutcome.OK, r); }
        public static SaveResult error(SaveOutcome o) { return new SaveResult(o, null); }
    }

    @Value("${file.upload-dir:uploads/certificates}")
    private String uploadDir;

    private final EmployeeRepository employeeRepo;
    private final TrainingRecordRepository recordRepo;
    private final TrainingModuleRepository moduleRepo;

    public TrainingRecordService(EmployeeRepository employeeRepo,
                                 TrainingRecordRepository recordRepo, TrainingModuleRepository moduleRepo) {
        this.employeeRepo = employeeRepo;
        this.recordRepo = recordRepo;
        this.moduleRepo = moduleRepo;
    }

    /**
     * Creates a new training record for an already-resolved Employee.
     * Department is intentionally NOT part of the request — it comes solely
     * from the employee's own registration record (normalization: a certificate
     * submission shouldn't be able to re-declare/override someone's department).
     *
     * Business rule: a record submitted as COMPLETED must have a certificate
     * attached immediately. A record submitted as IN_PROGRESS may omit the
     * file — it can be completed later via completeRecord().
     */
    @Transactional
    public SaveResult saveRecord(Employee employee, TrainingRecordRequest req, MultipartFile file) throws IOException {

        boolean hasFile = file != null && !file.isEmpty();

        if (hasFile) {
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                return SaveResult.error(SaveOutcome.INVALID_FILE_TYPE);
            }
        }

        Status status = req.status() != null ? Status.valueOf(req.status()) : Status.IN_PROGRESS;

        if (status == Status.COMPLETED && !hasFile) {
            return SaveResult.error(SaveOutcome.CERTIFICATE_REQUIRED_FOR_COMPLETED);
        }

        TrainingModule module = moduleRepo.findByModuleName(req.trainingModule())
                .orElseGet(() -> {
                    TrainingModule m = new TrainingModule();
                    m.setModuleName(req.trainingModule());
                    m.setTrainingType(req.trainingType());
                    return moduleRepo.save(m);
                });

        TrainingRecord record = new TrainingRecord();
        record.setEmployee(employee);
        record.setModule(module);
        record.setInstructorName(req.instructor());
        record.setCertificateNumber(req.certificateNumber());
        record.setRemarks(req.remarks());
        record.setIssueDate(req.issueDate());
        record.setStatus(status);


        if (hasFile) {
            attachFile(record, file);
        }

        return SaveResult.ok(recordRepo.save(record));
    }

    /**
     * Completes a previously IN_PROGRESS record: attaches the certificate
     * (now mandatory), flips status to COMPLETED, and resets approvalStatus
     * to WAITING so the admin reviews it fresh.
     * Returns null if the record isn't currently IN_PROGRESS (can't "complete"
     * something already completed, or something that doesn't exist).
     */
    @Transactional
    public SaveResult completeRecord(TrainingRecord record, CompleteRecordRequest req, MultipartFile file) throws IOException {
        if (record.getStatus() != Status.IN_PROGRESS) {
            return SaveResult.error(SaveOutcome.CERTIFICATE_REQUIRED_FOR_COMPLETED); // reused: "not eligible"
        }

        boolean hasFile = file != null && !file.isEmpty();
        if (!hasFile) {
            return SaveResult.error(SaveOutcome.CERTIFICATE_REQUIRED_FOR_COMPLETED);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            return SaveResult.error(SaveOutcome.INVALID_FILE_TYPE);
        }

        record.setCertificateNumber(req.certificateNumber());
        if (req.issueDate() != null) {
            record.setIssueDate(req.issueDate());
        }
        if (req.remarks() != null && !req.remarks().isBlank()) {
            record.setRemarks(req.remarks());
        }
        attachFile(record, file);
        record.setStatus(Status.COMPLETED);
        record.setApprovalStatus(ApprovalStatus.WAITING);
        record.setAdminRemarks(null);

        return SaveResult.ok(recordRepo.save(record));
    }

    /**
     * Admin's approve/invalidate decision. Only meaningful for COMPLETED
     * records (nothing to review on an IN_PROGRESS one with no certificate).
     * Returns false if the record isn't COMPLETED, or if INVALID is chosen
     * without remarks.
     */
    @Transactional
    public boolean decideApproval(TrainingRecord record, ApprovalStatus decision, String remarks) {
        if (record.getStatus() != Status.COMPLETED) {
            return false;
        }
        if (decision == ApprovalStatus.INVALID && (remarks == null || remarks.isBlank())) {
            return false;
        }
        record.setApprovalStatus(decision);
        record.setAdminRemarks(decision == ApprovalStatus.INVALID ? remarks : null);
        recordRepo.save(record);
        return true;
    }

    private void attachFile(TrainingRecord record, MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String safeFilename = originalFilename.replaceAll("[\\r\\n\"]", "_"); // strip CR/LF/quotes
        String uniqueFilename = UUID.randomUUID() + "-" + safeFilename;

        Path targetLocation = uploadPath.resolve(uniqueFilename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);


        if (record.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(record.getFilePath()));
            } catch (IOException ignored) {

            }
        }

        record.setFileName(safeFilename);
        record.setFileType(file.getContentType());
        record.setFilePath(targetLocation.toString());
    }

    @Transactional(readOnly = true)
    public List<TrainingRecord> getRecordsForEmployee(String employeeId) {
        return recordRepo.findByEmployeeIdWithDetails(employeeId);
    }

    @Transactional(readOnly = true)
    public TrainingRecord getRecordById(Long recordId) {
        return recordRepo.findById(recordId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TrainingRecord> getAllRecords() {
        return recordRepo.findAllWithDetails();
    }

    /**
     * Deletion is now an owner-only action (admins review via approve/invalidate
     * instead of deleting). The controller enforces the ownership check; this
     * method just performs the deletion once authorized.
     */
    @Transactional
    public boolean deleteRecord(Long recordId) {
        TrainingRecord record = recordRepo.findById(recordId).orElse(null);
        if (record == null) {
            return false;
        }
        if (record.getFilePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(record.getFilePath()));
            } catch (IOException ignored) {

            }
        }
        recordRepo.deleteById(recordId);
        return true;
    }
}