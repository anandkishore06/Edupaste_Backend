package com.edupaste.services;

import com.edupaste.models.AcademicTerm;
import com.edupaste.payloads.AcademicTermRequest;
import com.edupaste.payloads.AcademicTermResponse;
import com.edupaste.repositories.AcademicTermRepository;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
public class AcademicTermService {
    @Autowired private AcademicTermRepository repository;
    @Autowired private AcademicSessionRepository sessionRepository;

    public Page<AcademicTermResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public AcademicTermResponse create(AcademicTermRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var session = sessionRepository.findById(request.getSessionId())
                .orElseThrow(() -> new RuntimeException("Session not found"));
        
        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new RuntimeException("Start date must be before end date");
        }

        AcademicTerm term = new AcademicTerm();
        term.setSchoolId(schoolId);
        term.setSession(session);
        term.setName(request.getName());
        if(request.getStartDate() != null) term.setStartDate(request.getStartDate());
        if(request.getEndDate() != null) term.setEndDate(request.getEndDate());
        if(request.getDisplayOrder() != null) term.setDisplayOrder(request.getDisplayOrder());
        
        term = repository.save(term);
        return mapToResponse(term);
    }

    public AcademicTermResponse update(UUID id, AcademicTermRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        
        return mapToResponse(repository.save(entity));
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");
        repository.delete(entity);
    }

    private AcademicTermResponse mapToResponse(AcademicTerm term) {
        AcademicTermResponse res = new AcademicTermResponse();
        res.setId(term.getId());
        res.setSessionId(term.getSession().getId());
        res.setSessionName(term.getSession().getName());
        res.setName(term.getName());
        res.setStartDate(term.getStartDate());
        res.setEndDate(term.getEndDate());
        res.setDisplayOrder(term.getDisplayOrder());
        return res;
    }
}