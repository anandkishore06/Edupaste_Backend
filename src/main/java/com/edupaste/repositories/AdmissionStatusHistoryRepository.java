package com.edupaste.repositories;

import com.edupaste.models.AdmissionStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdmissionStatusHistoryRepository extends JpaRepository<AdmissionStatusHistory, UUID> {
    List<AdmissionStatusHistory> findByAdmissionApplicationIdOrderByChangedAtAsc(UUID admissionApplicationId);
}
