package com.MyNicProject.hrms.repository;

import com.MyNicProject.hrms.entity.TrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TrainingRecordRepository extends JpaRepository<TrainingRecord, Long> {


    @Query("""
        SELECT tr FROM TrainingRecord tr
        JOIN FETCH tr.employee e
        JOIN FETCH tr.module m
        WHERE e.employeeId = :employeeId
        """)
    List<TrainingRecord> findByEmployeeIdWithDetails(@Param("employeeId") String employeeId);
}