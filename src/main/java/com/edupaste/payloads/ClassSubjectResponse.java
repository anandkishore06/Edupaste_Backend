package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;

@Data
public class ClassSubjectResponse {
    private UUID id;
    private UUID sectionId;
    private String sectionName;
    private String className;
    private UUID subjectId;
    private String subjectName;
    private String subjectCode;
    private String subjectType;
    private Integer weeklyPeriods;
    private Boolean isMandatory;
}