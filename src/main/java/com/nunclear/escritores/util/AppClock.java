package com.nunclear.escritores.util;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

public final class AppClock {

    public static final ZoneId APPLICATION_ZONE = ZoneId.of("UTC");
    private static final Clock CLOCK = Clock.system(APPLICATION_ZONE);

    private AppClock() {
        throw new UnsupportedOperationException("Utility class");
    }

    public static Clock clock() {
        return CLOCK;
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }

    public static LocalDate today() {
        return LocalDate.now(CLOCK);
    }
}
