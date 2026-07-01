package com.MyNicProject.hrms.service;

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

    @Value("${file.upload-dir:uploads/certificates}")
    private String uploadDir;

    private final DepartmentRepository departmentRepo;
    private final EmployeeRepository employeeRepo;
    private final TrainingRecordRepository recordRepo;
    private final TrainingModuleRepository moduleRepo;

    public TrainingRecordService(DepartmentRepository departmentRepo, EmployeeRepository employeeRepo,
                                 TrainingRecordRepository recordRepo, TrainingModuleRepository moduleRepo) {
        this.departmentRepo = departmentRepo;
        this.employeeRepo = employeeRepo;
        this.recordRepo = recordRepo;
        this.moduleRepo = moduleRepo;
    }


    @Transactional
    public TrainingRecord saveRecord(TrainingRecordRequest req, MultipartFile file) throws IOException {



        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
                return null;
            }
        }

        Department department = departmentRepo.findByDepartmentName(req.department())
                .orElseGet(() -> {
                    Department d = new Department();
                    d.setDepartmentName(req.department());
                    return departmentRepo.save(d);
                });

        Employee employee = employeeRepo.findByEmployeeId(req.employeeId())
                .orElseGet(() -> {
                    Employee e = new Employee();
                    e.setEmployeeId(req.employeeId());
                    e.setEmployeeName(req.employeeName());
                    e.setDepartment(department);
                    return employeeRepo.save(e);
                });

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
        if (req.status() != null) {
            record.setStatus(Status.valueOf(req.status()));
        }

        if (file != null && !file.isEmpty()) {
            attachFile(record, file);
        }

        return recordRepo.save(record);
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
                // file already gone / not critical to the delete operation
            }
        }
        recordRepo.deleteById(recordId);
        return true;
    }
}