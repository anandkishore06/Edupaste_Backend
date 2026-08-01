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
        
        var session = (request.getSessionId() != null)
                ? sessionRepository.findById(request.getSessionId())
                        .orElseThrow(() -> new IllegalArgumentException("Selected session not found."))
                : sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                        .orElseThrow(() -> new IllegalArgumentException("No active academic session found for this school."));
        
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Term name is required.");
        }

        if (request.getStartDate() != null && request.getEndDate() != null && request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date.");
        }

        if (session.getStartDate() != null && session.getEndDate() != null) {
            if ((request.getStartDate() != null && request.getStartDate().isBefore(session.getStartDate())) ||
                (request.getEndDate() != null && request.getEndDate().isAfter(session.getEndDate()))) {
                throw new IllegalArgumentException("Term dates must fall within academic session '" + session.getName() + "' duration (" + session.getStartDate() + " to " + session.getEndDate() + ").");
            }
        }

        boolean nameExists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(t -> t.getSession().getId().equals(session.getId()) && t.getName().trim().equalsIgnoreCase(trimmedName));
        if (nameExists) {
            throw new IllegalArgumentException("A term named '" + trimmedName + "' already exists in session '" + session.getName() + "'.");
        }

        AcademicTerm term = new AcademicTerm();
        term.setSchoolId(schoolId);
        term.setSession(session);
        term.setName(trimmedName);
        if (request.getStartDate() != null) term.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) term.setEndDate(request.getEndDate());
        if (request.getDisplayOrder() != null) term.setDisplayOrder(request.getDisplayOrder());
        
        term = repository.save(term);
        return mapToResponse(term);
    }

    public AcademicTermResponse update(UUID id, AcademicTermRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");

        if (request.getSessionId() != null) {
            var session = sessionRepository.findById(request.getSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
            entity.setSession(session);
        }

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            final UUID currentSessionId = entity.getSession().getId();
            boolean nameExists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(t -> !t.getId().equals(id) && t.getSession().getId().equals(currentSessionId) && t.getName().trim().equalsIgnoreCase(trimmedName));
            if (nameExists) {
                throw new IllegalArgumentException("A term named '" + trimmedName + "' already exists in session '" + entity.getSession().getName() + "'.");
            }
            entity.setName(trimmedName);
        }
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());
        
        if (entity.getStartDate() != null && entity.getEndDate() != null && entity.getStartDate().isAfter(entity.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date.");
        }

        var currentSession = entity.getSession();
        if (currentSession != null && currentSession.getStartDate() != null && currentSession.getEndDate() != null) {
            if ((entity.getStartDate() != null && entity.getStartDate().isBefore(currentSession.getStartDate())) ||
                (entity.getEndDate() != null && entity.getEndDate().isAfter(currentSession.getEndDate()))) {
                throw new IllegalArgumentException("Term dates must fall within academic session '" + currentSession.getName() + "' duration (" + currentSession.getStartDate() + " to " + currentSession.getEndDate() + ").");
            }
        }
        
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