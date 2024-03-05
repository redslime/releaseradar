package xyz.redslime.releaseradar.command

import com.sigpwned.emoji4j.core.GraphemeMatcher
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.GuildEmoji
import dev.kord.core.entity.ReactionEmoji
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import kotlinx.coroutines.flow.toList
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.emojiRegex

/**
 * @author redslime
 * @version 2023-05-26
 */
class SetReactionsCommand: Command("setreactions", "Set custom default reactions for new release posts", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel for which the default reactions should be set")
        builder.string("like", "An emoji for like (left)") {
            required = true
        }
        builder.string("dislike", "An emoji for dislike (middle)") {
            required = true
        }
        builder.string("heart", "An emoji for heart (right)") {
            required = true
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val channel = getChannelInput(interaction)!!
        val radarId = db.getRadarId(channel)

        channel.data.guildId.value?.let { guildId ->
            interaction.kord.getGuildOrNull(guildId)?.let { guild ->
                val response = interaction.deferPublicResponse()
                val guildEmojis = guild.emojis.toList()
                val like = parseEmoji(interaction.command.strings["like"]!!, guildEmojis)
                val dislike = parseEmoji(interaction.command.strings["dislike"]!!, guildEmojis)
                val heart = parseEmoji(interaction.command.strings["heart"]!!, guildEmojis)

                if(like != null && dislike != null && heart != null) {
                    val mentions = "${like.mention},${dislike.mention},${heart.mention}"
                    db.setRadarEmotes(radarId, mentions)

                    response.respond {
                        embed {
                            success()
                            title = "Updated default reactions in ${channel.mention}"
                            description = "Now reacting with ${like.mention} ${dislike.mention} ${heart.mention} by default"
                        }

                        actionRow {
                            addInteractionButton(this, ButtonStyle.Success, "Apply to all radars") {
                                it.deferPublicResponse().respond {
                                    db.setServerEmotes(guildId.asLong(), mentions)

                                    embed {
                                        success()
                                        title = "Updated default reactions for all radars"
                                        description = "Now reacting with ${like.mention} ${dislike.mention} ${heart.mention} by default everywhere"
                                    }
                                }
                                it.message.delete()
                            }
                        }
                    }
                } else {
                    response.respond {
                        embed {
                            error()
                            title = "Invalid emojis given!"
                            description = "Please make sure all customs emojis are from this server"
                        }
                    }
                }
            }
        }
    }

    private fun parseEmoji(str: String, guildEmojis: List<GuildEmoji>): ReactionEmoji? {
        if(str.matches(emojiRegex)) {
            val id = str.replace(emojiRegex, "$3").toLong()
            return guildEmojis.firstOrNull { it.id.asLong() == id }?.let { ReactionEmoji.Companion.from(it) }
        } else {
            val matcher = GraphemeMatcher(str)

            if(matcher.find())
                return ReactionEmoji.Unicode(str)
        }

        return null
    }
}