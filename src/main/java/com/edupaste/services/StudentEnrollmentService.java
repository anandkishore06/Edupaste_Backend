package com.edupaste.services;
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
}