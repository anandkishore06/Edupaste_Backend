package com.edupaste.services;
import com.edupaste.models.AcademicSession;
import com.edupaste.models.TeacherAssignment;
import com.edupaste.payloads.TeacherAssignmentRequest;
import com.edupaste.payloads.TeacherAssignmentResponse;
import com.edupaste.repositories.TeacherAssignmentRepository;
import com.edupaste.repositories.ClassSubjectRepository;
import com.edupaste.repositories.UserRepository;
import com.edupaste.repositories.AcademicSessionRepository;
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
    @Autowired private AcademicSessionRepository sessionRepository;

    public Page<TeacherAssignmentResponse> getAll(UUID sessionId, Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        if (sessionId != null) {
            return repository.findBySchoolIdAndAcademicSessionId(schoolId, sessionId, pageable).map(this::mapToResponse);
        }
        var activeSessionOpt = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId);
        if (activeSessionOpt.isPresent()) {
            return repository.findBySchoolIdAndAcademicSessionId(schoolId, activeSessionOpt.get().getId(), pageable).map(this::mapToResponse);
        }
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public TeacherAssignmentResponse create(TeacherAssignmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();

        var cs = csRepository.findById(request.getClassSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Class Subject not found"));
        var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));

        AcademicSession targetSession = null;
        if (request.getAcademicSessionId() != null) {
            targetSession = sessionRepository.findById(request.getAcademicSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Academic Session not found"));
        } else if (cs.getSection() != null && cs.getSection().getSchoolClass() != null && cs.getSection().getSchoolClass().getSession() != null) {
            targetSession = cs.getSection().getSchoolClass().getSession();
        } else {
            targetSession = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId).orElse(null);
        }

        if (targetSession != null) {
            final UUID targetSessId = targetSession.getId();
            boolean existsInSession = repository.findBySchoolIdAndTeacherId(schoolId, request.getTeacherId()).stream()
                    .anyMatch(ta -> ta.getClassSubject().getId().equals(request.getClassSubjectId()) && targetSessId.equals(
                        ta.getAcademicSession() != null ? ta.getAcademicSession().getId() :
                        (ta.getClassSubject().getSection() != null && ta.getClassSubject().getSection().getSchoolClass() != null && ta.getClassSubject().getSection().getSchoolClass().getSession() != null ? ta.getClassSubject().getSection().getSchoolClass().getSession().getId() : null)
                    ));
            if (existsInSession) {
                throw new IllegalArgumentException("This teacher is already assigned to this subject section for the selected academic session.");
            }
        }
        
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setSchoolId(schoolId);
        assignment.setTeacher(teacher);
        assignment.setClassSubject(cs);
        assignment.setAcademicSession(targetSession);
        
        return mapToResponse(repository.save(assignment));
    }

    public TeacherAssignmentResponse update(UUID id, TeacherAssignmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new IllegalArgumentException("Unauthorized");

        Long targetTeacherId = request.getTeacherId() != null ? request.getTeacherId() : entity.getTeacher().getId();
        UUID targetCsId = request.getClassSubjectId() != null ? request.getClassSubjectId() : entity.getClassSubject().getId();

        var cs = csRepository.findById(targetCsId)
                .orElseThrow(() -> new IllegalArgumentException("Class Subject not found"));

        AcademicSession targetSession = null;
        if (request.getAcademicSessionId() != null) {
            targetSession = sessionRepository.findById(request.getAcademicSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Academic Session not found"));
        } else if (entity.getAcademicSession() != null) {
            targetSession = entity.getAcademicSession();
        } else if (cs.getSection() != null && cs.getSection().getSchoolClass() != null && cs.getSection().getSchoolClass().getSession() != null) {
            targetSession = cs.getSection().getSchoolClass().getSession();
        }

        if (targetSession != null) {
            final UUID targetSessId = targetSession.getId();
            boolean existsInSession = repository.findBySchoolIdAndTeacherId(schoolId, targetTeacherId).stream()
                    .anyMatch(ta -> !ta.getId().equals(id) && ta.getClassSubject().getId().equals(targetCsId) && targetSessId.equals(
                        ta.getAcademicSession() != null ? ta.getAcademicSession().getId() :
                        (ta.getClassSubject().getSection() != null && ta.getClassSubject().getSection().getSchoolClass() != null && ta.getClassSubject().getSection().getSchoolClass().getSession() != null ? ta.getClassSubject().getSection().getSchoolClass().getSession().getId() : null)
                    ));
            if (existsInSession) {
                throw new IllegalArgumentException("This teacher is already assigned to this subject section for the selected academic session.");
            }
        }

        if (request.getTeacherId() != null) {
            var teacher = userRepository.findById(request.getTeacherId())
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
            entity.setTeacher(teacher);
        }
        entity.setClassSubject(cs);
        if (targetSession != null) {
            entity.setAcademicSession(targetSession);
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
        if (ta.getTeacher() != null) {
            res.setTeacherId(ta.getTeacher().getId());
            res.setTeacherName(ta.getTeacher().getFullName());
        }
        if (ta.getClassSubject() != null) {
            res.setClassSubjectId(ta.getClassSubject().getId());
            if (ta.getClassSubject().getSection() != null) {
                res.setSectionName(ta.getClassSubject().getSection().getName());
                if (ta.getClassSubject().getSection().getSchoolClass() != null) {
                    res.setClassName(ta.getClassSubject().getSection().getSchoolClass().getName());
                }
            }
            if (ta.getClassSubject().getSubject() != null) {
                res.setSubjectName(ta.getClassSubject().getSubject().getName());
                res.setSubjectType(ta.getClassSubject().getSubject().getType());
            }
        }

        if (ta.getAcademicSession() != null) {
            res.setAcademicSessionId(ta.getAcademicSession().getId());
            res.setAcademicSessionName(ta.getAcademicSession().getName());
        } else if (ta.getClassSubject() != null && ta.getClassSubject().getSection() != null &&
                   ta.getClassSubject().getSection().getSchoolClass() != null &&
                   ta.getClassSubject().getSection().getSchoolClass().getSession() != null) {
            var sess = ta.getClassSubject().getSection().getSchoolClass().getSession();
            res.setAcademicSessionId(sess.getId());
            res.setAcademicSessionName(sess.getName());
        }

        return res;
    }
}