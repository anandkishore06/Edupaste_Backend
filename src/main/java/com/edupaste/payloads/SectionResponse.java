package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SectionResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private String name;
    private Integer capacity;
    private String room;
}