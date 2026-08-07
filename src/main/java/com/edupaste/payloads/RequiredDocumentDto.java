package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RequiredDocumentDto {
    private UUID id;
    private String documentName;
    private String documentKey;
    private Boolean isRequired;
    private String description;
}
