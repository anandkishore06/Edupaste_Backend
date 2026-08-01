package com.edupaste.services;

import com.edupaste.models.Parent;
import com.edupaste.models.Role;
import com.edupaste.models.Student;
import com.edupaste.models.User;
import com.edupaste.payloads.ParentRequest;
import com.edupaste.payloads.ParentResponse;
import com.edupaste.repositories.ParentRepository;
import com.edupaste.repositories.StudentRepository;
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
public class ParentService {

    @Autowired
    private ParentRepository repository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Page<ParentResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Parent> parents = repository.findBySchoolId(schoolId);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), parents.size());
        List<ParentResponse> pageContent = parents.subList(Math.max(0, Math.min(start, parents.size())), Math.max(0, Math.min(end, parents.size())))
                .stream().map(this::mapToResponse).collect(Collectors.toList());

        return new PageImpl<>(pageContent, pageable, parents.size());
    }

    public List<ParentResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        return repository.findBySchoolId(schoolId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public ParentResponse getById(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Parent parent = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!parent.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to parent record");
        }
        return mapToResponse(parent);
    }

    public ParentResponse create(ParentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        String primaryName = getPrimaryContactName(request);
        String rawPassword = StringUtils.hasText(request.getPassword()) ? request.getPassword() : "Edupaste@123";

        User user = User.builder()
                .fullName(primaryName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(Role.PARENT)
                .schoolId(schoolId)
                .build();
        user = userRepository.save(user);

        Parent parent = Parent.builder()
                .user(user)
                .fatherName(request.getFatherName() != null ? request.getFatherName().trim() : null)
                .motherName(request.getMotherName() != null ? request.getMotherName().trim() : null)
                .guardianName(request.getGuardianName() != null ? request.getGuardianName().trim() : null)
                .phone(request.getPhone() != null ? request.getPhone().trim() : "")
                .email(email)
                .occupation(request.getOccupation())
                .address(request.getAddress())
                .status(StringUtils.hasText(request.getStatus()) ? request.getStatus() : "ACTIVE")
                .build();
        parent.setSchoolId(schoolId);
        parent = repository.save(parent);

        return mapToResponse(parent);
    }

    public ParentResponse update(UUID id, ParentRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Parent parent = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!parent.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to parent record");
        }

        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";

        if (!parent.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email '" + email + "' is already registered to an account.");
        }

        parent.setFatherName(request.getFatherName() != null ? request.getFatherName().trim() : null);
        parent.setMotherName(request.getMotherName() != null ? request.getMotherName().trim() : null);
        parent.setGuardianName(request.getGuardianName() != null ? request.getGuardianName().trim() : null);
        parent.setPhone(request.getPhone() != null ? request.getPhone().trim() : "");
        parent.setEmail(email);
        parent.setOccupation(request.getOccupation());
        parent.setAddress(request.getAddress());
        if (StringUtils.hasText(request.getStatus())) {
            parent.setStatus(request.getStatus());
        }

        if (parent.getUser() != null) {
            parent.getUser().setFullName(getPrimaryContactName(request));
            parent.getUser().setEmail(email);
            if (StringUtils.hasText(request.getPassword())) {
                parent.getUser().setPassword(passwordEncoder.encode(request.getPassword()));
            }
            userRepository.save(parent.getUser());
        }

        parent = repository.save(parent);
        return mapToResponse(parent);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        Parent parent = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Parent not found"));
        if (!parent.getSchoolId().equals(schoolId)) {
            throw new IllegalArgumentException("Unauthorized access to parent record");
        }

        User user = parent.getUser();
        repository.delete(parent);
        if (user != null) {
            userRepository.delete(user);
        }
    }

    private String getPrimaryContactName(ParentRequest r) {
        if (StringUtils.hasText(r.getFatherName())) return r.getFatherName().trim();
        if (StringUtils.hasText(r.getMotherName())) return r.getMotherName().trim();
        if (StringUtils.hasText(r.getGuardianName())) return r.getGuardianName().trim();
        return "Parent Account";
    }

    private ParentResponse mapToResponse(Parent p) {
        ParentResponse res = new ParentResponse();
        res.setId(p.getId());
        res.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        res.setFatherName(p.getFatherName());
        res.setMotherName(p.getMotherName());
        res.setGuardianName(p.getGuardianName());
        
        String primary = "Parent Account";
        if (StringUtils.hasText(p.getFatherName())) primary = p.getFatherName();
        else if (StringUtils.hasText(p.getMotherName())) primary = p.getMotherName();
        else if (StringUtils.hasText(p.getGuardianName())) primary = p.getGuardianName();
        res.setPrimaryContactName(primary);

        res.setPhone(p.getPhone());
        res.setEmail(p.getEmail());
        res.setOccupation(p.getOccupation());
        res.setAddress(p.getAddress());
        res.setStatus(p.getStatus());
        res.setCreatedAt(p.getCreatedAt());

        List<Student> children = studentRepository.findByParentId(p.getId());
        if (children != null && !children.isEmpty()) {
            res.setChildren(children.stream().map(c -> new ParentResponse.ChildSummary(
                    c.getId(),
                    c.getFullName(),
                    c.getAdmissionNumber(),
                    c.getSchoolClass() != null ? c.getSchoolClass().getName() : null,
                    c.getSection() != null ? c.getSection().getName() : null
            )).collect(Collectors.toList()));
        }

        return res;
    }
}
