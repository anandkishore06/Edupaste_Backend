package com.edupaste.payloads;

import lombok.Data;
import java.util.UUID;

@Data
public class TeacherAssignmentResponse {
    private UUID id;
    private Long teacherId;
    private String teacherName;
    private UUID classSubjectId;
    private String className;
    private String sectionName;
    private String subjectName;
    private String subjectType;
    private UUID academicSessionId;
    private String academicSessionName;
    private Boolean isPrimary;
}