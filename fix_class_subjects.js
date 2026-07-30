const fs = require('fs');
const path = require('path');

const baseDir = path.join(__dirname, 'src/main/java/com/edupaste');

const files = {
    'payloads/ClassSubjectRequest.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class ClassSubjectRequest {
    private UUID sectionId;
    private UUID subjectId;
}`,
    'payloads/ClassSubjectResponse.java': `package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class ClassSubjectResponse {
    private UUID id;
    private UUID sectionId;
    private String sectionName;
    private String className;
    private UUID subjectId;
    private String subjectName;
}`,
    'services/ClassSubjectService.java': `package com.edupaste.services;

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
import java.util.stream.Collectors;

@Service
public class ClassSubjectService {

    @Autowired
    private ClassSubjectRepository repository;

    @Autowired
    private SectionRepository sectionRepository;
    
    @Autowired
    private SubjectRepository subjectRepository;

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
        
        cs = repository.save(cs);
        return mapToResponse(cs);
    }

    private ClassSubjectResponse mapToResponse(ClassSubject cs) {
        ClassSubjectResponse res = new ClassSubjectResponse();
        res.setId(cs.getId());
        res.setSectionId(cs.getSection().getId());
        res.setSectionName(cs.getSection().getName());
        res.setClassName(cs.getSection().getSchoolClass().getName());
        res.setSubjectId(cs.getSubject().getId());
        res.setSubjectName(cs.getSubject().getName());
        return res;
    }
}
`
};

for (const [relPath, content] of Object.entries(files)) {
    const fullPath = path.join(baseDir, relPath);
    fs.mkdirSync(path.dirname(fullPath), { recursive: true });
    fs.writeFileSync(fullPath, content, 'utf8');
    console.log('Written:', relPath);
}
