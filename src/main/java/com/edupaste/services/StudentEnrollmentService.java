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
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

@Service
@Transactional
public class StudentEnrollmentService {
    @Autowired private StudentEnrollmentRepository repository;
    @Autowired private SectionRepository sectionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private StudentRepository studentRepository;
    @Autowired private AcademicSessionRepository sessionRepository;

    public Page<StudentEnrollmentResponse> getAll(UUID sessionId, Pageable pageable) {
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

    public StudentEnrollmentResponse create(StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();

        var section = sectionRepository.findById(request.getSectionId())
                .orElseThrow(() -> new IllegalArgumentException("Section not found"));
        var studentUser = userRepository.findById(request.getStudentId())
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

        if (targetSession != null) {
            final UUID targetSessId = targetSession.getId();
            boolean existsInSession = repository.findBySchoolIdAndStudentId(schoolId, request.getStudentId()).stream()
                    .anyMatch(e -> {
                        UUID sId = e.getAcademicSession() != null ? e.getAcademicSession().getId() :
                                   (e.getSection() != null && e.getSection().getSchoolClass() != null && e.getSection().getSchoolClass().getSession() != null ? e.getSection().getSchoolClass().getSession().getId() : null);
                        return targetSessId.equals(sId);
                    });
            if (existsInSession) {
                throw new IllegalArgumentException("This student is already enrolled in a section for the selected academic session.");
            }
        }

        StudentEnrollment enr = new StudentEnrollment();
        enr.setSchoolId(schoolId);
        enr.setStudent(studentUser);
        enr.setSection(section);
        enr.setAcademicSession(targetSession);
        if (request.getEnrollmentDate() != null) enr.setEnrollmentDate(request.getEnrollmentDate());
        if (request.getRollNumber() != null) enr.setRollNumber(request.getRollNumber());

        StudentEnrollment saved = repository.save(enr);

        // Sync with Student entity directly
        studentRepository.findBySchoolIdAndUserId(schoolId, studentUser.getId()).ifPresent(studentEntity -> {
            if (section.getSchoolClass() != null) {
                studentEntity.setSchoolClass(section.getSchoolClass());
            }
            studentEntity.setSection(section);
            if (request.getRollNumber() != null) {
                studentEntity.setRollNumber(request.getRollNumber());
            }
            studentRepository.save(studentEntity);
        });

        return mapToResponse(saved);
    }

    public StudentEnrollmentResponse update(UUID id, StudentEnrollmentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new IllegalArgumentException("Unauthorized");

        Long targetStudentId = request.getStudentId() != null ? request.getStudentId() : entity.getStudent().getId();
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

        if (targetSession != null) {
            final UUID targetSessId = targetSession.getId();
            boolean existsInSession = repository.findBySchoolIdAndStudentId(schoolId, targetStudentId).stream()
                    .anyMatch(e -> !e.getId().equals(id) && targetSessId.equals(
                        e.getAcademicSession() != null ? e.getAcademicSession().getId() :
                        (e.getSection() != null && e.getSection().getSchoolClass() != null && e.getSection().getSchoolClass().getSession() != null ? e.getSection().getSchoolClass().getSession().getId() : null)
                    ));
            if (existsInSession) {
                throw new IllegalArgumentException("This student is already enrolled in a section for the selected academic session.");
            }
        }

        if (request.getStudentId() != null) {
             var studentUser = userRepository.findById(request.getStudentId())
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
             entity.setStudent(studentUser);
        }
        entity.setSection(section);
        if (targetSession != null) {
            entity.setAcademicSession(targetSession);
        }
        if (request.getEnrollmentDate() != null) entity.setEnrollmentDate(request.getEnrollmentDate());
        if (request.getRollNumber() != null) entity.setRollNumber(request.getRollNumber());

        StudentEnrollment saved = repository.save(entity);

        // Sync with Student entity directly
        if (saved.getStudent() != null) {
            studentRepository.findBySchoolIdAndUserId(schoolId, saved.getStudent().getId()).ifPresent(studentEntity -> {
                if (saved.getSection() != null && saved.getSection().getSchoolClass() != null) {
                    studentEntity.setSchoolClass(saved.getSection().getSchoolClass());
                    studentEntity.setSection(saved.getSection());
                }
                if (saved.getRollNumber() != null) {
                    studentEntity.setRollNumber(saved.getRollNumber());
                }
                studentRepository.save(studentEntity);
            });
        }
        
        return mapToResponse(saved);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id).orElseThrow(() -> new RuntimeException("Record not found"));
        if (!entity.getSchoolId().equals(schoolId)) throw new RuntimeException("Unauthorized");

        if (entity.getStudent() != null) {
            studentRepository.findBySchoolIdAndUserId(schoolId, entity.getStudent().getId()).ifPresent(studentEntity -> {
                studentEntity.setSchoolClass(null);
                studentEntity.setSection(null);
                studentEntity.setRollNumber(null);
                studentRepository.save(studentEntity);
            });
        }

        repository.delete(entity);
    }

    private StudentEnrollmentResponse mapToResponse(StudentEnrollment se) {
        StudentEnrollmentResponse res = new StudentEnrollmentResponse();
        res.setId(se.getId());
        if (se.getStudent() != null) {
            res.setStudentId(se.getStudent().getId());
            res.setStudentName(se.getStudent().getFullName());
        }
        if (se.getSection() != null) {
            res.setSectionId(se.getSection().getId());
            res.setSectionName(se.getSection().getName());
            if (se.getSection().getSchoolClass() != null) {
                res.setClassName(se.getSection().getSchoolClass().getName());
            }
        }
        res.setEnrollmentDate(se.getEnrollmentDate());
        res.setRollNumber(se.getRollNumber());

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