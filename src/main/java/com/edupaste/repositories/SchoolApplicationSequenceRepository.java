package com.edupaste.repositories;

import com.edupaste.models.SchoolApplicationSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolApplicationSequenceRepository extends JpaRepository<SchoolApplicationSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM SchoolApplicationSequence s WHERE s.schoolId = :schoolId AND s.currentYear = :currentYear")
    Optional<SchoolApplicationSequence> findBySchoolIdAndCurrentYearForUpdate(
            @Param("schoolId") Long schoolId,
            @Param("currentYear") Integer currentYear
    );
}
