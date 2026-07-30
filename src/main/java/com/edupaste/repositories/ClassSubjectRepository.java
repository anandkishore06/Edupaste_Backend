package com.edupaste.repositories;

import com.edupaste.models.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {
    List<ClassSubject> findBySchoolId(Long schoolId);
    Page<ClassSubject> findBySchoolId(Long schoolId, Pageable pageable);
}
