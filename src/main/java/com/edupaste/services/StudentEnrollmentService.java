package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.StudentEnrollment;
import com.edupaste.payloads.StudentEnrollmentRequest;
import com.edupaste.payloads.StudentEnrollmentResponse;
import com.edupaste.repositories.StudentEnrollmentRepository;
import com.edupaste.repositories.StudentRepository;
import com.edupaste.repositories.SectionRepository;
import com.edupaste.repositories.UserRepository;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.models.SchoolClass;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.util.StringUtils;
import java.util.UUID;

@Service
@Transactional
public class StudentEnrollmentService {
    @Autowired private StudentEnrollmentRepository repository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AcademicSessionRepository sessionRepository;
    @Autowired private SchoolClassRepository classRepository;

    public Page<StudentEnrollmentResponse> getAll(UUID sessionId, Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        if (schoolId == null) {
            if (sessionId != null) {
                return repository.findByAcademicSessionId(sessionId, pageable).map(this::mapToResponse);
            }
            return repository.findAll(pageable).map(this::mapToResponse);
        }
        if (sessionId != null) {
            return repository.findBySchoolIdAndAcademicSessionId(schoolId, sessionId, pageable).map(this::mapToResponse);
        }
        return repository.findBySchoolId(schoolId, pageable).map(this::mapToResponse);
    }

    public StudentEnrollmentResponse create(StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();

        var section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        var student = studentRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        AcademicSession targetSession = null;
        if (request.getAcademicSessionId() != null) {
            targetSession = sessionRepository.findById(request.getAcademicSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Academic Session not found"));
        } else if (section.getSchoolClass() != null && section.getSchoolClass().getSession() != null) {
            targetSession = section.getSchoolClass().getSession();
        } else {
            targetSession = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId).orElse(null);
        }

        SchoolClass classEntity = null;
        if (request.getClassId() != null) {
            classEntity = classRepository.findById(request.getClassId())
                    .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        } else if (section.getSchoolClass() != null) {
            classEntity = section.getSchoolClass();
        }

        if (targetSession != null) {
            boolean existsInSession = repository.existsBySchoolIdAndStudentIdAndAcademicSessionId(schoolId, student.getId(), targetSession.getId());
            if (existsInSession) {
                throw new IllegalArgumentException("This student is already enrolled in a section for the selected academic session.");
            }
        }

        if (StringUtils.hasText(request.getRollNumber()) && targetSession != null && classEntity != null) {
            boolean rollExists = repository.existsBySchoolIdAndAcademicSessionIdAndSchoolClassIdAndSectionIdAndRollNumber(
                schoolId, targetSession.getId(), classEntity.getId(), section.getId(), request.getRollNumber().trim()
            );
            if (rollExists) {
                throw new IllegalArgumentException("Roll number '" + request.getRollNumber() + "' is already assigned in this section.");
            }
        }

        StudentEnrollment enr = new StudentEnrollment();
        enr.setSchoolId(schoolId);
        enr.setStudent(student);
        enr.setSection(section);
        enr.setSchoolClass(classEntity);
        enr.setAcademicSession(targetSession);
        enr.setEnrollmentDate(request.getEnrollmentDate() != null ? request.getEnrollmentDate() : java.time.LocalDate.now());
        if (request.getRollNumber() != null) enr.setRollNumber(request.getRollNumber());
        enr.setEnrollmentStatus(StringUtils.hasText(request.getEnrollmentStatus()) ? request.getEnrollmentStatus() : "ACTIVE");

        StudentEnrollment saved = repository.save(enr);
        return mapToResponse(saved);
    }

    public StudentEnrollmentResponse update(UUID id, StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new IllegalArgumentException("Unauthorized");

        UUID targetStudentId = request.getStudentId() != null ? request.getStudentId() : entity.getStudent().getId();
        var student = studentRepository.findById(targetStudentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));

        UUID targetSectionId = request.getSectionId() != null ? request.getSectionId() : entity.getSection().getId();
        var section = sectionRepository.findById(targetSectionId)
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));

        AcademicSession targetSession = null;
        if (request.getAcademicSessionId() != null) {
            targetSession = sessionRepository.findById(request.getAcademicSessionId())
                    .orElseThrow(() -> new IllegalArgumentException("Academic Session not found"));
        } else if (entity.getAcademicSession() != null) {
            targetSession = entity.getAcademicSession();
        } else if (section.getSchoolClass() != null && section.getSchoolClass().getSession() != null) {
            targetSession = section.getSchoolClass().getSession();
        }

        SchoolClass classEntity = null;
        UUID targetClassId = request.getClassId() != null ? request.getClassId() : (entity.getSchoolClass() != null ? entity.getSchoolClass().getId() : null);
        if (targetClassId != null) {
            classEntity = classRepository.findById(targetClassId)
                    .orElseThrow(() -> new IllegalArgumentException("Class not found"));
        } else if (section.getSchoolClass() != null) {
            classEntity = section.getSchoolClass();
        }

        if (targetSession != null) {
            boolean existsInSession = repository.existsBySchoolIdAndStudentIdAndAcademicSessionIdAndIdNot(schoolId, student.getId(), targetSession.getId(), id);
            if (existsInSession) {
                throw new IllegalArgumentException("This student is already enrolled in a section for the selected academic session.");
            }
        }

        if (StringUtils.hasText(request.getRollNumber()) && targetSession != null && classEntity != null) {
            boolean rollExists = repository.existsBySchoolIdAndAcademicSessionIdAndSchoolClassIdAndSectionIdAndRollNumberAndIdNot(
                schoolId, targetSession.getId(), classEntity.getId(), section.getId(), request.getRollNumber().trim(), id
            );
            if (rollExists) {
                throw new IllegalArgumentException("Roll number '" + request.getRollNumber() + "' is already assigned in this section.");
            }
        }

        entity.setStudent(student);
        entity.setSection(section);
        entity.setSchoolClass(classEntity);
        if (targetSession != null) {
            entity.setAcademicSession(targetSession);
        }
        if (request.getEnrollmentDate() != null) entity.setEnrollmentDate(request.getEnrollmentDate());
        if (request.getRollNumber() != null) entity.setRollNumber(request.getRollNumber());
        if (request.getEnrollmentStatus() != null) entity.setEnrollmentStatus(request.getEnrollmentStatus());

        StudentEnrollment saved = repository.save(entity);
        return mapToResponse(saved);
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
        if (se.getStudent() != null) {
            res.setStudentId(se.getStudent().getId());
            res.setStudentName(se.getStudent().getFullName());
        }
        if (se.getSchoolClass() != null) {
            res.setClassId(se.getSchoolClass().getId());
            res.setClassName(se.getSchoolClass().getName());
        }
        if (se.getSection() != null) {
            res.setSectionId(se.getSection().getId());
            res.setSectionName(se.getSection().getName());
            if (se.getSchoolClass() == null && se.getSection().getSchoolClass() != null) {
                res.setClassId(se.getSection().getSchoolClass().getId());
                res.setClassName(se.getSection().getSchoolClass().getName());
            }
        }
        res.setEnrollmentDate(se.getEnrollmentDate());
        res.setRollNumber(se.getRollNumber());
        res.setEnrollmentStatus(se.getEnrollmentStatus());

        if (se.getAcademicSession() != null) {
            res.setAcademicSessionId(se.getAcademicSession().getId());
            res.setAcademicSessionName(se.getAcademicSession().getName());
        } else if (se.getSection() != null && se.getSection().getSchoolClass() != null &&
                   se.getSection().getSchoolClass().getSession() != null) {
            var sess = se.getSection().getSchoolClass().getSession();
            res.setAcademicSessionId(sess.getId());
            res.setAcademicSessionName(sess.getName());
        }

        return res;
    }
}