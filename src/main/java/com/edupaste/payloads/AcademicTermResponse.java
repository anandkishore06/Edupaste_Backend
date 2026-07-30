package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
import java.util.UUID;

@Data
public class AcademicTermResponse {
    private UUID id;
    private UUID sessionId;
    private String sessionName;
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer displayOrder;
}