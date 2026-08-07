package com.edupaste.services;

import com.edupaste.models.Role;
import com.edupaste.models.Teacher;
import com.edupaste.models.User;
import com.edupaste.payloads.TeacherRequest;
import com.edupaste.payloads.TeacherResponse;
import com.edupaste.repositories.TeacherRepository;
import com.edupaste.repositories.UserRepository;
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
public class TeacherService {

    @Autowired
    private TeacherRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<TeacherResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Teacher> teachers = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), teachers.size());
        List<TeacherResponse> pageContent = teachers.subList(Math.max(0, Math.min(start, teachers.size())), Math.max(0, Math.min(end, teachers.size())))
                .stream().map(this::mapToResponse).collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, teachers.size());
    }

    public List<TeacherResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Teacher> teachers = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        return teachers.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TeacherResponse getById(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Teacher teacher = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to teacher record");
        }
        return mapToResponse(teacher);
    }

    public TeacherResponse create(TeacherRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        String employeeId = request.getEmployeeId() != null ? request.getEmployeeId().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (repository.existsBySchoolIdAndEmployeeId(schoolId, employeeId)) {
            throw new IllegalArgumentException("Employee ID '" + employeeId + "' already exists in this school.");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Edupaste@123";
        User user = User.builder()
                .fullName(request.getFirstName().trim() + " " + request.getLastName().trim())
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.TEACHER)
                .schoolId(schoolId)
                .build();
        user = userRepository.save(user);

        Teacher teacher = Teacher.builder()
                .user(user)
                .employeeId(employeeId)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(email)
                .phone(request.getPhone())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .qualification(request.getQualification())
                .experience(request.getExperience())
                .joiningDate(request.getJoiningDate())
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE")
                .build();
        teacher.setSchoolId(schoolId);
        teacher = repository.save(teacher);

        return mapToResponse(teacher);
    }

    public TeacherResponse update(UUID id, TeacherRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Teacher teacher = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to teacher record");
        }

        String employeeId = request.getEmployeeId() != null ? request.getEmployeeId().trim() : "";
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (repository.existsBySchoolIdAndEmployeeIdAndIdNot(schoolId, employeeId, id)) {
            throw new IllegalArgumentException("Employee ID '" + employeeId + "' already exists in this school.");
        }

        if (!teacher.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        teacher.setEmployeeId(employeeId);
        teacher.setFirstName(request.getFirstName().trim());
        teacher.setLastName(request.getLastName().trim());
        teacher.setEmail(email);
        teacher.setPhone(request.getPhone());
        teacher.setGender(request.getGender());
        teacher.setDateOfBirth(request.getDateOfBirth());
        teacher.setQualification(request.getQualification());
        teacher.setExperience(request.getExperience());
        teacher.setJoiningDate(request.getJoiningDate());
        if (StringUtils.hasText(request.getStatus())) {
            teacher.setStatus(request.getStatus());
        }

        if (teacher.getUser() != null) {
            teacher.getUser().setFullName(request.getFirstName().trim() + " " + request.getLastName().trim());
            teacher.getUser().setEmail(email);
            if (StringUtils.hasText(request.getPassword())) {
                teacher.getUser().setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(teacher.getUser());
        }

        teacher = repository.save(teacher);
        return mapToResponse(teacher);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Teacher teacher = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Teacher not found"));
        if (!teacher.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to teacher record");
        }

        User user = teacher.getUser();
        repository.delete(teacher);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private TeacherResponse mapToResponse(Teacher t) {
        TeacherResponse res = new TeacherResponse();
        res.setId(t.getId());
        res.setUserId(t.getUser() != null ? t.getUser().getId() : null);
        res.setEmployeeId(t.getEmployeeId());
        res.setFirstName(t.getFirstName());
        res.setLastName(t.getLastName());
        res.setFullName(t.getFirstName() + " " + t.getLastName());
        res.setEmail(t.getEmail());
        res.setPhone(t.getPhone());
        res.setGender(t.getGender());
        res.setDateOfBirth(t.getDateOfBirth());
        res.setQualification(t.getQualification());
        res.setExperience(t.getExperience());
        res.setJoiningDate(t.getJoiningDate());
        res.setStatus(t.getStatus());
        res.setCreatedAt(t.getCreatedAt());
        return res;
    }
}
