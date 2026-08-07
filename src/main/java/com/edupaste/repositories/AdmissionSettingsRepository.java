package com.edupaste.repositories;

import com.edupaste.models.AdmissionSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdmissionSettingsRepository extends JpaRepository<AdmissionSettings, UUID> {
    Optional<AdmissionSettings> findBySchoolId(Long schoolId);
    boolean existsByPublicCode(String publicCode);
    Optional<AdmissionSettings> findByPublicCode(String publicCode);
}
