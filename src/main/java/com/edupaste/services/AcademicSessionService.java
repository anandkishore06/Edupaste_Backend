package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.payloads.AcademicSessionRequest;
import com.edupaste.payloads.AcademicSessionResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicSessionService {

    @Autowired
    private AcademicSessionRepository repository;

    public Page<AcademicSessionResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public List<AcademicSessionResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public AcademicSessionResponse create(AcademicSessionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        
        if (request.getStartDate() != null && request.getEndDate() != null) {
            if (request.getStartDate().isAfter(request.getEndDate())) {
                throw new RuntimeException("Start date must be before end date.");
            }
        }
        
        boolean isFirst = repository.findBySchoolId(schoolId).isEmpty();
        boolean shouldBeCurrent = isFirst || (request.getIsCurrent() != null && request.getIsCurrent());
        
        if (shouldBeCurrent && !isFirst) {
            repository.findBySchoolId(schoolId).stream()
                .filter(AcademicSession::getIsCurrent)
                .forEach(s -> {
                    s.setIsCurrent(false);
                    repository.save(s);
                });
        }
        
        AcademicSession session = new AcademicSession();
        session.setSchoolId(schoolId);
        session.setName(request.getName());
        session.setStartDate(request.getStartDate());
        session.setEndDate(request.getEndDate());
        session.setIsCurrent(shouldBeCurrent);
        
        session = repository.save(session);
        return mapToResponse(session);
    }

    
    @Transactional
    public AcademicSessionResponse update(UUID id, AcademicSessionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            if (entity.getStartDate().isAfter(entity.getEndDate())) {
                throw new RuntimeException("Start date must be before end date.");
            }
        }
        
        if (request.getIsCurrent() != null && request.getIsCurrent() && !entity.getIsCurrent()) {
            repository.findBySchoolId(schoolId).stream()
                .filter(s -> !s.getId().equals(id) && s.getIsCurrent())
                .forEach(s -> {
                    s.setIsCurrent(false);
                    repository.save(s);
                });
            entity.setIsCurrent(true);
        }

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

private AcademicSessionResponse mapToResponse(AcademicSession session) {
        AcademicSessionResponse res = new AcademicSessionResponse();
        res.setId(session.getId());
        res.setName(session.getName());
        res.setStartDate(session.getStartDate());
        res.setEndDate(session.getEndDate());
        res.setIsCurrent(session.getIsCurrent());
        return res;
    }
}
