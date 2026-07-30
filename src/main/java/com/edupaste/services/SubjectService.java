package com.edupaste.services;

import com.edupaste.models.Subject;
import com.edupaste.payloads.SubjectRequest;
import com.edupaste.payloads.SubjectResponse;
import com.edupaste.repositories.SubjectRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository repository;

    public Page<SubjectResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public List<SubjectResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SubjectResponse create(SubjectRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
                
        Subject sub = new Subject();
        sub.setSchoolId(schoolId);
        sub.setName(request.getName());
        sub.setSubjectCode(request.getCode());
        sub.setDescription(request.getDescription());
        if (request.getType() != null) sub.setType(request.getType());
        if (request.getIsElective() != null) sub.setIsElective(request.getIsElective());
        if (request.getIsLanguage() != null) sub.setIsLanguage(request.getIsLanguage());
        if (request.getCredits() != null) sub.setCredits(request.getCredits());
        
        // Validation: Duplicate subject code
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(s -> s.getSubjectCode().equalsIgnoreCase(request.getCode()));
        if (exists) {
            throw new RuntimeException("A subject with this code already exists.");
        }
        
        sub = repository.save(sub);
        return mapToResponse(sub);
    }

    
    public SubjectResponse update(UUID id, SubjectRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getName() != null) entity.setName(request.getName());
        if (request.getCode() != null) {
            boolean exists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(s -> !s.getId().equals(id) && s.getSubjectCode().equalsIgnoreCase(request.getCode()));
            if (exists) {
                throw new RuntimeException("A subject with this code already exists.");
            }
            entity.setSubjectCode(request.getCode());
        }
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
        if (request.getType() != null) entity.setType(request.getType());
        if (request.getIsElective() != null) entity.setIsElective(request.getIsElective());
        if (request.getIsLanguage() != null) entity.setIsLanguage(request.getIsLanguage());
        if (request.getCredits() != null) entity.setCredits(request.getCredits());

        
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

private SubjectResponse mapToResponse(Subject sub) {
        SubjectResponse res = new SubjectResponse();
        res.setId(sub.getId());
        res.setName(sub.getName());
        res.setCode(sub.getSubjectCode());
        res.setDescription(sub.getDescription());
        res.setType(sub.getType());
        res.setIsElective(sub.getIsElective());
        res.setIsLanguage(sub.getIsLanguage());
        res.setCredits(sub.getCredits());
        return res;
    }
}
