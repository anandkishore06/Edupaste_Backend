package com.edupaste.services;

import com.edupaste.models.ClassSubject;
import com.edupaste.models.Section;
import com.edupaste.models.Subject;
import com.edupaste.payloads.ClassSubjectRequest;
import com.edupaste.payloads.ClassSubjectResponse;
import com.edupaste.repositories.ClassSubjectRepository;
import com.edupaste.repositories.SectionRepository;
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
public class ClassSubjectService {

    @Autowired
    private ClassSubjectRepository repository;

    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;

    public Page<ClassSubjectResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public List<ClassSubjectResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ClassSubjectResponse create(ClassSubjectRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Section sec = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found"));
        Subject sub = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
                
        ClassSubject cs = new ClassSubject();
        cs.setSchoolId(schoolId);
        cs.setSection(sec);
        cs.setSubject(sub);
        if (request.getWeeklyPeriods() != null) cs.setWeeklyPeriods(request.getWeeklyPeriods());
        if (request.getIsMandatory() != null) cs.setIsMandatory(request.getIsMandatory());
        
        // Validation: duplicate mapping
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(c -> c.getSection().getId().equals(sec.getId()) && c.getSubject().getId().equals(sub.getId()));
        if (exists) {
            throw new IllegalArgumentException("Subject '" + sub.getName() + "' is already assigned to section '" + sec.getName() + "'.");
        }
        
        cs = repository.save(cs);
        return mapToResponse(cs);
    }

    
    public ClassSubjectResponse update(UUID id, ClassSubjectRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getSectionId() != null) {
            var sec = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
            entity.setSection(sec);
        }
        if (request.getWeeklyPeriods() != null) entity.setWeeklyPeriods(request.getWeeklyPeriods());
        if (request.getIsMandatory() != null) entity.setIsMandatory(request.getIsMandatory());

        if (request.getSubjectId() != null) {
            var sub = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
            entity.setSubject(sub);
        }
        
        // Duplicate check on update
        if (request.getSectionId() != null || request.getSubjectId() != null) {
            final java.util.UUID currentSectionId = entity.getSection().getId();
            final java.util.UUID currentSubjectId = entity.getSubject().getId();
            boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(c -> !c.getId().equals(id) && c.getSection().getId().equals(currentSectionId) && c.getSubject().getId().equals(currentSubjectId));
            if (exists) {
                throw new IllegalArgumentException("Subject '" + entity.getSubject().getName() + "' is already assigned to section '" + entity.getSection().getName() + "'.");
            }
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

    private ClassSubjectResponse mapToResponse(ClassSubject cs) {
        ClassSubjectResponse res = new ClassSubjectResponse();
        res.setId(cs.getId());
        res.setSectionId(cs.getSection().getId());
        res.setSectionName(cs.getSection().getName());
        if (cs.getSection().getSchoolClass() != null) {
            res.setClassName(cs.getSection().getSchoolClass().getName());
        }
        if (cs.getSubject() != null) {
            res.setSubjectId(cs.getSubject().getId());
            res.setSubjectName(cs.getSubject().getName());
            res.setSubjectCode(cs.getSubject().getSubjectCode());
            String type = cs.getSubject().getType();
            res.setSubjectType(type != null && !type.isBlank() ? type : "THEORY");
        }
        res.setWeeklyPeriods(cs.getWeeklyPeriods());
        res.setIsMandatory(cs.getIsMandatory());
        return res;
    }
}
