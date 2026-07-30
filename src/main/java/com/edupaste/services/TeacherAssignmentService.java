package com.edupaste.services;
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
}