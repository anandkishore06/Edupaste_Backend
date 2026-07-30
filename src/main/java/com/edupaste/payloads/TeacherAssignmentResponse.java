package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;

@Data
public class TeacherAssignmentResponse {
    private UUID id;
    private Long teacherId;
    private UUID classSubjectId;
    private Boolean isPrimary;
}