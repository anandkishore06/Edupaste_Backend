package com.edupaste.specifications;

import com.edupaste.models.AdmissionApplication;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdmissionApplicationSpecification {

    public static Specification<AdmissionApplication> withFilters(Long schoolId, UUID sessionId, String status, UUID classId, String search, boolean todayOnly) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (schoolId != null) {
                predicates.add(criteriaBuilder.equal(root.get("schoolId"), schoolId));
            }

            if (sessionId != null) {
                predicates.add(criteriaBuilder.equal(root.get("academicSessionId"), sessionId));
            }

            if (status != null && !status.trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (classId != null) {
                predicates.add(criteriaBuilder.equal(root.get("applyingClassId"), classId));
            }

            if (search != null && !search.trim().isEmpty()) {
                String searchPattern = "%" + search.toLowerCase() + "%";
                Predicate appNumberMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("applicationNumber")), searchPattern);
                Predicate firstNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("firstName")), searchPattern);
                Predicate lastNameMatch = criteriaBuilder.like(criteriaBuilder.lower(root.get("lastName")), searchPattern);
                predicates.add(criteriaBuilder.or(appNumberMatch, firstNameMatch, lastNameMatch));
            }

            if (todayOnly) {
                LocalDateTime startOfDay = LocalDateTime.now().toLocalDate().atStartOfDay();
                LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);
                predicates.add(criteriaBuilder.between(root.get("submittedAt"), startOfDay, endOfDay));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
