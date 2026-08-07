package com.edupaste.repositories;

import com.edupaste.models.AdmissionApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface AdmissionApplicationRepository extends JpaRepository<AdmissionApplication, UUID>, JpaSpecificationExecutor<AdmissionApplication> {

    List<AdmissionApplication> findBySchoolId(Long schoolId);
    
    AdmissionApplication findFirstBySchoolIdAndSubmittedAtGreaterThanOrderBySubmittedAtAsc(Long schoolId, LocalDateTime submittedAt);
    AdmissionApplication findFirstBySchoolIdAndSubmittedAtLessThanOrderBySubmittedAtDesc(Long schoolId, LocalDateTime submittedAt);

    Optional<AdmissionApplication> findByApplicationNumber(String applicationNumber);

    Optional<AdmissionApplication> findBySchoolIdAndApplicationNumber(Long schoolId, String applicationNumber);

    boolean existsBySchoolIdAndFatherEmailAndFirstNameAndLastNameAndApplyingClassId(
            Long schoolId, String fatherEmail, String firstName, String lastName, UUID applyingClassId
    );

    @Query("SELECT a FROM AdmissionApplication a WHERE a.applicationNumber = :appNumber " +
           "AND a.dateOfBirth = :dob " +
           "AND (a.fatherMobile = :mobile OR a.motherMobile = :mobile OR a.guardianMobile = :mobile OR a.mobile = :mobile)")
    Optional<AdmissionApplication> findForPublicTracking(
            @Param("appNumber") String appNumber,
            @Param("dob") LocalDate dob,
            @Param("mobile") String mobile
    );
}
