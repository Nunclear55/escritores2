package com.nunclear.escritores.dto.response;

public record AssignedReportResponse(
        Integer id,
        Integer reviewedByUserId,
        String statusName
) {
}