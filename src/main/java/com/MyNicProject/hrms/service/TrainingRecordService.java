package com.MyNicProject.hrms.service;

import com.MyNicProject.hrms.entity.Department;
import com.MyNicProject.hrms.entity.Employee;
import com.MyNicProject.hrms.entity.TrainingModule;
import com.MyNicProject.hrms.entity.TrainingRecord;
import com.MyNicProject.hrms.repository.DepartmentRepository;
import com.MyNicProject.hrms.repository.EmployeeRepository;
import com.MyNicProject.hrms.repository.TrainingModuleRepository;
import com.MyNicProject.hrms.repository.TrainingRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
public class TrainingRecordService {

    @Value("${file.upload-dir:uploads/certificates}")
    private  String uploadDir;

    @Autowired
    private DepartmentRepository departmentRepo;

    @Autowired
    private EmployeeRepository employeeRepo;

    @Autowired
    private TrainingRecordRepository recordRepo;

    @Autowired
    private TrainingModuleRepository moduleRepo;


   @Transactional
    public TrainingRecord saveRecord(
            String employeeName, String employeeId, String departmentName,
            String moduleName, String trainingType , String instructorName,
            String status, LocalDate issueDate , String remarks,String certificateNum,
            MultipartFile file) throws IOException{

       Optional<Department> dept = departmentRepo.findByDepartmentName(departmentName);
       Department department;
       if(dept.isPresent()){
           department = dept.get();
       }else{
           Department newDept = new Department();
           newDept.setDepartmentName(departmentName);
           department = departmentRepo.save(newDept);
       }

       Optional<Employee> emp = employeeRepo.findByEmployeeId(employeeId);
       Employee employee;
       if(emp.isPresent()){
           employee = emp.get();
       }else{
           Employee newEmp = new Employee();
           newEmp.setEmployeeName(employeeName);
           newEmp.setEmployeeId(employeeId);
           newEmp.setDepartment(department);
           employee = employeeRepo.save(newEmp);
       }

       Optional< TrainingModule> Tmodule = moduleRepo.findByModuleName(moduleName);
       TrainingModule module ;
       if(Tmodule.isPresent()){
           module = Tmodule.get();
       }else{
           TrainingModule newModule = new TrainingModule();
           newModule.setModuleName(moduleName);
           newModule.setTrainingType(trainingType);
           module = moduleRepo.save(newModule);
       }

       TrainingRecord record = new TrainingRecord();
       record.setEmployee(employee);
       record.setModule(module);
       record.setInstructorName(instructorName);
       record.setCertificateNumber(certificateNum);
       record.setRemarks(remarks);
       record.setIssueDate(issueDate);
       record.setStatus(status);

       if(file != null && !file.isEmpty()){
           Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
           if(!Files.exists(uploadPath)){
               Files.createDirectories(uploadPath);
           }
           String originalFname= StringUtils.cleanPath(file.getOriginalFilename());
           String uniqueFname = UUID.randomUUID().toString()+ "-"+originalFname;
           Path targetLocation = uploadPath.resolve(uniqueFname);

           Files.copy(file.getInputStream(),targetLocation, StandardCopyOption.REPLACE_EXISTING);

           record.setFileName(originalFname);
           String contentType = file.getContentType();
           record.setFileType(contentType == null || contentType.isEmpty()?"application/octet-stream":contentType);
           record.setFilePath(targetLocation.toString());
       }

     return recordRepo.save(record);
   }

}
