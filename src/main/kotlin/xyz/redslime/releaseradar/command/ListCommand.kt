package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.message.modify.embed
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.warning

/**
 * @author redslime
 * @version 2023-05-19
 */
class ListCommand : Command("list", "Lists all artists on a radar", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel to list. If not supplied, the current channel is listed", false)
        builder.boolean("ids", "Whether to include artist ids") {
            required = false
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferPublicResponse()
        val channel = interaction.command.channels["channel"] ?: interaction.channel.asChannel()
        val ids = interaction.command.booleans["ids"] ?: false
        val artists = cache.getArtistNamesInRadarChannel(channel)

        response.respond {
            if(artists.isEmpty()) {
                embed {
                    warning()
                    title = "There are no artists on radar in ${channel.mention}"
                    description = "Add artists using the /add command!"
                }
            } else {
                val joined: String = if(ids)
                    artists.map { "${it.key} ([``${it.value}``](https://open.spotify.com/artist/${it.value}))" }.joinToString("\n")
                else
                    artists.map { it.key }.joinToString("\n")

                embed {
                    success()
                    title = "Artists on radar in ${channel.mention} (${artists.size}):"

                    if(joined.length <= 4000)
                        description = joined
                }

                if(joined.length > 4000) {
                    val text: String = if(ids)
                        artists.map { "${it.key} (${it.value})" }.joinToString("\n")
                    else
                        joined
                    addFile("artists.txt", ChannelProvider(null) { ByteReadChannel(text) })
                }
            }
        }
    }
}