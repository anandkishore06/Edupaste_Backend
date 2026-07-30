package com.edupaste.repositories;

import com.edupaste.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findBySchoolId(Long schoolId);
    Page<Section> findBySchoolId(Long schoolId, Pageable pageable);
}
