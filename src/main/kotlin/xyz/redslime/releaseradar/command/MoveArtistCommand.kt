package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import com.adamratzman.spotify.models.SimpleArtist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2025-01-27
 */
class MoveArtistCommand: ArtistCommand("move", "Move an artist from on radar to another", PermissionLevel.CONFIG_CHANNEL) {

    override fun addParams(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "Radar channel to move artist from", name = "from")
        addChannelInput(builder, "Radar channel to move artist to", name = "to")
    }

    override suspend fun handleArtist(
        artist: Artist,
        response: DeferredMessageInteractionResponseBehavior,
        interaction: ChatInputCommandInteraction,
    ) {
        val from = interaction.command.channels["from"]!!
        val ridFrom = cache.getRadarId(from.getDbId())
        val to = interaction.command.channels["to"]!!
        val ridTo = cache.getRadarId(to.getDbId())

        if(from.id == to.id) {
            respondErrorEmbed(response, "Channels cannot be the same!")
            return
        }

        if(!checkRadarChannel(ridFrom, response, from) || !checkRadarChannel(ridTo, response, to)
            || ridTo == null || ridFrom == null)
            return

        if(cache.getArtistRecordsInRadarChannel(from).any { it.id == artist.id }) {
            if(db.removeArtistFromRadar(artist, ridFrom)) {
                if(db.addArtistToRadar(artist, ridTo)) {
                    respondSuccessEmbed(response) {
                        description = "Moved from ${from.mention} to ${to.mention}"
                        artistTitle(artist)
                    }
                } else {
                    respondSuccessEmbed(response) {
                        description = "Moved from ${from.mention} to ${to.mention}"
                        footer("Artist was already on radar")
                        artistTitle(artist)
                    }
                }
            } else {
                respondErrorEmbed(response, desc = "There is no artist named ${artist.name} on the radar in ${from.mention}")
            }
        } else {
            respondErrorEmbed(response, desc = "There is no artist named ${artist.name} on the radar in ${from.mention}")
        }
    }

    override suspend fun handleArtists(
        artists: List<Artist>,
        response: DeferredMessageInteractionResponseBehavior,
        unresolved: List<String>,
        interaction: ChatInputCommandInteraction,
    ) {
        val from = interaction.command.channels["from"]!!
        val ridFrom = cache.getRadarId(from.getDbId())
        val to = interaction.command.channels["to"]!!
        val ridTo = cache.getRadarId(to.getDbId())

        if(!checkRadarChannel(ridFrom, response, from) || !checkRadarChannel(ridTo, response, to)
            || ridFrom == null || ridTo == null)
            return

        val artistList = artists.map { it.toSimpleArtist() }
        val pool = cache.getArtistRecordsInRadarChannel(from)
        val map = mutableMapOf<SimpleArtist, Result>()

        artistList.filter { artist ->
            // init fallback value
            map[artist] = Result.NOT_IN_CHANNEL

            // artist is in source channel, try to move in next step
            pool.any { it.id == artist.id }
        }.also { toMove ->
            // let's assume the move was successful
            toMove.forEach { map[it] = Result.SUCCESS }

            db.removeArtistsFromRadar(toMove, from)
            db.addArtistsToRadar(toMove, ridTo).forEach { skipped ->
                map[skipped] = Result.SKIPPED
            }
        }

        val actualMoved = map.values.count { it != Result.NOT_IN_CHANNEL }

        respondEmbed(response) {
            colorize(actualMoved, artistList.size)
            title = "Moved ${pluralPrefixed("artist", actualMoved)} from ${from.mention} to ${to.mention}:"
            description = ""

            map.forEach { (artist, result) ->
                description += "${result.friendly.format(artist.name)}\n"
            }
        }
    }

    private enum class Result(val friendly: String) {
        SUCCESS(":white_check_mark: **%s**"),
        SKIPPED(":white_check_mark: **%s** (was already on radar)"),
        NOT_IN_CHANNEL(":x: **%s** not found on radar")
    }
}