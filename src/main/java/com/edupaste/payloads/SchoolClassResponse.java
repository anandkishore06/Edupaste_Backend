package com.edupaste.payloads;
import lombok.Data;
import java.util.UUID;
@Data
public class SchoolClassResponse {
    private UUID id;
    private String name;
    private String description;
}