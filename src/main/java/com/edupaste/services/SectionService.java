package com.edupaste.services;

import com.edupaste.models.SchoolClass;
import com.edupaste.models.Section;
import com.edupaste.payloads.SectionRequest;
import com.edupaste.payloads.SectionResponse;
import com.edupaste.repositories.SchoolClassRepository;
import com.edupaste.repositories.SectionRepository;
import com.edupaste.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SectionService {

    @Autowired
    private SectionRepository repository;

    @Autowired
    private SchoolClassRepository classRepository;

    public Page<SectionResponse> getAll(Pageable pageable) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Section> sections = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        sections.sort(this::compareSections);
        
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), sections.size());
        List<SectionResponse> pageContent = sections.subList(Math.max(0, Math.min(start, sections.size())), Math.max(0, Math.min(end, sections.size())))
                .stream().map(this::mapToResponse).collect(Collectors.toList());
                
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, sections.size());
    }

    public List<SectionResponse> getAll() {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        List<Section> sections = (schoolId == null) ? repository.findAll() : repository.findBySchoolId(schoolId);
        sections.sort(this::compareSections);
        return sections.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public SectionResponse create(SectionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        SchoolClass sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Selected class not found."));
                
        String trimmedName = request.getName() != null ? request.getName().trim() : "";
        if (trimmedName.isEmpty()) {
            throw new IllegalArgumentException("Section name is required.");
        }

        boolean exists = repository.findBySchoolId(schoolId).stream()
                .anyMatch(s -> s.getSchoolClass() != null && s.getSchoolClass().getId().equals(sc.getId()) && s.getName() != null && s.getName().trim().equalsIgnoreCase(trimmedName));
        if (exists) {
            throw new IllegalArgumentException("A section named '" + trimmedName + "' already exists in " + sc.getName() + ".");
        }

        Section section = new Section();
        section.setSchoolId(schoolId);
        section.setSchoolClass(sc);
        section.setName(trimmedName);
        if (request.getCapacity() != null) section.setCapacity(request.getCapacity());
        if (request.getRoom() != null) section.setRoom(request.getRoom());
        
        section = repository.save(section);
        return mapToResponse(section);
    }

    
    public SectionResponse update(UUID id, SectionRequest request) {
        Long schoolId = SecurityUtils.getCurrentUserDetails().getSchoolId();
        var entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Record not found"));
                
        if (!entity.getSchoolId().equals(schoolId)) {
            throw new RuntimeException("Unauthorized");
        }
        

        if (request.getClassId() != null) {
            var sc = classRepository.findById(request.getClassId())
                .orElseThrow(() -> new IllegalArgumentException("Selected class not found."));
            entity.setSchoolClass(sc);
        }

        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (trimmedName.isEmpty()) {
                throw new IllegalArgumentException("Section name is required.");
            }
            final java.util.UUID currentClassId = entity.getSchoolClass().getId();
            boolean exists = repository.findBySchoolId(schoolId).stream()
                    .anyMatch(s -> !s.getId().equals(id) && s.getSchoolClass() != null && s.getSchoolClass().getId().equals(currentClassId) && s.getName() != null && s.getName().trim().equalsIgnoreCase(trimmedName));
            if (exists) {
                throw new IllegalArgumentException("A section named '" + trimmedName + "' already exists in " + entity.getSchoolClass().getName() + ".");
            }
            entity.setName(trimmedName);
        }
        if (request.getCapacity() != null) entity.setCapacity(request.getCapacity());
        if (request.getRoom() != null) entity.setRoom(request.getRoom());

        
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

    private SectionResponse mapToResponse(Section sec) {
        SectionResponse res = new SectionResponse();
        res.setId(sec.getId());
        res.setClassId(sec.getSchoolClass().getId());
        res.setClassName(sec.getSchoolClass().getName());
        res.setName(sec.getName());
        res.setCapacity(sec.getCapacity());
        res.setRoom(sec.getRoom());
        return res;
    }

    private int compareSections(Section s1, Section s2) {
        SchoolClass classA = s1.getSchoolClass();
        SchoolClass classB = s2.getSchoolClass();
        
        int classCompare = compareClasses(classA, classB);
        if (classCompare != 0) {
            return classCompare;
        }

        String nameA = s1.getName() != null ? s1.getName() : "";
        String nameB = s2.getName() != null ? s2.getName() : "";
        return compareClassNames(nameA, nameB);
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

    private int compareClassNames(String nameA, String nameB) {
        if (nameA == null) nameA = "";
        if (nameB == null) nameB = "";
        nameA = nameA.trim();
        nameB = nameB.trim();

        boolean hasDigitA = nameA.matches(".*\\d.*");
        boolean hasDigitB = nameB.matches(".*\\d.*");

        if (!hasDigitA && hasDigitB) return -1;
        if (hasDigitA && !hasDigitB) return 1;

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
}
