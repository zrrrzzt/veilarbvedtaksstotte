package no.nav.veilarbvedtaksstotte.utils

import java.time.*

object TimeUtils {
    @JvmStatic
    fun toLocalDateTime(zonedDateTime: ZonedDateTime): LocalDateTime {
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    @JvmStatic
    fun toLocalDate(zonedDateTime: ZonedDateTime): LocalDate {
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDate()
    }

    @JvmStatic
    fun toZonedDateTime(localDateTime: LocalDateTime): ZonedDateTime {
        return ZonedDateTime.of(localDateTime, ZoneId.systemDefault())
    }

    @JvmStatic
    fun toInstant(localDateTime: LocalDateTime): Instant {
        return toZonedDateTime(localDateTime).toInstant()
    }
}
