const fs = require('fs');
const path = require('path');

const entities = [
    { name: 'AcademicTerm', var: 'academicTerm', route: 'academic-terms' },
    { name: 'SchoolClass', var: 'schoolClass', route: 'classes' },
    { name: 'Section', var: 'section', route: 'sections' },
    { name: 'Subject', var: 'subject', route: 'subjects' },
    { name: 'ClassSubject', var: 'classSubject', route: 'class-subjects' },
    { name: 'TeacherAssignment', var: 'teacherAssignment', route: 'teacher-assignments' },
    { name: 'StudentEnrollment', var: 'studentEnrollment', route: 'student-enrollments' },
];

const basePath = path.join(__dirname, 'src', 'main', 'java', 'com', 'edupaste');

entities.forEach(entity => {
    // Request DTO
    const requestCode = `package com.edupaste.payloads;

import lombok.Data;

@Data
public class ${entity.name}Request {
    // Scaffolded request properties
    private String status;
}
`;
    fs.writeFileSync(path.join(basePath, 'payloads', `${entity.name}Request.java`), requestCode);

    // Response DTO
    const responseCode = `package com.edupaste.payloads;

import lombok.Data;
import java.util.UUID;
import java.time.LocalDateTime;

@Data
public class ${entity.name}Response {
    private UUID id;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
`;
    fs.writeFileSync(path.join(basePath, 'payloads', `${entity.name}Response.java`), responseCode);

    // Service
    const serviceCode = `package com.edupaste.services;

import com.edupaste.models.${entity.name};
import com.edupaste.payloads.${entity.name}Request;
import com.edupaste.payloads.${entity.name}Response;
import com.edupaste.repositories.${entity.name}Repository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ${entity.name}Service {

    @Autowired
    private ${entity.name}Repository repository;

    // Scaffolded service methods
}
`;
    fs.writeFileSync(path.join(basePath, 'services', `${entity.name}Service.java`), serviceCode);

    // Controller
    const controllerCode = `package com.edupaste.controllers;

import com.edupaste.payloads.${entity.name}Request;
import com.edupaste.payloads.${entity.name}Response;
import com.edupaste.services.${entity.name}Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/${entity.route}")
public class ${entity.name}Controller {

    @Autowired
    private ${entity.name}Service service;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN', 'SCHOOL_ADMIN', 'TEACHER')")
    public ResponseEntity<?> getAll() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "${entity.name} API active");
        return ResponseEntity.ok(response);
    }
}
`;
    fs.writeFileSync(path.join(basePath, 'controllers', `${entity.name}Controller.java`), controllerCode);
});
console.log("APIs scaffolded.");
