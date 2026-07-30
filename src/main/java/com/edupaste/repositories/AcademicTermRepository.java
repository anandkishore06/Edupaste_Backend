package com.edupaste.repositories;

import com.edupaste.models.AcademicTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface AcademicTermRepository extends JpaRepository<AcademicTerm, UUID> {
    Page<AcademicTerm> findBySchoolId(Long schoolId, Pageable pageable);
    List<AcademicTerm> findBySchoolIdAndSessionId(Long schoolId, UUID sessionId);
}
