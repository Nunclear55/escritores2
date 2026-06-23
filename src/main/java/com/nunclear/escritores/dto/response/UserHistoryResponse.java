package com.nunclear.escritores.dto.response;

import java.util.List;

public record UserHistoryResponse(
        List<UserHistoryEventResponse> events
) {
}