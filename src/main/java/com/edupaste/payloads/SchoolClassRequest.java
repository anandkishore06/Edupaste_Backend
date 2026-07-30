package com.edupaste.payloads;
import lombok.Data;
@Data
public class SchoolClassRequest {
    private String name;
    private String description;

    private Integer displayOrder;
}