const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'src/main/java/com/edupaste');

const processService = (filePath, entityVar, updateLogic) => {
    let content = fs.readFileSync(filePath, 'utf8');
    
    if (!content.includes('import java.util.UUID;')) {
        content = content.replace('import java.util.List;', 'import java.util.List;\nimport java.util.UUID;');
    }
    
    // Check if update is already implemented
    if (content.includes('public ' + entityVar + 'Response update(')) {
        return; // Already done
    }

    const mapMethodRegex = /private \w+Response mapToResponse\(/;
    const match = content.match(mapMethodRegex);
    
    if (match) {
        const updateStr = `
    public ${entityVar}Response update(UUID id, ${entityVar}Request request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        
${updateLogic}
        
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

`;
        content = content.slice(0, match.index) + updateStr + content.slice(match.index);
        fs.writeFileSync(filePath, content, 'utf8');
        console.log("Updated Service: " + filePath);
    }
};

const processController = (filePath, requestClass) => {
    let content = fs.readFileSync(filePath, 'utf8');
    
    if (!content.includes('import java.util.UUID;')) {
        content = content.replace('import java.util.Map;', 'import java.util.Map;\nimport java.util.UUID;');
    }
    
    if (content.includes('@PutMapping')) {
        return;
    }

    const classEndRegex = /}\s*$/;
    const match = content.match(classEndRegex);
    
    if (match) {
        const crudStr = `
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody ${requestClass} request) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", service.update(id, request));
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SCHOOL_ADMIN')")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        service.delete(id);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        return ResponseEntity.ok(response);
    }
`;
        content = content.slice(0, match.index) + crudStr + content.slice(match.index);
        fs.writeFileSync(filePath, content, 'utf8');
        console.log("Updated Controller: " + filePath);
    }
};

// 1. AcademicSession
processService(path.join(baseDir, 'services/AcademicSessionService.java'), 'AcademicSession', `
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getStartDate() != null) entity.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) entity.setEndDate(request.getEndDate());
`);
processController(path.join(baseDir, 'controllers/AcademicSessionController.java'), 'AcademicSessionRequest');

// 2. SchoolClass
processService(path.join(baseDir, 'services/SchoolClassService.java'), 'SchoolClass', `
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
`);
processController(path.join(baseDir, 'controllers/SchoolClassController.java'), 'SchoolClassRequest');

// 3. Subject
processService(path.join(baseDir, 'services/SubjectService.java'), 'Subject', `
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getCode() != null) entity.setSubjectCode(request.getCode());
        if (request.getDescription() != null) entity.setDescription(request.getDescription());
`);
processController(path.join(baseDir, 'controllers/SubjectController.java'), 'SubjectRequest');

// 4. Section
processService(path.join(baseDir, 'services/SectionService.java'), 'Section', `
        if (request.getName() != null) entity.setName(request.getName());
        if (request.getClassId() != null) {
            var sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new RuntimeException("Class not found"));
            entity.setSchoolClass(sc);
        }
`);
processController(path.join(baseDir, 'controllers/SectionController.java'), 'SectionRequest');

// 5. ClassSubject
processService(path.join(baseDir, 'services/ClassSubjectService.java'), 'ClassSubject', `
        if (request.getSectionId() != null) {
            var sec = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new RuntimeException("Section not found"));
            entity.setSection(sec);
        }
        if (request.getSubjectId() != null) {
            var sub = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new RuntimeException("Subject not found"));
            entity.setSubject(sub);
        }
`);
processController(path.join(baseDir, 'controllers/ClassSubjectController.java'), 'ClassSubjectRequest');
