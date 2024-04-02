package xyz.redslime.releaseradar.command

import com.adamratzman.spotify.models.AudioFeatures
import com.adamratzman.spotify.models.Track
import com.adamratzman.spotify.utils.Market
import dev.kord.common.entity.ButtonStyle
import dev.kord.core.behavior.interaction.respondEphemeral
import dev.kord.core.behavior.interaction.response.DeferredMessageInteractionResponseBehavior
import dev.kord.core.behavior.interaction.response.respond
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.interaction.boolean
import dev.kord.rest.builder.interaction.string
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.formatPercentage
import xyz.redslime.releaseradar.interactionManager
import xyz.redslime.releaseradar.spotify
import xyz.redslime.releaseradar.util.*

/**
 * @author redslime
 * @version 2023-10-11
 */
class AnalysisCommand: Command("analysis", "Display track analysis data") {

    private val logger = LogManager.getLogger(javaClass)

    override fun addParameters(builder: ChatInputCreateBuilder) {
        builder.string("track", "Track URL") {
            required = true
        }
        builder.boolean("silent", "Send the analysis only visible to you") {
            required = false
        }

        interactionManager.staticButtons["analysis-desc"] = {
            it.respondEphemeral {
                embed {
                    title = "Audio Analysis Categories"

                    description = "\n\uD83C\uDFB8 **Acousticness**: A confidence measure from 0% to 100% of whether the track is acoustic. 100% represents high confidence the track is acoustic."
                    description += "\n\uD83D\uDD7A **Danceability**: Danceability describes how suitable a track is for dancing based on a combination of musical elements including tempo, rhythm stability, beat strength, and overall regularity. A value of 0% is least danceable and 100% is most danceable."
                    description += "\n⏱\uFE0F **Duration**: The duration of the track."
                    description += "\n\uD83D\uDCA5 **Energy**: Energy is a measure from 0% to 100% and represents a perceptual measure of intensity and activity. Typically, energetic tracks feel fast, loud, and noisy. For example, death metal has high energy, while a Bach prelude scores low on the scale. Perceptual features contributing to this attribute include dynamic range, perceived loudness, timbre, onset rate, and general entropy."
                    description += "\n\uD83C\uDFBA **Instrumentalness**: Predicts whether a track contains no vocals. \"Ooh\" and \"aah\" sounds are treated as instrumental in this context. Rap or spoken word tracks are clearly \"vocal\". The closer the instrumentalness value is to 100%, the greater likelihood the track contains no vocal content. Values above 50% are intended to represent instrumental tracks, but confidence is higher as the value approaches 100%."
                    description += "\n\uD83C\uDFB5 **Key:** The key the track is in."
                    description += "\n\uD83C\uDFA4 **Liveness**: Detects the presence of an audience in the recording. Higher liveness values represent an increased probability that the track was performed live. A value above 80% provides strong likelihood that the track is live."
                    description += "\n\uD83D\uDD0A **Loudness**: The overall loudness of a track in decibels (dB). Loudness values are averaged across the entire track and are useful for comparing relative loudness of tracks. Loudness is the quality of a sound that is the primary psychological correlate of physical strength (amplitude). Values typically range between -60 and 0 db."
                    description += "\n⚖\uFE0F **Mode**: Mode indicates the modality (major or minor) of a track, the type of scale from which its melodic content is derived."
                    description += "\n\uD83C\uDF99\uFE0F **Speechiness**: Speechiness detects the presence of spoken words in a track. The more exclusively speech-like the recording (e.g. talk show, audio book, poetry), the closer to 100% the attribute value. Values above 66% describe tracks that are probably made entirely of spoken words. Values between 33% and 66% describe tracks that may contain both music and speech, either in sections or layered, including such cases as rap music. Values below 33% most likely represent music and other non-speech-like tracks."
                    description += "\n\uD83C\uDF9A\uFE0F **Tempo**: The overall estimated tempo of a track in beats per minute (BPM). In musical terminology, tempo is the speed or pace of a given piece and derives directly from the average beat duration."
                    description += "\n\uD83C\uDFBC **Time signature**: An estimated time signature. The time signature (meter) is a notational convention to specify how many beats are in each bar (or measure)."
                    description += "\n\uD83D\uDE04 **Valence**: A measure from 0% to 100% describing the musical positiveness conveyed by a track. Tracks with high valence sound more positive (e.g. happy, cheerful, euphoric), while tracks with low valence sound more negative (e.g. sad, depressed, angry)."
                }
            }
        }
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val silent = interaction.command.booleans["silent"] ?: false
        val response = if(silent) {
            interaction.deferEphemeralResponse()
        } else {
            interaction.deferPublicResponse()
        }
        val input = interaction.command.strings["track"]!!
        val url = if(input.matches(shortLinkRegex)) {
            resolveShortenedLink(input, logger)
        } else {
            input
        }

        if(url == null) {
            respondErrorEmbed(response, ":x: Invalid URL given!")
            return
        }

        if(url.matches(albumRegex)) {
            val id = url.replace(albumRegex, "$1")
            val album = spotify.api { api ->
                api.albums.getAlbum(id, Market.WS)
            }

            if(album == null) {
                respondErrorEmbed(response, ":x: Failed to lookup album for the given URL!")
                return
            } else {
                if(album.totalTracks > 1) {
                    respondErrorEmbed(response, ":x: This album has multiple tracks. Please retry with a track URL!")
                    return
                } else {
                    album.tracks.firstOrNull()?.let { track ->
                        val analysis = spotify.api { api ->
                            api.tracks.getAudioFeatures(track.id)
                        }
                        respondAnalysis(response, track.toFullTrack(Market.WS)!!, analysis)
                    }
                }
            }
        } else if(url.matches(trackRegex)) {
            val id = url.replace(trackRegex, "$1")
            val track = spotify.api { api ->
                api.tracks.getTrack(id, Market.WS)
            }

            if(track == null) {
                respondErrorEmbed(response, ":x: Failed to lookup track for the given URL!")
            } else {
                val analysis = spotify.api { api ->
                    api.tracks.getAudioFeatures(track.id)
                }
                respondAnalysis(response, track, analysis)
            }
        } else {
            val track = spotify.api { api ->
                api.search.searchTrack(url).firstOrNull()
            }

            if(track == null) {
                respondErrorEmbed(response, ":x: Failed to find track!")
            } else {
                val analysis = spotify.api { api ->
                    api.tracks.getAudioFeatures(track.id)
                }
                respondAnalysis(response, track, analysis)
            }
        }
    }

    private suspend fun respondAnalysis(response: DeferredMessageInteractionResponseBehavior, track: Track, analysis: AudioFeatures) {
        val artists = track.artists.filter { it.name != null }.joinToString(" & ") { it.name!! }
        val name = track.name

        response.respond {
            embed {
                title = "Audio Analysis\n$artists - $name"
                thumbnail {
                    url = track.album.images?.get(0)?.url ?: ""
                }

                description = "\n\uD83C\uDFB8 **Acousticness**: ${analysis.acousticness.formatPercentage()}"
                description += "\n\uD83D\uDD7A **Danceability**: ${analysis.danceability.formatPercentage()}"
                description += "\n⏱\uFE0F **Duration**: ${formatMilliseconds(analysis.durationMs)}"
                description += "\n\uD83D\uDCA5 **Energy**: ${analysis.energy.formatPercentage()}"
                description += "\n\uD83C\uDFBA **Instrumentalness**: ${analysis.instrumentalness.formatPercentage()}"
                description += "\n\uD83C\uDFB5 **Key**: ${transformKey(analysis.key)}"
                description += "\n\uD83C\uDFA4 **Liveness**: ${analysis.liveness.formatPercentage()}"
                description += "\n\uD83D\uDD0A **Loudness**: ${analysis.loudness}db"
                description += "\n⚖\uFE0F **Mode**: ${ if(analysis.mode == 1) "Major" else "Minor" }"
                description += "\n\uD83C\uDF99\uFE0F **Speechiness**: ${analysis.speechiness.formatPercentage()}"
                description += "\n\uD83C\uDF9A\uFE0F **Tempo**: ${analysis.tempo} bpm"
                description += "\n\uD83C\uDFBC **Time signature**: ${analysis.timeSignature}/4"
                description += "\n\uD83D\uDE04 **Valence**: ${analysis.valence.formatPercentage()}"
            }

            actionRow {
                addStaticInteractionButton("analysis-desc", this, ButtonStyle.Secondary, "Category explanations")
            }
        }
    }

    private fun transformKey(key: Int): String {
        // https://en.wikipedia.org/wiki/Pitch_class
        return when(key) {
            0 -> "C (also B♯, Ddouble flat)"
            1 -> "C♯, D♭ (also Bdouble sharp)"
            2 -> "D (also Cdouble sharp, Edouble flat)"
            3 -> "D♯, E♭ (also Fdouble flat)"
            4 -> "E (also Ddouble sharp, F♭)"
            5 -> "F (also E♯, Gdouble flat)"
            6 -> "F♯, G♭ (also Edouble sharp)"
            7 -> "G (also Fdouble sharp, Adouble flat)"
            8 -> "G♯, A♭"
            9 -> "A (also Gdouble sharp, Bdouble flat)"
            10 -> "A♯, B♭ (also Cdouble flat)"
            11 -> "B (also Adouble sharp, C♭)"
            else -> "Unknown"
        }
    }
}