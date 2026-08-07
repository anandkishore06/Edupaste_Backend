package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicClassDto {
    private String name;
    private String description;
    private Integer displayOrder;
}
