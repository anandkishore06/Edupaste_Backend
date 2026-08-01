package com.edupaste.repositories;

import com.edupaste.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);

    @Query("SELECT COALESCE(MAX(u.schoolId), 1) FROM User u")
    Long findMaxSchoolId();
}
