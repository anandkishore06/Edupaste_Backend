package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SubjectResponse {
    private UUID id;
    private String name;
    private String code;
    private String description;
    private String type;
    private Boolean isElective;
    private Boolean isLanguage;
    private Integer credits;
}