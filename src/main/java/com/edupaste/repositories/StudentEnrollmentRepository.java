package com.edupaste.repositories;

import com.edupaste.models.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface StudentEnrollmentRepository extends JpaRepository<StudentEnrollment, UUID> {
    Page<StudentEnrollment> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("SELECT e FROM StudentEnrollment e WHERE e.schoolId = :schoolId AND " +
           "(e.academicSession.id = :sessionId OR (e.academicSession IS NULL AND e.section.schoolClass.session.id = :sessionId))")
    Page<StudentEnrollment> findBySchoolIdAndAcademicSessionId(@Param("schoolId") Long schoolId, @Param("sessionId") UUID sessionId, Pageable pageable);
    List<StudentEnrollment> findBySchoolIdAndSectionId(Long schoolId, UUID sectionId);
    List<StudentEnrollment> findBySchoolIdAndStudentId(Long schoolId, Long studentId);
    Optional<StudentEnrollment> findBySchoolIdAndStudentIdAndSectionSchoolClassSessionId(Long schoolId, Long studentId, UUID sessionId);
    boolean existsBySchoolIdAndStudentId(Long schoolId, Long studentId);
    boolean existsBySchoolIdAndStudentIdAndIdNot(Long schoolId, Long studentId, UUID id);
    boolean existsBySchoolIdAndStudentIdAndSectionId(Long schoolId, Long studentId, UUID sectionId);
    boolean existsBySchoolIdAndStudentIdAndSectionIdAndIdNot(Long schoolId, Long studentId, UUID sectionId, UUID id);
}
