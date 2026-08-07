package com.edupaste.services;

import com.edupaste.models.*;
import com.edupaste.payloads.StudentRequest;
import com.edupaste.payloads.StudentResponse;
import com.edupaste.repositories.*;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class StudentService {

    @Autowired
    private StudentRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ParentRepository parentRepository;

    @Autowired
    private SchoolClassRepository classRepository;

    @Autowired
    private SectionRepository sectionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private AcademicSessionRepository academicSessionRepository;

    public Page<StudentResponse> getAll(UUID sessionId, Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Student> students = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        
        List<StudentResponse> list = students.stream()
                .map(s -> mapToResponse(s, sessionId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), list.size());
        List<StudentResponse> pageContent = list.subList(Math.max(0, Math.min(start, list.size())), Math.max(0, Math.min(end, list.size())));

        return new PageImpl<>(pageContent, pageable, list.size());
    }

    public List<StudentResponse> getAll(UUID sessionId) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Student> students = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        return students.stream()
                .map(s -> mapToResponse(s, sessionId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public Page<StudentResponse> getAll(Pageable pageable) {
        return getAll((UUID) null, pageable);
    }

    public List<StudentResponse> getAll() {
        return getAll((UUID) null);
    }

    private UUID getEffectiveSessionId(Long schoolId, UUID requestedSessionId) {
        if (requestedSessionId != null) {
            return requestedSessionId;
        }
        if (schoolId != null) {
            return academicSessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(AcademicSession::getId)
                    .orElse(null);
        }
        return academicSessionRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsCurrent()))
                .map(AcademicSession::getId)
                .findFirst().orElse(null);
    }

    public StudentResponse getById(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Student student = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to student record");
        }
        return mapToResponse(student);
    }

    public StudentResponse create(StudentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        String admissionNumber = request.getAdmissionNumber() != null ? request.getAdmissionNumber().trim() : "";
        
        if (repository.existsBySchoolIdAndAdmissionNumber(schoolId, admissionNumber)) {
            throw new IllegalArgumentException("Admission Number '" + admissionNumber + "' already exists in this school.");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!StringUtils.hasText(email)) {
            email = "std." + admissionNumber.replaceAll("[^a-zA-Z0-9]", "") + "@school." + schoolId + ".edupaste.com";
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Edupaste@123";

        User user = User.builder()
                .fullName(request.getFirstName().trim() + " " + request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.STUDENT)
                .schoolId(schoolId)
                .build();
        user = userRepository.save(user);

        Parent parent = null;
        if (request.getParentId() != null) {
            parent = parentRepository.findById(request.getParentId()).orElse(null);
        }

        AcademicSession admissionSession = null;
        if (request.getAdmissionSessionId() != null) {
            admissionSession = academicSessionRepository.findById(request.getAdmissionSessionId()).orElse(null);
        }

        Student student = Student.builder()
                .user(user)
                .parent(parent)
                .admissionNumber(admissionNumber)
                .admissionDate(request.getAdmissionDate() != null ? request.getAdmissionDate() : java.time.LocalDate.now())
                .admissionSession(admissionSession)
                .photo(request.getPhoto())
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .bloodGroup(request.getBloodGroup())
                .email(email)
                .mobile(request.getMobile())
                .address(request.getAddress())
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE")
                .build();
        student.setSchoolId(schoolId);
        student = repository.save(student);

        return mapToResponse(student);
    }

    public StudentResponse update(UUID id, StudentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Student student = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to student record");
        }

        String admissionNumber = request.getAdmissionNumber() != null ? request.getAdmissionNumber().trim() : "";
        if (repository.existsBySchoolIdAndAdmissionNumberAndIdNot(schoolId, admissionNumber, id)) {
            throw new IllegalArgumentException("Admission Number '" + admissionNumber + "' already exists in this school.");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (!StringUtils.hasText(email)) {
            email = "std." + admissionNumber.replaceAll("[^a-zA-Z0-9]", "") + "@school." + schoolId + ".edupaste.com";
        }

        if (student.getEmail() != null && !student.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        Parent parent = null;
        if (request.getParentId() != null) {
            parent = parentRepository.findById(request.getParentId()).orElse(null);
        }

        AcademicSession admissionSession = null;
        if (request.getAdmissionSessionId() != null) {
            admissionSession = academicSessionRepository.findById(request.getAdmissionSessionId()).orElse(null);
        }

        student.setAdmissionNumber(admissionNumber);
        student.setFirstName(request.getFirstName().trim());
        student.setLastName(request.getLastName().trim());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setBloodGroup(request.getBloodGroup());
        student.setEmail(email);
        student.setMobile(request.getMobile());
        student.setAddress(request.getAddress());
        student.setParent(parent);
        student.setPhoto(request.getPhoto());
        if (request.getAdmissionDate() != null) {
            student.setAdmissionDate(request.getAdmissionDate());
        }
        if (admissionSession != null) {
            student.setAdmissionSession(admissionSession);
        }

        if (StringUtils.hasText(request.getStatus())) {
            student.setStatus(request.getStatus());
        }

        if (student.getUser() != null) {
            student.getUser().setFullName(request.getFirstName().trim() + " " + request.getLastName().trim());
            student.getUser().setEmail(email);
            if (StringUtils.hasText(request.getPassword())) {
                student.getUser().setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(student.getUser());
        }

        student = repository.save(student);
        return mapToResponse(student);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Student student = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Student not found"));
        if (!student.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to student record");
        }

        User user = student.getUser();
        repository.delete(student);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    @Autowired
    private StudentEnrollmentRepository studentEnrollmentRepository;

    private StudentResponse mapToResponse(Student s) {
        return mapToResponse(s, null);
    }

    private StudentResponse mapToResponse(Student s, UUID targetSessionId) {
        StudentResponse res = new StudentResponse();
        res.setId(s.getId());
        res.setUserId(s.getUser() != null ? s.getUser().getId() : null);
        res.setAdmissionNumber(s.getAdmissionNumber());
        res.setFirstName(s.getFirstName());
        res.setLastName(s.getLastName());
        res.setFullName(s.getFirstName() + " " + s.getLastName());
        res.setGender(s.getGender());
        res.setDateOfBirth(s.getDateOfBirth());
        res.setBloodGroup(s.getBloodGroup());
        res.setEmail(s.getEmail());
        res.setMobile(s.getMobile());
        res.setAddress(s.getAddress());
        res.setAdmissionDate(s.getAdmissionDate());
        res.setPhoto(s.getPhoto());
        if (s.getAdmissionSession() != null) {
            res.setAdmissionSessionId(s.getAdmissionSession().getId());
            res.setAdmissionSessionName(s.getAdmissionSession().getName());
        }

        if (s.getParent() != null) {
            res.setParentId(s.getParent().getId());
            String parentName = "Parent Account";
            if (StringUtils.hasText(s.getParent().getFatherName())) parentName = s.getParent().getFatherName();
            else if (StringUtils.hasText(s.getParent().getMotherName())) parentName = s.getParent().getMotherName();
            else if (StringUtils.hasText(s.getParent().getGuardianName())) parentName = s.getParent().getGuardianName();
            res.setParentName(parentName);
        }

        // Auto-populate Class, Section, and Roll Number from Academic Student Enrollment
        String className = "Not Enrolled";
        String sectionName = null;
        String rollNumber = "N/A";

        boolean hasExplicitEnrollment = false;
        boolean matchesTargetSession = false;

        List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySchoolIdAndStudentId(s.getSchoolId(), s.getId());
        StudentEnrollment matchedEnr = null;

        if (targetSessionId != null && !enrollments.isEmpty()) {
            matchedEnr = enrollments.stream()
                    .filter(e -> (e.getAcademicSession() != null && targetSessionId.equals(e.getAcademicSession().getId())) ||
                                 (e.getSection() != null && e.getSection().getSchoolClass() != null && e.getSection().getSchoolClass().getSession() != null && targetSessionId.equals(e.getSection().getSchoolClass().getSession().getId())))
                    .findFirst().orElse(null);
        }

        if (targetSessionId == null && !enrollments.isEmpty()) {
            matchedEnr = enrollments.get(0);
        }

        if (matchedEnr != null) {
            hasExplicitEnrollment = true;
            matchesTargetSession = true;
            if (matchedEnr.getSection() != null) {
                res.setSectionId(matchedEnr.getSection().getId());
                sectionName = matchedEnr.getSection().getName();
                if (matchedEnr.getSection().getSchoolClass() != null) {
                    res.setClassId(matchedEnr.getSection().getSchoolClass().getId());
                    className = matchedEnr.getSection().getSchoolClass().getName();
                }
            }
            if (StringUtils.hasText(matchedEnr.getRollNumber())) {
                rollNumber = matchedEnr.getRollNumber();
            }
        }

        if (targetSessionId != null && !matchesTargetSession) {
            return null;
        }

        res.setClassName(className);
        res.setSectionName(sectionName);
        res.setRollNumber(rollNumber);

        res.setStatus(s.getStatus());
        res.setCreatedAt(s.getCreatedAt());
        return res;
    }
}
