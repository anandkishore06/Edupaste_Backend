package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class ClassSubjectRequest {
    private UUID sectionId;
    private UUID subjectId;
    private Integer weeklyPeriods;
    private Boolean isMandatory;
}