package com.MyNicProject.hrms.repository;

import com.MyNicProject.hrms.entity.TrainingModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrainingModuleRepository extends JpaRepository<TrainingModule, Long> {

    Optional<TrainingModule> findByModuleName(String moduleName);
}