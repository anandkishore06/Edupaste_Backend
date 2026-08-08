package com.edupaste.payloads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FeeCategoryRequest {
    @NotNull(message = "Class is required")
    private UUID classId;

    @NotNull(message = "Academic session is required")
    private UUID academicSessionId;

    @NotBlank(message = "Fee type is required")
    private String feeType;

    @NotNull(message = "Amount is required")
    private BigDecimal amount;
}
