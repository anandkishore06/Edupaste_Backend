package com.edupaste.repositories;

import com.edupaste.models.Parent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {
    List<Parent> findBySchoolId(Long schoolId);
    Page<Parent> findBySchoolId(Long schoolId, Pageable pageable);
    
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, UUID id);
}
