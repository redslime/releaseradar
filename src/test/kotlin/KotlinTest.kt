import xyz.redslime.releaseradar.util.Timezone
import java.time.ZonedDateTime

/**
 * @author redslime
 * @version 2023-05-24
 */

fun main() {
    Timezone.values().firstOrNull { ZonedDateTime.now(it.zone).hour == 0 }?.let { timezone ->
        println(timezone.toString())
    }
}