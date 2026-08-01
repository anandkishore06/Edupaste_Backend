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
        
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Session name is required.");
        }

        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required.");
        }
        
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before end date.");
        }

        List<AcademicSession> existingSessions = repository.findBySchoolId(schoolId);
        
        // Name duplicate check
        boolean nameExists = existingSessions.stream()
                .anyMatch(s -> s.getName().trim().equalsIgnoreCase(trimmedName));
        if (nameExists) {
            throw new IllegalArgumentException("An academic session named '" + trimmedName + "' already exists.");
        }

        // Duration / Date overlap check
        for (AcademicSession s : existingSessions) {
            if (s.getStartDate() != null && s.getEndDate() != null) {
                boolean isOverlapping = !request.getStartDate().isAfter(s.getEndDate()) && !request.getEndDate().isBefore(s.getStartDate());
                if (isOverlapping) {
                    throw new IllegalArgumentException("An academic session for this duration already exists (" + s.getName() + ": " + s.getStartDate() + " to " + s.getEndDate() + ").");
                }
            }
        }
        
        boolean isFirst = existingSessions.isEmpty();
        boolean shouldBeCurrent = isFirst || (request.getIsCurrent() != null && request.getIsCurrent());
        
        if (shouldBeCurrent && !isFirst) {
            existingSessions.stream()
                .filter(AcademicSession::getIsCurrent)
                .forEach(s -> {
                    s.setIsCurrent(false);
                    repository.save(s);
                });
        }
        
        AcademicSession session = new AcademicSession();
        session.setSchoolId(schoolId);
        session.setName(trimmedName);
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
        
        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            boolean nameExists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(s -> !s.getId().equals(id) && s.getName().trim().equalsIgnoreCase(trimmedName));
            if (nameExists) {
                throw new IllegalArgumentException("An academic session named '" + trimmedName + "' already exists.");
            }
            entity.setName(trimmedName);
        }
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
        
        if (entity.getStartDate() != null && entity.getEndDate() != null) {
            if (entity.getStartDate().isAfter(entity.getEndDate())) {
                throw new IllegalArgumentException("Start date must be before end date.");
            }

            for (AcademicSession s : repository.findBySchoolId(schoolId)) {
                if (!s.getId().equals(id) && s.getStartDate() != null && s.getEndDate() != null) {
                    boolean isOverlapping = !entity.getStartDate().isAfter(s.getEndDate()) && !entity.getEndDate().isBefore(s.getStartDate());
                    if (isOverlapping) {
                        throw new IllegalArgumentException("An academic session for this duration already exists (" + s.getName() + ": " + s.getStartDate() + " to " + s.getEndDate() + ").");
                    }
                }
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

    @Transactional
    public AcademicSessionResponse activate(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AcademicSession target = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Academic session not found"));
                
        if (!target.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }

        // Deactivate all existing current sessions for this school
        repository.findBySchoolId(schoolId).stream()
            .filter(s -> Boolean.TRUE.equals(s.getIsCurrent()))
            .forEach(s -> {
                s.setIsCurrent(false);
                repository.save(s);
            });

        target.setIsCurrent(true);
        target = repository.save(target);
        return mapToResponse(target);
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
