const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'src/main/java/com/edupaste');

const files = {
    // Payloads
    'payloads/AcademicSessionRequest.java': `package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
@Data
public class AcademicSessionRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
}`,
    'payloads/AcademicSessionResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;
@Data
public class AcademicSessionResponse {
    private UUID id;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}`,
    'payloads/SchoolClassRequest.java': `package com.edupaste.payloads;
import lombok.Data;
@Data
public class SchoolClassRequest {
    private String name;
    private String description;
}`,
    'payloads/SchoolClassResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SchoolClassResponse {
    private UUID id;
    private String name;
    private String description;
}`,
    'payloads/SubjectRequest.java': `package com.edupaste.payloads;
import lombok.Data;
@Data
public class SubjectRequest {
    private String name;
    private String code;
    private String description;
}`,
    'payloads/SubjectResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SubjectResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
}`,

    // Services
    'services/AcademicSessionService.java': `package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.payloads.AcademicSessionRequest;
import com.edupaste.payloads.AcademicSessionResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AcademicSessionService {

    @Autowired
    private AcademicSessionRepository repository;

    public List<AcademicSessionResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public AcademicSessionResponse create(AcademicSessionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        
        // If this is the first session, make it current. Otherwise not current by default (simplified logic).
        boolean isFirst = repository.findBySchoolId(schoolId).isEmpty();
        
        AcademicSession session = new AcademicSession();
        session.setSchoolId(schoolId);
        session.setName(request.getName());
        session.setStartDate(request.getStartDate());
        session.setEndDate(request.getEndDate());
        session.setIsCurrent(isFirst);
        
        session = repository.save(session);
        return mapToResponse(session);
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
`,

    'services/SchoolClassService.java': `package com.edupaste.services;

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
import java.util.stream.Collectors;

@Service
public class SchoolClassService {

    @Autowired
    private SchoolClassRepository repository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

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
        
        sc = repository.save(sc);
        return mapToResponse(sc);
    }

    private SchoolClassResponse mapToResponse(SchoolClass sc) {
        SchoolClassResponse res = new SchoolClassResponse();
        res.setId(sc.getId());
        res.setName(sc.getName());
        res.setDescription(sc.getDescription());
        return res;
    }
}
`,

    'services/SubjectService.java': `package com.edupaste.services;

import com.edupaste.models.Subject;
import com.edupaste.payloads.SubjectRequest;
import com.edupaste.payloads.SubjectResponse;
import com.edupaste.repositories.SubjectRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubjectService {

    @Autowired
    private SubjectRepository repository;

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
        
        sub = repository.save(sub);
        return mapToResponse(sub);
    }

    private SubjectResponse mapToResponse(Subject sub) {
        SubjectResponse res = new SubjectResponse();
        res.setId(sub.getId());
        res.setName(sub.getName());
        res.setCode(sub.getSubjectCode());
        res.setDescription(sub.getDescription());
        return res;
    }
}
`,

    // Controllers
    'controllers/AcademicSessionController.java': `package com.edupaste.controllers;

import com.edupaste.payloads.AcademicSessionRequest;
import com.edupaste.services.AcademicSessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/academic-sessions")
public class AcademicSessionController {

    @Autowired
    private AcademicSessionService service;

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
    public ResponseEntity<?> create(@RequestBody AcademicSessionRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }
}
`,

    'controllers/SchoolClassController.java': `package com.edupaste.controllers;

import com.edupaste.payloads.SchoolClassRequest;
import com.edupaste.services.SchoolClassService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/classes")
public class SchoolClassController {

    @Autowired
    private SchoolClassService service;

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
    public ResponseEntity<?> create(@RequestBody SchoolClassRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }
}
`,

    'controllers/SubjectController.java': `package com.edupaste.controllers;

import com.edupaste.payloads.SubjectRequest;
import com.edupaste.services.SubjectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/subjects")
public class SubjectController {

    @Autowired
    private SubjectService service;

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
    public ResponseEntity<?> create(@RequestBody SubjectRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }
}
`,

    // Additional repositories
    'repositories/SchoolClassRepository.java': `package com.edupaste.repositories;

import com.edupaste.models.SchoolClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {
    List<SchoolClass> findBySchoolId(Long schoolId);
}
`,
    'repositories/SubjectRepository.java': `package com.edupaste.repositories;

import com.edupaste.models.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, UUID> {
    List<Subject> findBySchoolId(Long schoolId);
}
`
};

for (const [relPath, content] of Object.entries(files)) {
    const fullPath = path.join(baseDir, relPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, content, 'utf8');
    console.log('Written:', relPath);
}
