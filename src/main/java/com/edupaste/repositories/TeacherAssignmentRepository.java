package com.edupaste.repositories;

import com.edupaste.models.TeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface TeacherAssignmentRepository extends JpaRepository<TeacherAssignment, UUID> {
    Page<TeacherAssignment> findBySchoolId(Long schoolId, Pageable pageable);
    List<TeacherAssignment> findBySchoolIdAndTeacherId(Long schoolId, Long teacherId);
    List<TeacherAssignment> findBySchoolIdAndClassSubjectId(Long schoolId, UUID classSubjectId);
}
