package com.edupaste.repositories;

import com.edupaste.models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findBySchoolId(Long schoolId);
    Page<Subject> findBySchoolId(Long schoolId, Pageable pageable);
}
