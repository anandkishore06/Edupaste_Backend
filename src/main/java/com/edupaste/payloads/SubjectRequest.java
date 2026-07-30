package com.edupaste.payloads;
import lombok.Data;
@Data
public class SubjectRequest {
    private String name;
    private String code;
    private String description;
    private String type;
    private Boolean isElective;
    private Boolean isLanguage;
    private Integer credits;
}