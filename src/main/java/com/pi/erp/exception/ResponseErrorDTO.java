package com.pi.erp.exception;

import java.time.LocalDateTime;

public record ResponseErrorDTO(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
}
