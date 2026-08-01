package com.edupaste.repositories;

import com.edupaste.models.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findBySchoolId(Long schoolId);
    List<SchoolClass> findBySchoolIdOrderByNameAsc(Long schoolId);
    Page<SchoolClass> findBySchoolIdOrderByNameAsc(Long schoolId, Pageable pageable);
}
