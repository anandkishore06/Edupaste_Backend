package com.edupaste.repositories;

import com.edupaste.models.AdmissionDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdmissionDocumentRepository extends JpaRepository<AdmissionDocument, UUID> {
    List<AdmissionDocument> findByAdmissionApplicationId(UUID admissionApplicationId);
}
