package com.nunclear.escritores.dto.response;

public record ResolvedReportResponse(
        Integer id,
        String statusName,
        String resolutionText
) {
}