package xyz.redslime.releaseradar.command

import dev.kord.common.entity.ButtonStyle
import dev.kord.common.entity.Snowflake
import dev.kord.core.behavior.channel.MessageChannelBehavior
import dev.kord.core.behavior.interaction.response.createPublicFollowup
import dev.kord.core.entity.interaction.ChatInputCommandInteraction
import dev.kord.core.entity.interaction.followup.PublicFollowupMessage
import dev.kord.rest.builder.interaction.ChatInputCreateBuilder
import dev.kord.rest.builder.message.actionRow
import dev.kord.rest.builder.message.embed
import xyz.redslime.releaseradar.*
import xyz.redslime.releaseradar.util.ChunkedString
import xyz.redslime.releaseradar.util.plural
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2024-02-01
 */
class DuplicatesCommand: Command("duplicates", "Find duplicated artists on radars", perm = PermissionLevel.CONFIG_CHANNEL) {

    override fun addParameters(builder: ChatInputCreateBuilder) {
        addChannelInput(builder, "The radar channel to compare to all other radars", req = false)
    }

    override suspend fun handleInteraction(interaction: ChatInputCommandInteraction) {
        val re = interaction.deferPublicResponse()
        val channel = getChannelInput(interaction)

        if(channel != null && !checkRadarChannel(cache.getRadarId(channel.getDbId()), re, channel))
            return

        val serverId = interaction.data.guildId.value?.asLong()!!
        val map: Map<String, List<Int>> = if(channel == null) {
            cache.getDuplicatedArtistsInServer(serverId)
        } else {
            cache.getDuplicatedArtistsInChannel(serverId, channel.id.asLong())
        }

        if(map.isEmpty()) {
            respondSuccessEmbed(re, "No duplicate artists found!")
            return
        }

        val desc = ChunkedString()
        val artists = spotify.api { api ->
            api.artists.getArtists(*map.keys.toTypedArray()).filterNotNull().associateBy { it.id }
        }

        map.forEach { (artistId, radarIds) ->
            val artist = artists[artistId]
            val chs = radarIds.mapNotNull { cache.getChannelId(it) }
                .mapNotNull { interaction.kord.getChannel(Snowflake(it)) }
                .map { it as MessageChannelBehavior }
                .map { it.mention }

            desc.add("${artist?.name}: ${chs.joinToString(" ")}")
        }

        val messageParts = mutableListOf<PublicFollowupMessage>()
        val rep = desc.chunked({ first ->
            respondSuccessEmbed(re) {
                title = if(channel == null) {
                    "Duplicated ${plural("artist", map.size)} (${map.size}):"
                } else {
                    "Duplicated ${plural("artist", map.size)} from ${channel.mention} (${map.size}):"
                }

                description = first
            }
        }, { _, first, chunk ->
            messageParts.add(first.createPublicFollowup {
                embed {
                    this.description = chunk
                }
            })
        })

        if(channel != null) {
            rep.createPublicFollowup {
                successEmbed("Take cleanup measures (optional)", "You can automatically remove duplicates below:")
                actionRow {
                    addInteractionButton(this, ButtonStyle.Secondary, "Remove from #${channel.name}") {
                        val repp = it.deferPublicResponse()
                        val list = artists.values.map { a -> a.toSimpleArtist() }

                        db.removeArtistsFromRadar(list, channel)
                        it.message.delete()
                        respondSuccessEmbed(repp, "Removed ${pluralPrefixed("duplicate", map.size)} from ${channel.mention}")
                    }
                    addInteractionButton(this, ButtonStyle.Secondary, "Remove from other channels") {
                        val repp = it.deferPublicResponse()
                        val channelMap = mutableMapOf<Int, MutableList<String>>()

                        map.forEach { (artistId, radarIds) ->
                            radarIds.forEach { radarId ->
                                channelMap.getOrPut(radarId) { mutableListOf() }.add(artistId)
                            }
                        }

                        channelMap.remove(cache.getRadarId(channel.getDbId()))
                        channelMap.forEach { (radarId, artistIds) ->
                            db.removeArtistIdsFromRadar(artistIds, radarId)
                        }

                        it.message.delete()
                        respondSuccessEmbed(repp, "Removed ${pluralPrefixed("duplicate", map.size)} from other channels")
                    }
                    addInteractionButton(this, ButtonStyle.Danger, "Cancel") {
                        it.message.delete()
                    }
                }
            }
        }
    }
}