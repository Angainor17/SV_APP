package su.sv.commonui.ext

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun Long.toLocalDateTime(): LocalDateTime {
    return Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault()) // default zone
        .toLocalDateTime(); // returns actual LocalDateTime object
}
