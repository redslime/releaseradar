package xyz.redslime.releaseradar

import com.google.gson.GsonBuilder
import dev.kord.common.entity.ChannelType
import org.apache.logging.log4j.LogManager
import xyz.redslime.releaseradar.command.Command
import xyz.redslime.releaseradar.data.Cache
import xyz.redslime.releaseradar.data.Config
import xyz.redslime.releaseradar.data.Database
import xyz.redslime.releaseradar.util.InteractionManager
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * @author redslime
 * @version 2023-05-18
 */

val commands: ArrayList<Command> = ArrayList()
val allowedChannels = mutableListOf(ChannelType.GuildText, ChannelType.GuildNews, ChannelType.PublicGuildThread, ChannelType.PublicNewsThread, ChannelType.PrivateThread)
var startedAt = 0L

val interactionManager = InteractionManager()
lateinit var config: Config
lateinit var discord: DiscordClient
lateinit var db: Database
lateinit var cache: Cache
lateinit var spotify: SpotifyClient
lateinit var webServer: WebServer

suspend fun main() {
    config = readConfig()
    cache = Cache()
    db = Database(cache, config.dbHost, config.dbUser, config.dbPassword)
    spotify = SpotifyClient(config.spotifyClientId, config.spotifySecret).login()
    webServer = WebServer()

    thread {
        webServer.start()
    }

    discord = DiscordClient().create()
}

private fun readConfig(): Config {
    val configFile = File("config.json")

    if (!configFile.exists()) {
        // Todo this doesnt work lmao
        val bytes = DiscordClient.Companion::class.java.getResource("config.json")?.readBytes()
        ByteArrayInputStream(bytes).copyTo(FileOutputStream(configFile))
        LogManager.getLogger({}.javaClass).error("Please fill in the credentials in config.json")
        exitProcess(0)
    }

    val config = GsonBuilder().create().fromJson(FileInputStream(configFile).bufferedReader(), Config::class.java)
    validateConfig(config)

    return config
}

private fun validateConfig(config: Config) {
    if(config.discordToken.isBlank())
        throw IllegalStateException("Please fill in discordToken in config.json")
    if(config.spotifyClientId.isBlank())
        throw IllegalStateException("Please fill in spotifyClientId in config.json")
    if(config.spotifySecret.isBlank())
        throw IllegalStateException("Please fill in spotifySecret in config.json")
    if(config.dbDriverName.isBlank())
        throw IllegalStateException("Please fill in dbDriverName in config.json")
    if(config.dbHost.isBlank())
        throw IllegalStateException("Please fill in dbHost in config.json")
    if(config.dbUser.isBlank())
        throw IllegalStateException("Please fill in dbUser in config.json")
    if(config.dbPassword.isBlank())
        throw IllegalStateException("Please fill in dbPassword in config.json")
}