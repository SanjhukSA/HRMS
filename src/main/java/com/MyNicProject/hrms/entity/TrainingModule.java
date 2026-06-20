package com.MyNicProject.hrms.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Entity
@Table(name = "training_module")
@Getter
@Setter
public class TrainingModule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "module_id")
    private Long moduleId;

    @Column(name = "module_name", nullable = false,unique = true)
    private String moduleName;

    @Column(name = "training_type", nullable = false)
    private String trainingType;


}