package com.edupaste.repositories;

import com.edupaste.models.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    Page<StudentEnrollment> findBySchoolId(Long schoolId, Pageable pageable);
    List<StudentEnrollment> findBySchoolIdAndSectionId(Long schoolId, UUID sectionId);
    List<StudentEnrollment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    Optional<StudentEnrollment> findBySchoolIdAndStudentIdAndSectionSchoolClassSessionId(Long schoolId, Long studentId, UUID sessionId);
}
