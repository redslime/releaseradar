package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.db.releaseradar.tables.records.ArtistRecord
import xyz.redslime.releaseradar.util.ChunkedString
import xyz.redslime.releaseradar.util.NameCacheProvider
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-19
 */
class RemoveArtistCommand : ArtistCommand("remove", "Remove an artist from a release radar for the specified channel", PermissionLevel.CONFIG_CHANNEL, artistLimit = 50) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The channel the artist should be removed from")
    }

    override suspend fun handleArtist(artist: Artist, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val rid = db.getRadarId(channel)

        if(db.removeArtistFromRadar(artist, rid)) {
            respondSuccessEmbed(response, "Removed ${artist.name} from radar in ${channel.mention}")
        } else {
            respondErrorEmbed(response) {
                title = "There is no artist named ${artist.name} on the radar in ${channel.mention}"
                description = "Check who is on the radar using the ``/list`` command!"
            }
        }
    }

    override suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction) {
        val cmd = interaction.command
        val channel = cmd.channels["channel"]!!
        val description = ChunkedString()
        val simpleArtists = artists.map { it.toSimpleArtist() }
        val skipped = db.removeArtistsFromRadar(simpleArtists, channel)
        val removed = artists.size - skipped.size

        simpleArtists.forEach {  artist ->
            if(!skipped.contains(artist)) {
                description.add(":white_check_mark: **${artist.name}** ([``${artist.uri.id}``](${artist.externalUrls.spotify}))")
            } else {
                description.add(":x: **${artist.name}** not removed, not on radar?")
            }
        }

        unresolved.forEach {
            description.add(":x: **$it** not removed, failed to find artist")
        }

        description.chunked({ first ->
            respondEmbed(response) {
                colorize(removed, artists.size)
                this.title = "Removed ${pluralPrefixed("artist", removed)} from ${channel.mention}:"
                this.description = first
            }
        }, { _, first, chunk ->
            first.createPublicFollowup {
                embed {
                    this.description = chunk
                }
            }
        })
    }

    override fun getNameCacheProvider(interaction: ChatInputCommandInteraction): NameCacheProvider {
        return object : NameCacheProvider {
            override suspend fun findArtistRecByName(name: String, ignoreCase: Boolean): List<ArtistRecord> {
                // limit the pool of artists to search in to just this channel
                val channel = interaction.command.channels["channel"]!!
                return cache.getArtistRecordsInRadarChannel(channel)
                    .filter { it.name.equals(name, ignoreCase) }
            }
        }
    }
}