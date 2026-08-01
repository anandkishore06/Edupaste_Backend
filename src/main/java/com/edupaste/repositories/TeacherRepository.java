package com.edupaste.repositories;

import com.edupaste.models.Teacher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeacherRepository extends JpaRepository<Teacher, UUID> {
    List<Teacher> findBySchoolId(Long schoolId);
    Page<Teacher> findBySchoolId(Long schoolId, Pageable pageable);
    
    boolean existsBySchoolIdAndEmployeeId(Long schoolId, String employeeId);
    boolean existsBySchoolIdAndEmployeeIdAndIdNot(Long schoolId, String employeeId, UUID id);
    
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
