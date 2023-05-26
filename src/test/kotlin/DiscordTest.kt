import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.core.entity.channel.TextChannel
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.apache.logging.log4j.LogManager

/**
 * @author redslime
 * @version 2023-05-22
 */

private const val discordToken = "" // waffle

suspend fun main() {
    val logger = LogManager.getLogger()
    val client = Kord(discordToken)
    client.login {
        logger.info("Test bot logged in!")
        @OptIn(PrivilegedIntent::class)
        intents += Intent.MessageContent

        val channel = client.getChannel(Snowflake(1108804483938525325L)) as TextChannel
        val custom = "<a:SCgivemeattentionNOW:849297210448674836>"

    }
}