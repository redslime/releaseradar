package xyz.redslime.releaseradar

import dev.kord.common.entity.Snowflake
import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.command.*
import xyz.redslime.releaseradar.listener.*
import xyz.redslime.releaseradar.task.PostLaterTask
import xyz.redslime.releaseradar.task.ScanNewTracksTask
import xyz.redslime.releaseradar.util.pluralPrefixed

/**
 * @author redslime
 * @version 2023-05-25
 */
class DiscordClient {

    private lateinit var client: Kord

    companion object {
        var disconnectedGuilds = mutableListOf<Snowflake>()
        lateinit var scanTask: ScanNewTracksTask
        lateinit var postLaterTask: PostLaterTask
    }

    suspend fun create(): DiscordClient {
        client = Kord(config.discordToken)

        registerCommands()
        registerListeners()
        registerTasks()

        client.login {
            startedAt = System.currentTimeMillis()
            LogManager.getLogger(javaClass).info("Bot logged in!")

            @OptIn(PrivilegedIntent::class)
            intents += Intent.MessageContent

            this.presence {
                listening(pluralPrefixed("new release", postLaterTask.getUniqueAlbumReminders()))
            }
        }
        return this
    }

    private suspend fun registerCommands() {
        // possibly register those via reflection when list gets long
        AddArtistCommand().register(client)
        RemoveArtistCommand().register(client)
        LatestCommand().register(client)
        ListCommand().register(client)
        SetConfigChannelCommand().register(client)
        ImportCommand().register(client)
        ClearCommand().register(client)
        RadarsCommand().register(client)
        PrintCommand().register(client)
        FindCommand().register(client)
        ScanCommand().register(client)
        GlobalStatsCommand().register(client)
        StopCommand().register(client)
        MyTimezoneCommand().register(client)
        SetTimezoneCommand().register(client)
        EmbedTypeCommand().register(client)
        SetReactionsCommand().register(client)
        CollectRemindersCommand().register(client)
        LinkMasterSpotifyCommand().register(client)
        LinkSpotifyCommand().register(client)
        EnlistCommand().register(client)
        ReminderPlaylistCommand().register(client)
        ForceRemindersCommand().register(client)
        TopCommand().register(client)
        AnalysisCommand().register(client)
        RebuildCommand().register(client)
        PruneCommand().register(client)
        DuplicatesCommand().register(client)
        ExcludeArtistCommand().register(client)
        IncludeArtistCommand().register(client)
        ListExcludedCommand().register(client)
    }

    private fun registerListeners() {
        CommandListener().register(client)
        JoinListener().register(client)
        QuitListener().register(client)
        InteractionListener().register(client)
        ReactListener().register(client)
    }

    private fun registerTasks() {
        scanTask = ScanNewTracksTask().apply {
            start(client)
        }
        postLaterTask = PostLaterTask().apply {
            start(client)
        }
    }
}