package com.nunclear.escritores.dto.response;

public record GlobalNoticeResponse(
        Integer id,
        String title,
        String messageText,
        Boolean isEnabled
) {
}