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

import java.util.ArrayList;
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

    @Autowired
    private com.edupaste.repositories.AcademicSessionRepository academicSessionRepository;

    @Autowired
    private com.edupaste.repositories.StudentEnrollmentRepository studentEnrollmentRepository;

    public Page<ParentResponse> getAll(UUID sessionId, Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Parent> parents = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        
        List<ParentResponse> allFlatRows = new ArrayList<>();
        for (Parent p : parents) {
            allFlatRows.addAll(mapToFlatResponseList(p, sessionId));
        }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allFlatRows.size());
        List<ParentResponse> pageContent = allFlatRows.subList(Math.max(0, Math.min(start, allFlatRows.size())), Math.max(0, Math.min(end, allFlatRows.size())));

        return new PageImpl<>(pageContent, pageable, allFlatRows.size());
    }

    public List<ParentResponse> getAll(UUID sessionId) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Parent> parents = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        List<ParentResponse> allFlatRows = new ArrayList<>();
        for (Parent p : parents) {
            allFlatRows.addAll(mapToFlatResponseList(p, sessionId));
        }
        return allFlatRows;
    }

    public Page<ParentResponse> getAll(Pageable pageable) {
        return getAll((UUID) null, pageable);
    }

    public List<ParentResponse> getAll() {
        return getAll((UUID) null);
    }

    private UUID getEffectiveSessionId(Long schoolId, UUID requestedSessionId) {
        if (requestedSessionId != null) {
            return requestedSessionId;
        }
        if (schoolId != null) {
            return academicSessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                    .map(com.edupaste.models.AcademicSession::getId)
                    .orElse(null);
        }
        return academicSessionRepository.findAll().stream()
                .filter(s -> Boolean.TRUE.equals(s.getIsCurrent()))
                .map(com.edupaste.models.AcademicSession::getId)
                .findFirst().orElse(null);
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
                .guardianRelation(request.getGuardianRelation() != null ? request.getGuardianRelation().trim() : null)
                .mobile(request.getMobile() != null ? request.getMobile().trim() : "")
                .alternateMobile(request.getAlternateMobile() != null ? request.getAlternateMobile().trim() : null)
                .email(email)
                .occupation(request.getOccupation())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
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
        parent.setGuardianRelation(request.getGuardianRelation() != null ? request.getGuardianRelation().trim() : null);
        parent.setMobile(request.getMobile() != null ? request.getMobile().trim() : "");
        parent.setAlternateMobile(request.getAlternateMobile() != null ? request.getAlternateMobile().trim() : null);
        parent.setEmail(email);
        parent.setOccupation(request.getOccupation());
        parent.setAddress(request.getAddress());
        parent.setCity(request.getCity());
        parent.setState(request.getState());
        parent.setCountry(request.getCountry());
        parent.setPostalCode(request.getPostalCode());
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
        List<ParentResponse> list = mapToFlatResponseList(p, null);
        return list.isEmpty() ? createBaseParentResponse(p) : list.get(0);
    }

    private ParentResponse createBaseParentResponse(Parent p) {
        ParentResponse res = new ParentResponse();
        res.setId(p.getId());
        res.setUserId(p.getUser() != null ? p.getUser().getId() : null);
        res.setFatherName(p.getFatherName());
        res.setMotherName(p.getMotherName());
        res.setGuardianName(p.getGuardianName());
        res.setGuardianRelation(p.getGuardianRelation());
        
        String primary = "Parent Account";
        if (StringUtils.hasText(p.getFatherName())) primary = p.getFatherName();
        else if (StringUtils.hasText(p.getMotherName())) primary = p.getMotherName();
        else if (StringUtils.hasText(p.getGuardianName())) primary = p.getGuardianName();
        res.setPrimaryContactName(primary);

        res.setMobile(p.getMobile());
        res.setAlternateMobile(p.getAlternateMobile());
        res.setEmail(p.getEmail());
        res.setOccupation(p.getOccupation());
        res.setAddress(p.getAddress());
        res.setCity(p.getCity());
        res.setState(p.getState());
        res.setCountry(p.getCountry());
        res.setPostalCode(p.getPostalCode());
        res.setStatus(p.getStatus());
        res.setCreatedAt(p.getCreatedAt());
        return res;
    }

    private List<ParentResponse> mapToFlatResponseList(Parent p, UUID targetSessionId) {
        List<ParentResponse> list = new ArrayList<>();
        List<Student> children = studentRepository.findByParentId(p.getId());

        if (children == null || children.isEmpty()) {
            ParentResponse res = createBaseParentResponse(p);
            res.setChildName("Unlinked");
            res.setAdmissionNumber("N/A");
            res.setClassName("N/A");
            res.setSectionName("N/A");
            res.setRollNumber("N/A");
            res.setAcademicSessionName("N/A");
            list.add(res);
            return list;
        }

        boolean parentHasMatchedChildren = false;

        for (Student c : children) {
            String className = "Not Enrolled";
            String sectionName = null;
            String rollNumber = "N/A";
            String sessionName = "N/A";
            UUID sessionId = null;
            UUID classId = null;
            UUID sectionId = null;

            boolean hasExplicitEnrollment = false;
            boolean matchesTargetSession = false;

            List<com.edupaste.models.StudentEnrollment> enrollments = studentEnrollmentRepository.findBySchoolIdAndStudentId(c.getSchoolId(), c.getId());
            com.edupaste.models.StudentEnrollment matchedEnr = null;
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
                    sectionId = matchedEnr.getSection().getId();
                    sectionName = matchedEnr.getSection().getName();
                    if (matchedEnr.getSection().getSchoolClass() != null) {
                        classId = matchedEnr.getSection().getSchoolClass().getId();
                        className = matchedEnr.getSection().getSchoolClass().getName();
                        if (matchedEnr.getSection().getSchoolClass().getSession() != null) {
                            sessionName = matchedEnr.getSection().getSchoolClass().getSession().getName();
                            sessionId = matchedEnr.getSection().getSchoolClass().getSession().getId();
                        }
                    }
                }
                if (matchedEnr.getAcademicSession() != null) {
                    sessionName = matchedEnr.getAcademicSession().getName();
                    sessionId = matchedEnr.getAcademicSession().getId();
                }
                if (StringUtils.hasText(matchedEnr.getRollNumber())) {
                    rollNumber = matchedEnr.getRollNumber();
                }
            }

            if (targetSessionId == null || matchesTargetSession) {
                parentHasMatchedChildren = true;
                ParentResponse res = createBaseParentResponse(p);
                res.setChildId(c.getId());
                res.setChildName(c.getFullName());
                res.setAdmissionNumber(c.getAdmissionNumber());
                res.setClassId(classId);
                res.setClassName(className);
                res.setSectionId(sectionId);
                res.setSectionName(sectionName);
                res.setRollNumber(rollNumber);
                res.setAcademicSessionId(sessionId);
                res.setAcademicSessionName(sessionName);

                // Build Children summary for backward compatibility
                res.setChildren(List.of(new ParentResponse.ChildSummary(
                        c.getId(),
                        c.getFullName(),
                        c.getAdmissionNumber(),
                        className,
                        sectionName
                )));
                list.add(res);
            }
        }

        if (targetSessionId != null && !parentHasMatchedChildren) {
            ParentResponse res = createBaseParentResponse(p);
            res.setChildName("No children in session");
            res.setAdmissionNumber("N/A");
            res.setClassName("N/A");
            res.setSectionName("N/A");
            res.setRollNumber("N/A");
            res.setAcademicSessionName("N/A");
            list.add(res);
        }

        return list;
    }
}
