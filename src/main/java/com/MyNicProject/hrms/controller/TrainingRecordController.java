package com.MyNicProject.hrms.controller;

import com.MyNicProject.hrms.entity.TrainingRecord;
import com.MyNicProject.hrms.repository.TrainingRecordRepository;
import com.MyNicProject.hrms.service.TrainingRecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/certificates")
@CrossOrigin(origins = "*")
public class TrainingRecordController {

    @Autowired
    private TrainingRecordService trainingService;

    @Autowired
    private TrainingRecordRepository trainingRepo;

    @PostMapping(value = "/save", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> saveTrainingRecord(
            @RequestParam("employeeName") String employeeName,
            @RequestParam("employeeId") String employeeId,
            @RequestParam("department")String department,
            @RequestParam("trainingModule") String trainingModule,
            @RequestParam("trainingType") String trainingType,
            @RequestParam("instructor") String instructor,
            @RequestParam("status") String status,
            @RequestParam(value = "issueDate",required = false) String issueDate,
            @RequestParam(value = "certificateNumber",required = false)String certificateNumber,
            @RequestParam(value = "remarks",required = false) String remarks,
            @RequestParam(value = "certificateFile", required = false)MultipartFile file){

        try{
            LocalDate parsedDate = null;
            if(issueDate != null && !issueDate.isEmpty()){
                parsedDate = LocalDate.parse(issueDate);
            }

            trainingService.saveRecord(
                    employeeName,employeeId,department,trainingModule,trainingType,instructor,status,
                    parsedDate,remarks,certificateNumber,file);
            return ResponseEntity.status(HttpStatus.OK).body("Certificate saved successfully gng");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("GNG we have faced some error"+ e.getMessage());
        }
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<TrainingRecord>> getRecordsByEmployeeId(@PathVariable String employeeId){
        List<TrainingRecord> records = trainingRepo.findByEmployee_EmployeeId(employeeId);
        return ResponseEntity.ok(records);
    }

    @GetMapping("/download/{recordId}")
    public ResponseEntity<Resource> downloadCertificate(@PathVariable Long recordId){

        try{
            Optional<TrainingRecord> record = trainingRepo.findById(recordId);
            if(record.isPresent() && record.get().getFilePath() !=null){
                TrainingRecord newRecord = record.get();
                Path filePath = Paths.get(newRecord.getFilePath()).normalize();
                Resource resource = new UrlResource(filePath.toUri());
                if(resource.exists()){
                    return ResponseEntity.ok().contentType(MediaType.parseMediaType(newRecord.getFileType()))
                            .header(HttpHeaders.CONTENT_DISPOSITION,"inline ; filename=\"" + newRecord.getFileName() +"\"")
                            .body(resource);
                }
            }
            return ResponseEntity.notFound().build();
        }catch (Exception e){
            return ResponseEntity.internalServerError().build();
        }
    }

}
