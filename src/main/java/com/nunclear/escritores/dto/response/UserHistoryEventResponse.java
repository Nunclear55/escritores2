package com.nunclear.escritores.dto.response;

import java.time.LocalDateTime;

public record UserHistoryEventResponse(
        String changedField,
        String oldValue,
        String newValue,
        LocalDateTime changedAt,
        Integer changedByUserId
) {
}