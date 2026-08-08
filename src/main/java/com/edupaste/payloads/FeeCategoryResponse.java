package com.edupaste.payloads;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class FeeCategoryResponse {
    private UUID id;
    private UUID classId;
    private String className;
    private UUID academicSessionId;
    private String academicSessionName;
    private String feeType;
    private BigDecimal amount;
    private String status;
}
