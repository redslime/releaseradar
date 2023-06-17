package xyz.redslime.releaseradar

import dev.kord.core.Kord
import dev.kord.gateway.Intent
import dev.kord.gateway.PrivilegedIntent
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.command.*
import xyz.redslime.releaseradar.listener.*
import xyz.redslime.releaseradar.task.PostLaterTask
import xyz.redslime.releaseradar.task.ScanNewTracksTask

/**
 * @author redslime
 * @version 2023-05-25
 */
class DiscordClient {

    private lateinit var client: Kord

    companion object {
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
//        LinkSpotifyCommand().register(client)
        ReminderPlaylistCommand().register(client)
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