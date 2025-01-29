package xyz.redslime.releaseradar.command

import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.message.embed
import io.ktor.client.request.forms.*
import io.ktor.utils.io.*
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.successEmbed
import xyz.redslime.releaseradar.warning

/**
 * @author redslime
 * @version 2024-03-05
 */
class ListExcludedCommand: Command("listexcluded", "Lists all excluded artists from the specified radar", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel to list")
        builder.boolean("ids", "Whether to include artist ids") {
            required = false
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = interaction.deferPublicResponse()
        val channel = interaction.command.channels["channel"]!!
        val ids = interaction.command.booleans["ids"] ?: false
        val artists = cache.getExcludedArtistNamesFromRadarChannel(channel)

        response.respond {
            if(artists.isEmpty()) {
                embed {
                    warning()
                    title = "There are no artists excluded from the radar in ${channel.mention}"
                    description = "Exclude artists using the ``/exclude`` command!"
                }
            } else {
                val joined: String = if(ids)
                    artists.map { "${it.key} ([``${it.value}``](https://open.spotify.com/artist/${it.value}))" }.joinToString("\n")
                else
                    artists.map { it.key }.joinToString("\n")

                successEmbed("Excluded artists from radar in ${channel.mention} (${artists.size}):") {
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