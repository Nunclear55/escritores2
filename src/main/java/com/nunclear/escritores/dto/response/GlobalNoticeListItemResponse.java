package com.nunclear.escritores.dto.response;

public record GlobalNoticeListItemResponse(
        Integer id,
        String title,
        String messageText,
        Boolean isEnabled
) {
}