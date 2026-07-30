package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.SchoolClass;
import com.edupaste.payloads.SchoolClassRequest;
import com.edupaste.payloads.SchoolClassResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SchoolClassService {

    @Autowired
    private SchoolClassRepository repository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    public Page<SchoolClassResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public List<SchoolClassResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SchoolClassResponse create(SchoolClassRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AcademicSession session = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                .orElseThrow(() -> new RuntimeException("No active academic session found for this school."));
                
        SchoolClass sc = new SchoolClass();
        sc.setSchoolId(schoolId);
        sc.setSession(session);
        sc.setName(request.getName());
        sc.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) sc.setDisplayOrder(request.getDisplayOrder());
        
        // Validation: Duplicate name in same session
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(c -> c.getSession().getId().equals(session.getId()) && c.getName().equalsIgnoreCase(request.getName()));
        if (exists) {
            throw new RuntimeException("A class with this name already exists in the current session.");
        }
        
        sc = repository.save(sc);
        return mapToResponse(sc);
    }

    
    public SchoolClassResponse update(UUID id, SchoolClassRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getName() != null) {
            // Validation: Duplicate name in same session
            final java.util.UUID currentSessionId = entity.getSession().getId();
            boolean exists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(c -> !c.getId().equals(id) && c.getSession().getId().equals(currentSessionId) && c.getName().equalsIgnoreCase(request.getName()));
            if (exists) {
                throw new RuntimeException("A class with this name already exists in the current session.");
            }
            entity.setName(request.getName());
        }
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());

        
        entity = repository.save(entity);
        return mapToResponse(entity);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        repository.delete(entity);
    }

private SchoolClassResponse mapToResponse(SchoolClass sc) {
        SchoolClassResponse res = new SchoolClassResponse();
        res.setId(sc.getId());
        res.setName(sc.getName());
        res.setDescription(sc.getDescription());
        res.setDisplayOrder(sc.getDisplayOrder());
        return res;
    }
}
