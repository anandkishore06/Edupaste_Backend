package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SectionRequest {
    private UUID classId;
    private String name;
    private Integer capacity;
    private String room;
}