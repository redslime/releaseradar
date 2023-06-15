package xyz.redslime.releaseradar.command

import dev.kord.common.entity.Snowflake
import dev.kord.common.entity.optional.value
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.createEphemeralFollowup
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.modify.embed
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.addPostLater
import xyz.redslime.releaseradar.util.extractSpotifyLink
import xyz.redslime.releaseradar.util.getStartOfToday

/**
 * @author redslime
 * @version 2023-06-15
 */
class CollectRemindersCommand: AdminCommand("collectreminders", "Looks for reminders globally") {

    override fun addParameters(builder: ChatInputCreateBuilder) {

    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val today = getStartOfToday()
        val alarmEmoji = ReactionEmoji.Unicode("\u23F0")
        var added = 0
        val re = interaction.deferEphemeralResponse().respond {
            embed {
                success()
                title = "Starting collecting reminder reactions"
            }
        }

        cache.getAllActiveRadars().forEach { id ->
            interaction.kord.getChannel(Snowflake(id))?.let { ch ->
                ch.asChannelOrNull()?.let { channel ->
                    if(channel is MessageChannelBehavior) {
                        channel.data.lastMessageId.value?.let { lastMessage ->
                            channel.getMessagesBefore(lastMessage)
                                .takeWhile { it.timestamp > today }
                                .filter { it.author == interaction.kord.getSelf() }
                                .toList()
                                .forEach { message ->
                                    extractSpotifyLink(message)?.let { url ->
                                        message.getReactors(alarmEmoji)
                                            .filter { it != interaction.kord.getSelf() }
                                            .toList().forEach {
                                                if(addPostLater(url, it))
                                                    added++
                                        }
                                    }
                                }
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