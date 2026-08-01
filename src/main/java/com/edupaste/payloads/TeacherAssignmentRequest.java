package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;

@Data
public class TeacherAssignmentRequest {
    private Long teacherId;
    private UUID classSubjectId;
    private UUID academicSessionId;
    private Boolean isPrimary;
}