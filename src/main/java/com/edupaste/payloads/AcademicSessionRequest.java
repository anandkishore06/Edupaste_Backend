package com.edupaste.payloads;
import lombok.Data;
import java.time.LocalDate;
@Data
public class AcademicSessionRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isCurrent;
}