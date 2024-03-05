package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.PermissionLevel
import xyz.redslime.releaseradar.asLong
import xyz.redslime.releaseradar.cache
import xyz.redslime.releaseradar.success
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-21
 */
class FindCommand: ArtistCommand("find", "Lists all radars the specified artist is on", singleOnly = true, perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        // nothing here hehe
    }

    override suspend fun handleArtist(
        artist: Artist,
        response: DeferredMessageInteractionResponseBehavior,
        interaction: ChatInputCommandInteraction
    ) {
        var count = 0
        var channels = ""
        cache.getServerRadarsWithArtist(artist, interaction.getChannel().data.guildId.value?.asLong()!!).forEach { radar ->
            cache.getChannelId(radar)?.also {
                interaction.kord.getChannel(Snowflake(it))?.also { ch ->
                    count++
                    channels += "${ch.mention}\n"
                }
            }
        }

        response.respond {
            embed {
                success()
                title = "${artist.name} is on ${pluralPrefixed("radar", count)}"
                description = channels
            }
        }
    }

    override suspend fun handleArtists(
        artists: List<Artist>,
        response: DeferredMessageInteractionResponseBehavior,
        unresolved: List<String>,
        interaction: ChatInputCommandInteraction
    ) {

    }
}