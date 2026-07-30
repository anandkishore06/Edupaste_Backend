package com.edupaste.repositories;

import com.edupaste.models.AcademicSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicSessionRepository extends JpaRepository<AcademicSession, UUID> {
    List<AcademicSession> findBySchoolId(Long schoolId);
    Page<AcademicSession> findBySchoolId(Long schoolId, Pageable pageable);
    Optional<AcademicSession> findBySchoolIdAndIsCurrentTrue(Long schoolId);
}
