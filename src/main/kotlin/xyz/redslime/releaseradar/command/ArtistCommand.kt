package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.Artist
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.modify.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.exception.TooManyNamesException
import xyz.redslime.releaseradar.util.NameCacheProvider

/**
 * @author redslime
 * @version 2023-05-19
 */
abstract class ArtistCommand(name: String, description: String, perm: PermissionLevel = PermissionLevel.EVERYONE, val ephemeral: Boolean = false, val singleOnly: Boolean = false, val artistLimit: Int = 20) : Command(name, description, perm) {

    abstract fun addParams(builder: ChatInputCreateBuilder)

    abstract suspend fun handleArtist(artist: Artist, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction)

    abstract suspend fun handleArtists(artists: List<Artist>, response: DeferredMessageInteractionResponseBehavior, unresolved: List<String>, interaction: ChatInputCommandInteraction)

    override fun addParameters(builder: ChatInputCreateBuilder) {
        var desc = "The artist name. Artist URL is accepted too."

        if(!singleOnly)
            desc += " Multiple names can be comma seperated."

        builder.string("artist", desc) {
            required = true
        }
        addParams(builder)
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val response = if(ephemeral) interaction.deferEphemeralResponse() else interaction.deferPublicResponse()
        val cmd = interaction.command
        val artists: Map<String, Artist?>

        try {
            artists = spotify.findArtists(getNameCacheProvider(interaction), cmd.strings["artist"]!!, artistLimit = artistLimit)
        } catch (exc: TooManyNamesException) {
            response.respond {
                embed {
                    error()
                    title = "Too many names in request"
                    description = "Please limit yourself to ${exc.limit} names per request"
                    footer {
                        text = "You can bypass the limit by strictly providing artist urls"
                    }
                }
            }
            return
        }
        if(artists.isEmpty()) {
            if(isCustomHandle())
                handleInput(cmd.strings["artist"]!!, response, interaction)
            else
                respondErrorEmbed(response, "No artist found")
        } else if(artists.size == 1) {
            val entry = artists.entries.first()

            if(entry.value == null) {
                if(isCustomHandle()) {
                    handleInput(cmd.strings["artist"]!!, response, interaction)
                } else {
                    response.respond {
                        embed { buildNoSuchArtist(entry.key, this) }
                    }
                }
            } else {
                handleArtist(entry.value!!, response, interaction)
            }
        } else {
            if(singleOnly) {
                respondErrorEmbed(response, "This command only works with one artist, sorry!")
                return
            }

            val list = ArrayList<Artist>()
            val unresolved = ArrayList<String>()

            artists.forEach {
                if(it.value == null) {
                    unresolved.add(it.key)
                } else {
                    list.add(it.value!!)
                }
            }

            handleArtists(list, response, unresolved, interaction)
        }
    }

    open fun getNameCacheProvider(interaction: ChatInputCommandInteraction): NameCacheProvider {
        return cache
    }

    open fun isCustomHandle(): Boolean {
        return false
    }

    open suspend fun handleInput(str: String, response: DeferredMessageInteractionResponseBehavior, interaction: ChatInputCommandInteraction) {

    }
}