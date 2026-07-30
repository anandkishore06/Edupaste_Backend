const fs = require('fs');
const path = require('path');
const be = 'C:/Users/AMAN KUMAR/Desktop/Edupaste/Edupaste_Backend/src/main/java/com/edupaste';

function updateTerm() {
// Service
fs.writeFileSync(path.join(be, 'services', 'AcademicTermService.java'), `package com.edupaste.services;

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
}`);

// Controller
fs.writeFileSync(path.join(be, 'controllers', 'AcademicTermController.java'), `package com.edupaste.controllers;
import com.edupaste.payloads.AcademicTermRequest;
import com.edupaste.payloads.AcademicTermResponse;
import com.edupaste.payloads.PagedResponse;
import com.edupaste.services.AcademicTermService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/academic-terms")
public class AcademicTermController {
    @Autowired
    private AcademicTermService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<AcademicTermResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(new PagedResponse<>(
                pagedData.getContent(), page, limit, pagedData.getTotalElements(), pagedData.getTotalPages()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody AcademicTermRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody AcademicTermRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }
}`);
}

function updateTeacherAssignment() {
fs.writeFileSync(path.join(be, 'services', 'TeacherAssignmentService.java'), `package com.edupaste.services;
import com.edupaste.models.TeacherAssignment;
import com.edupaste.payloads.TeacherAssignmentRequest;
import com.edupaste.payloads.TeacherAssignmentResponse;
import com.edupaste.repositories.TeacherAssignmentRepository;
import com.edupaste.repositories.ClassSubjectRepository;
import com.edupaste.repositories.UserRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
public class TeacherAssignmentService {
    @Autowired private TeacherAssignmentRepository repository;
    @Autowired private ClassSubjectRepository csRepository;
    @Autowired private UserRepository userRepository;

    public Page<TeacherAssignmentResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public TeacherAssignmentResponse create(TeacherAssignmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var cs = csRepository.findById(request.getClassSubjectId())
                .orElseThrow(() -> new RuntimeException("Class Subject not found"));
        var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacher(teacher);
        assignment.setClassSubject(cs);
        
        return mapToResponse(repository.save(assignment));
    }

    public TeacherAssignmentResponse update(UUID id, TeacherAssignmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");

        if (request.getTeacherId() != null) {
            var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new RuntimeException("Teacher not found"));
            entity.setTeacher(teacher);
        }
        
        return mapToResponse(repository.save(entity));
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");
        repository.delete(entity);
    }

    private TeacherAssignmentResponse mapToResponse(TeacherAssignment ta) {
        TeacherAssignmentResponse res = new TeacherAssignmentResponse();
        res.setId(ta.getId());
        res.setTeacherId(ta.getTeacher().getId());
        res.setClassSubjectId(ta.getClassSubject().getId());
        return res;
    }
}`);

// Controller
fs.writeFileSync(path.join(be, 'controllers', 'TeacherAssignmentController.java'), `package com.edupaste.controllers;
import com.edupaste.payloads.TeacherAssignmentRequest;
import com.edupaste.payloads.TeacherAssignmentResponse;
import com.edupaste.payloads.PagedResponse;
import com.edupaste.services.TeacherAssignmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/teacher-assignments")
public class TeacherAssignmentController {
    @Autowired
    private TeacherAssignmentService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<TeacherAssignmentResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(new PagedResponse<>(
                pagedData.getContent(), page, limit, pagedData.getTotalElements(), pagedData.getTotalPages()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody TeacherAssignmentRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody TeacherAssignmentRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }
}`);
}

function updateStudentEnrollment() {
fs.writeFileSync(path.join(be, 'services', 'StudentEnrollmentService.java'), `package com.edupaste.services;
import com.edupaste.models.StudentEnrollment;
import com.edupaste.payloads.StudentEnrollmentRequest;
import com.edupaste.payloads.StudentEnrollmentResponse;
import com.edupaste.repositories.StudentEnrollmentRepository;
import com.edupaste.repositories.SectionRepository;
import com.edupaste.repositories.UserRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
public class StudentEnrollmentService {
    @Autowired private StudentEnrollmentRepository repository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private UserRepository userRepository;

    public Page<StudentEnrollmentResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public StudentEnrollmentResponse create(StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found"));
        var student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
        
        StudentEnrollment enr = new StudentEnrollment();
        enr.setSchoolId(schoolId);
        enr.setStudent(student);
        enr.setSection(section);
        if(request.getEnrollmentDate() != null) enr.setEnrollmentDate(request.getEnrollmentDate());
        if(request.getRollNumber() != null) enr.setRollNumber(request.getRollNumber());
        
        return mapToResponse(repository.save(enr));
    }

    public StudentEnrollmentResponse update(UUID id, StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");

        if (request.getStudentId() != null) {
             var student = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new RuntimeException("Student not found"));
             entity.setStudent(student);
        }
        if (request.getEnrollmentDate() != null) entity.setEnrollmentDate(request.getEnrollmentDate());
        if (request.getRollNumber() != null) entity.setRollNumber(request.getRollNumber());
        
        return mapToResponse(repository.save(entity));
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");
        repository.delete(entity);
    }

    private StudentEnrollmentResponse mapToResponse(StudentEnrollment se) {
        StudentEnrollmentResponse res = new StudentEnrollmentResponse();
        res.setId(se.getId());
        res.setStudentId(se.getStudent().getId());
        res.setSectionId(se.getSection().getId());
        res.setEnrollmentDate(se.getEnrollmentDate());
        res.setRollNumber(se.getRollNumber());
        return res;
    }
}`);

// Controller
fs.writeFileSync(path.join(be, 'controllers', 'StudentEnrollmentController.java'), `package com.edupaste.controllers;
import com.edupaste.payloads.StudentEnrollmentRequest;
import com.edupaste.payloads.StudentEnrollmentResponse;
import com.edupaste.payloads.PagedResponse;
import com.edupaste.services.StudentEnrollmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@RestController
@RequestMapping("/api/v1/student-enrollments")
public class StudentEnrollmentController {
    @Autowired
    private StudentEnrollmentService service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit
    ) {
        Page<StudentEnrollmentResponse> pagedData = service.getAll(PageRequest.of(page - 1, limit));
        return ResponseEntity.ok(new PagedResponse<>(
                pagedData.getContent(), page, limit, pagedData.getTotalElements(), pagedData.getTotalPages()
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> create(@RequestBody StudentEnrollmentRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.create(request));
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody StudentEnrollmentRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Deleted successfully");
        return ResponseEntity.ok(response);
    }
}`);
}

updateTerm();
updateTeacherAssignment();
updateStudentEnrollment();
console.log("Updated controllers and services");
