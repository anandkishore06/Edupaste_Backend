package com.edupaste.repositories;

import com.edupaste.models.AdmissionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AdmissionReviewRepository extends JpaRepository<AdmissionReview, UUID> {
}
