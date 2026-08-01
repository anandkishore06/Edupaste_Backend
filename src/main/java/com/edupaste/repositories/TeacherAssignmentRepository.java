package com.edupaste.repositories;

import com.edupaste.models.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {
    Page<TeacherAssignment> findBySchoolId(Long schoolId, Pageable pageable);

    @Query("SELECT ta FROM TeacherAssignment ta WHERE ta.schoolId = :schoolId AND " +
           "(ta.academicSession.id = :sessionId OR (ta.academicSession IS NULL AND ta.classSubject.section.schoolClass.session.id = :sessionId))")
    Page<TeacherAssignment> findBySchoolIdAndAcademicSessionId(@Param("schoolId") Long schoolId, @Param("sessionId") UUID sessionId, Pageable pageable);
    List<TeacherAssignment> findBySchoolIdAndTeacherId(Long schoolId, Long teacherId);
    List<TeacherAssignment> findBySchoolIdAndClassSubjectId(Long schoolId, UUID classSubjectId);
    boolean existsBySchoolIdAndTeacherIdAndClassSubjectId(Long schoolId, Long teacherId, UUID classSubjectId);
    boolean existsBySchoolIdAndTeacherIdAndClassSubjectIdAndIdNot(Long schoolId, Long teacherId, UUID classSubjectId, UUID id);
}
