package com.edupaste.services;

import com.edupaste.models.AcademicSession;
import com.edupaste.models.SchoolClass;
import com.edupaste.payloads.SchoolClassRequest;
import com.edupaste.payloads.SchoolClassResponse;
import com.edupaste.repositories.AcademicSessionRepository;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SchoolClassService {

    @Autowired
    private SchoolClassRepository repository;

    @Autowired
    private AcademicSessionRepository sessionRepository;

    public Page<SchoolClassResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<SchoolClass> classes = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        classes.sort(this::compareClasses);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), classes.size());
        List<SchoolClassResponse> pageContent = classes.subList(Math.max(0, Math.min(start, classes.size())), Math.max(0, Math.min(end, classes.size())))
                .stream().map(this::mapToResponse).collect(Collectors.toList());
                
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, classes.size());
    }

    public List<SchoolClassResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<SchoolClass> classes = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        classes.sort(this::compareClasses);
        return classes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SchoolClassResponse create(SchoolClassRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        AcademicSession session = sessionRepository.findBySchoolIdAndIsCurrentTrue(schoolId)
                .orElseThrow(() -> new IllegalArgumentException("No active academic session found for this school."));
                
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Class name is required.");
        }

        // Duplicate name check in school
        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(c -> c.getName() != null && c.getName().trim().equalsIgnoreCase(trimmedName));
        if (exists) {
            throw new IllegalArgumentException("A class named '" + trimmedName + "' already exists.");
        }

        SchoolClass sc = new SchoolClass();
        sc.setSchoolId(schoolId);
        sc.setSession(session);
        sc.setName(trimmedName);
        sc.setDescription(request.getDescription());
        
        sc = repository.save(sc);
        return mapToResponse(sc);
    }

    
    public SchoolClassResponse update(UUID id, SchoolClassRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("Class name is required.");
            }
            boolean exists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(c -> !c.getId().equals(id) && c.getName() != null && c.getName().trim().equalsIgnoreCase(trimmedName));
            if (exists) {
                throw new IllegalArgumentException("A class named '" + trimmedName + "' already exists.");
            }
            entity.setName(trimmedName);
        }
        if (request.getDescription() != null) entity.setDescription(request.getDescription());

        
        entity = repository.save(entity);
        return mapToResponse(entity);
    }

    public void delete(UUID id) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        
        repository.delete(entity);
    }

    private int compareClasses(SchoolClass c1, SchoolClass c2) {
        if (c1 == null && c2 == null) return 0;
        if (c1 == null) return 1;
        if (c2 == null) return -1;

        String nameA = c1.getName() != null ? c1.getName().trim() : "";
        String nameB = c2.getName() != null ? c2.getName().trim() : "";

        boolean hasDigitA = nameA.matches(".*\\d.*");
        boolean hasDigitB = nameB.matches(".*\\d.*");

        if (!hasDigitA && hasDigitB) return -1;
        if (hasDigitA && !hasDigitB) return 1;

        if (!hasDigitA && !hasDigitB) {
            if (c1.getCreatedAt() != null && c2.getCreatedAt() != null) {
                int timeCompare = c1.getCreatedAt().compareTo(c2.getCreatedAt());
                if (timeCompare != 0) return timeCompare;
            }
            if (c1.getId() != null && c2.getId() != null) {
                return c1.getId().compareTo(c2.getId());
            }
            return 0;
        }

        return naturalCompare(nameA, nameB);
    }

    private int naturalCompare(String s1, String s2) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("(\\d+|\\D+)");
        java.util.regex.Matcher m1 = p.matcher(s1);
        java.util.regex.Matcher m2 = p.matcher(s2);

        while (m1.find() && m2.find()) {
            String tok1 = m1.group();
            String tok2 = m2.group();

            int res;
            if (Character.isDigit(tok1.charAt(0)) && Character.isDigit(tok2.charAt(0))) {
                try {
                    java.math.BigInteger num1 = new java.math.BigInteger(tok1);
                    java.math.BigInteger num2 = new java.math.BigInteger(tok2);
                    res = num1.compareTo(num2);
                } catch (Exception e) {
                    res = tok1.compareToIgnoreCase(tok2);
                }
            } else {
                res = tok1.compareToIgnoreCase(tok2);
            }
            if (res != 0) return res;
        }
        return Boolean.compare(m1.find(), m2.find());
    }

    private SchoolClassResponse mapToResponse(SchoolClass sc) {
        SchoolClassResponse res = new SchoolClassResponse();
        res.setId(sc.getId());
        res.setName(sc.getName());
        res.setDescription(sc.getDescription());
        return res;
    }
}
