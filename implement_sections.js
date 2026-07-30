const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'src/main/java/com/edupaste');

const files = {
    // Payloads
    'payloads/SectionRequest.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SectionRequest {
    private UUID classId;
    private String name;
}`,
    'payloads/SectionResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SectionResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private String name;
}`,
    'payloads/ClassSubjectRequest.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class ClassSubjectRequest {
    private UUID classId;
    private UUID subjectId;
}`,
    'payloads/ClassSubjectResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class ClassSubjectResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private UUID subjectId;
    private String subjectName;
}`,

    // Services
    'services/SectionService.java': `package com.edupaste.services;

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
import java.util.stream.Collectors;

@Service
public class SectionService {

    @Autowired
    private SectionRepository repository;

    @Autowired
    private SchoolClassRepository classRepository;

    public List<SectionResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SectionResponse create(SectionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
                
        Section section = new Section();
        section.setSchoolId(schoolId);
        section.setSchoolClass(sc);
        section.setName(request.getName());
        
        section = repository.save(section);
        return mapToResponse(section);
    }

    private SectionResponse mapToResponse(Section sec) {
        SectionResponse res = new SectionResponse();
        res.setId(sec.getId());
        res.setClassId(sec.getSchoolClass().getId());
        res.setClassName(sec.getSchoolClass().getName());
        res.setName(sec.getName());
        return res;
    }
}
`,

    'services/ClassSubjectService.java': `package com.edupaste.services;

import com.edupaste.models.ClassSubject;
import com.edupaste.models.SchoolClass;
import com.edupaste.models.Subject;
import com.edupaste.payloads.ClassSubjectRequest;
import com.edupaste.payloads.ClassSubjectResponse;
import com.edupaste.repositories.ClassSubjectRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.repositories.SubjectRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClassSubjectService {

    @Autowired
    private ClassSubjectRepository repository;

    @Autowired
    private SchoolClassRepository classRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;

    public List<ClassSubjectResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public ClassSubjectResponse create(ClassSubjectRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
        Subject sub = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
                
        ClassSubject cs = new ClassSubject();
        cs.setSchoolId(schoolId);
        cs.setSchoolClass(sc);
        cs.setSubject(sub);
        
        cs = repository.save(cs);
        return mapToResponse(cs);
    }

    private ClassSubjectResponse mapToResponse(ClassSubject cs) {
        ClassSubjectResponse res = new ClassSubjectResponse();
        res.setId(cs.getId());
        res.setClassId(cs.getSchoolClass().getId());
        res.setClassName(cs.getSchoolClass().getName());
        res.setSubjectId(cs.getSubject().getId());
        res.setSubjectName(cs.getSubject().getName());
        return res;
    }
}
`,

    // Controllers
    'controllers/SectionController.java': `package com.edupaste.controllers;

import com.edupaste.payloads.SectionRequest;
import com.edupaste.services.SectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/sections")
public class SectionController {

    @Autowired
    private SectionService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getAll());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody SectionRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }
}
`,

    'controllers/ClassSubjectController.java': `package com.edupaste.controllers;

import com.edupaste.payloads.ClassSubjectRequest;
import com.edupaste.services.ClassSubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/class-subjects")
public class ClassSubjectController {

    @Autowired
    private ClassSubjectService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER', 'STUDENT', 'PARENT')")
    public ResponseEntity<?> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.getAll());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody ClassSubjectRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }
}
`,

    // Additional repositories
    'repositories/SectionRepository.java': `package com.edupaste.repositories;

import com.edupaste.models.Section;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SectionRepository extends JpaRepository<Section, UUID> {
    List<Section> findBySchoolId(Long schoolId);
}
`,
    'repositories/ClassSubjectRepository.java': `package com.edupaste.repositories;

import com.edupaste.models.ClassSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClassSubjectRepository extends JpaRepository<ClassSubject, UUID> {
    List<ClassSubject> findBySchoolId(Long schoolId);
}
`
};

for (const [relPath, content] of Object.entries(files)) {
    const fullPath = path.join(baseDir, relPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, content, 'utf8');
    console.log('Written:', relPath);
}
