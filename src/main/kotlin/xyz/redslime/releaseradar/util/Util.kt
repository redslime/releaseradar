package xyz.redslime.releaseradar.util

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import org.apache.logging.log4j.Logger
import xyz.redslime.releaseradar.plural
import java.time.*

/**
 * @author redslime
 * @version 2023-05-19
 */

val emojiRegex = Regex("<(a)?:(.*):([0-9]*)>")

fun plural(str: String, count: Int): String {
    return str.plural(count)
}

fun pluralPrefixed(str: String, count: Int): String {
    return "$count ${str.plural(count)}"
}

fun getNextMidnightNZ(): ZonedDateTime {
    // this is actually midnight in Samoa (GMT-14 exists too but this is the earliest when songs become available)
    val date = LocalDate.now(ZoneId.of("Etc/GMT-13"))
    val time = LocalTime.MIDNIGHT
    return LocalDateTime.of(date, time).plusDays(1).atZone(ZoneId.of("Etc/GMT-13"))
}

fun getMillisUntilMidnightNZ(): Long {
    return getNextMidnightNZ().toInstant().toEpochMilli() - System.currentTimeMillis()
}

fun getMillisUntilTopOfTheHour(): Long {
    val now = LocalDateTime.now().atZone(ZoneId.of("UTC"))
    val date = LocalDateTime.now().atZone(ZoneId.of("UTC"))
        .plusHours(1).withMinute(0).withSecond(0).withNano(0)

    return date.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
}

suspend fun printToDiscord(client: Kord, logger: Logger, str: String) {
    logger.info(str)
    val channel = client.getChannel(Snowflake(1108804483938525325L)) as TextChannel
    channel.createMessage(str)
}