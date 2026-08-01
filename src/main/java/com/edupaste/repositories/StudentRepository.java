package com.edupaste.repositories;

import com.edupaste.models.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {
    List<Student> findBySchoolId(Long schoolId);
    Page<Student> findBySchoolId(Long schoolId, Pageable pageable);
    List<Student> findByParentId(UUID parentId);
    
    boolean existsBySchoolIdAndAdmissionNumber(Long schoolId, String admissionNumber);
    boolean existsBySchoolIdAndAdmissionNumberAndIdNot(Long schoolId, String admissionNumber, UUID id);
    
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
    java.util.Optional<Student> findBySchoolIdAndUserId(Long schoolId, Long userId);
}
