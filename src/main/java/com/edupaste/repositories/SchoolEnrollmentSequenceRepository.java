package com.edupaste.repositories;

import com.edupaste.models.SchoolEnrollmentSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolEnrollmentSequenceRepository extends JpaRepository<SchoolEnrollmentSequence, UUID> {
    Optional<SchoolEnrollmentSequence> findBySchoolIdAndCurrentYear(Long schoolId, Integer currentYear);
}
