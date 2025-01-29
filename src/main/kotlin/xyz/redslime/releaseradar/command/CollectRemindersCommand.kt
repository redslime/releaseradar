package xyz.redslime.releaseradar.command

import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.util.addPostLater
import xyz.redslime.releaseradar.util.extractSpotifyLink
import xyz.redslime.releaseradar.util.getStartOfToday
import xyz.redslime.releaseradar.util.reminderEmoji

/**
 * @author redslime
 * @version 2023-06-15
 */
class CollectRemindersCommand: AdminCommand("collectreminders", "Looks for reminders globally") {

    private val logger = LogManager.getLogger(javaClass)

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val today = getStartOfToday()
        var added = 0

        val re = respondSuccessEmbed(interaction.deferEphemeralResponse(), "Starting collecting reminder reactions")

        cache.getAllActiveRadars().forEach { id ->
            interaction.kord.getChannel(Snowflake(id))?.asChannelOrNull()?.let { channel ->
                if(channel is MessageChannelBehavior) {
                    channel.data.lastMessageId.value?.let { lastMessage ->
                        try {
                            channel.getMessagesBefore(lastMessage)
                                .takeWhile { it.timestamp > today }
                                .filter { it.author == interaction.kord.getSelf() }
                                .toList()
                                .forEach { message ->
                                    extractSpotifyLink(message)?.let { url ->
                                        message.getReactors(reminderEmoji)
                                            .filter { it != interaction.kord.getSelf() }
                                            .toList().forEach {
                                                if (addPostLater(url, it))
                                                    added++
                                            }
                                    }
                                }
                        } catch (ex: Exception) {
                            logger.error("Failed to get last messages in $id", ex)
                        }
                    }
                }
            }
        }

        re.createEphemeralFollowup {
             content = "Added $added reminders"
        }
    }
}