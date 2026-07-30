package com.edupaste.services;

import com.edupaste.models.SchoolClass;
import com.edupaste.models.Section;
import com.edupaste.payloads.SectionRequest;
import com.edupaste.payloads.SectionResponse;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.repositories.SectionRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SectionService {

    @Autowired
    private SectionRepository repository;

    @Autowired
    private SchoolClassRepository classRepository;

    public Page<SectionResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public List<SectionResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SectionResponse create(SectionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
                
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(s -> s.getSchoolClass().getId().equals(sc.getId()) && s.getName().equalsIgnoreCase(request.getName()));
        if (exists) {
            throw new RuntimeException("A section with this name already exists in the selected class.");
        }

        Section section = new Section();
        section.setSchoolId(schoolId);
        section.setSchoolClass(sc);
        section.setName(request.getName());
        if (request.getCapacity() != null) section.setCapacity(request.getCapacity());
        if (request.getRoom() != null) section.setRoom(request.getRoom());
        
        section = repository.save(section);
        return mapToResponse(section);
    }

    
    public SectionResponse update(UUID id, SectionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getName() != null) {
            final java.util.UUID currentClassId = entity.getSchoolClass().getId();
            boolean exists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(s -> !s.getId().equals(id) && s.getSchoolClass().getId().equals(currentClassId) && s.getName().equalsIgnoreCase(request.getName()));
            if (exists) {
                throw new RuntimeException("A section with this name already exists in the selected class.");
            }
            entity.setName(request.getName());
        }
        if (request.getClassId() != null) {
            var sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
            entity.setSchoolClass(sc);
        }
        if (request.getCapacity() != null) entity.setCapacity(request.getCapacity());
        if (request.getRoom() != null) entity.setRoom(request.getRoom());

        
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

private SectionResponse mapToResponse(Section sec) {
        SectionResponse res = new SectionResponse();
        res.setId(sec.getId());
        res.setClassId(sec.getSchoolClass().getId());
        res.setClassName(sec.getSchoolClass().getName());
        res.setName(sec.getName());
        res.setCapacity(sec.getCapacity());
        res.setRoom(sec.getRoom());
        return res;
    }
}
