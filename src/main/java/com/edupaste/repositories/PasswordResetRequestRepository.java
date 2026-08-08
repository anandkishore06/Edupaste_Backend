package com.edupaste.repositories;

import com.edupaste.models.PasswordResetRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PasswordResetRequestRepository extends JpaRepository<PasswordResetRequest, UUID> {
    Optional<PasswordResetRequest> findByIdAndResetToken(UUID id, String resetToken);
}
