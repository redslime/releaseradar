package xyz.redslime.releaseradar.task

import dev.kord.core.Kord
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.prettyPrint
import java.time.Duration
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * @author redslime
 * @version 2023-05-19
 */
abstract class Task(private val delay: Duration = Duration.ZERO, private val period: Duration) {

    abstract fun run(client: Kord): TimerTask.() -> Unit

    fun start(client: Kord) {
        LogManager.getLogger(javaClass).info(javaClass.simpleName + " will be running in ${delay.prettyPrint()} every ${period.prettyPrint()}")
        fixedRateTimer(daemon = false,
            startAt = Date(System.currentTimeMillis() + delay.toMillis()),
            period = period.toMillis(),
            action = run(client))
    }
}