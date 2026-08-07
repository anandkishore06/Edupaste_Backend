package com.edupaste.payloads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicRequiredDocumentDto {
    private String documentName;
    private String documentKey;
    private Boolean isRequired;
    private String description;
}
