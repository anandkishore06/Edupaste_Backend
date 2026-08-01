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

    public Page<StudentResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Student> students = repository.findBySchoolId(schoolId);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), students.size());
        List<StudentResponse> pageContent = students.subList(Math.max(0, Math.min(start, students.size())), Math.max(0, Math.min(end, students.size())))
                .stream().map(this::mapToResponse).collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, students.size());
    }

    public List<StudentResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
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

        SchoolClass schoolClass = null;
        if (request.getClassId() != null) {
            schoolClass = classRepository.findById(request.getClassId()).orElse(null);
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        Student student = Student.builder()
                .user(user)
                .parent(parent)
                .schoolClass(schoolClass)
                .section(section)
                .admissionNumber(admissionNumber)
                .rollNumber(request.getRollNumber() != null ? request.getRollNumber().trim() : null)
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

        SchoolClass schoolClass = null;
        if (request.getClassId() != null) {
            schoolClass = classRepository.findById(request.getClassId()).orElse(null);
        }

        Section section = null;
        if (request.getSectionId() != null) {
            section = sectionRepository.findById(request.getSectionId()).orElse(null);
        }

        student.setAdmissionNumber(admissionNumber);
        student.setRollNumber(request.getRollNumber() != null ? request.getRollNumber().trim() : null);
        student.setFirstName(request.getFirstName().trim());
        student.setLastName(request.getLastName().trim());
        student.setGender(request.getGender());
        student.setDateOfBirth(request.getDateOfBirth());
        student.setBloodGroup(request.getBloodGroup());
        student.setEmail(email);
        student.setMobile(request.getMobile());
        student.setAddress(request.getAddress());
        student.setParent(parent);
        student.setSchoolClass(schoolClass);
        student.setSection(section);

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

        if (s.getUser() != null) {
            List<StudentEnrollment> enrollments = studentEnrollmentRepository.findBySchoolIdAndStudentId(s.getSchoolId(), s.getUser().getId());
            if (!enrollments.isEmpty()) {
                StudentEnrollment activeEnr = enrollments.get(0);
                if (activeEnr.getSection() != null) {
                    res.setSectionId(activeEnr.getSection().getId());
                    sectionName = activeEnr.getSection().getName();
                    if (activeEnr.getSection().getSchoolClass() != null) {
                        res.setClassId(activeEnr.getSection().getSchoolClass().getId());
                        className = activeEnr.getSection().getSchoolClass().getName();
                    }
                }
                if (StringUtils.hasText(activeEnr.getRollNumber())) {
                    rollNumber = activeEnr.getRollNumber();
                }
            }
        }

        // Fallback if not found in enrollment but saved in Student entity
        if ("Not Enrolled".equals(className) && s.getSchoolClass() != null) {
            res.setClassId(s.getSchoolClass().getId());
            className = s.getSchoolClass().getName();
        }
        if (sectionName == null && s.getSection() != null) {
            res.setSectionId(s.getSection().getId());
            sectionName = s.getSection().getName();
        }
        if ("N/A".equals(rollNumber) && StringUtils.hasText(s.getRollNumber())) {
            rollNumber = s.getRollNumber();
        }

        res.setClassName(className);
        res.setSectionName(sectionName);
        res.setRollNumber(rollNumber);

        res.setStatus(s.getStatus());
        res.setCreatedAt(s.getCreatedAt());
        return res;
    }
}
