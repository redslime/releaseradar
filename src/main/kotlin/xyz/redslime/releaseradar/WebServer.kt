package xyz.redslime.releaseradar

import com.adamratzman.spotify.SpotifyScope
import com.adamratzman.spotify.SpotifyUserAuthorization
import com.adamratzman.spotify.getSpotifyAuthorizationUrl
import com.adamratzman.spotify.spotifyClientApi
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.html.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.css.*
import kotlinx.html.*
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.util.*
import kotlin.collections.set

/**
 * @author redslime
 * @version 2023-06-16
 */
class WebServer {

    val logger: Logger = LogManager.getLogger(javaClass)
    val authConsumers = mutableMapOf<String, suspend String?.() -> Unit>()
    var enabled = true

    fun start() {
        if(config.redirectUrl.isBlank()) {
            logger.info("Not starting web service, no redirect url in config specified")
            enabled = false
            return
        }

        embeddedServer(Netty, host = "0.0.0.0", port = config.serverPort, module = Application::module).start(wait = true)
    }

    fun getAuthUrl(public: Boolean, consumer: suspend String?.() -> Unit): String {
        val state = UUID.randomUUID().toString()
        authConsumers[state] = consumer

        return getSpotifyAuthorizationUrl(
//            scopes = arrayOf((if(public) SpotifyScope.PlaylistModifyPublic else SpotifyScope.PlaylistModifyPrivate)),
            scopes = arrayOf(SpotifyScope.PlaylistModifyPublic, SpotifyScope.PlaylistReadCollaborative, SpotifyScope.PlaylistReadPrivate, SpotifyScope.PlaylistModifyPrivate), // spotify api being silly: https://community.spotify.com/t5/Spotify-for-Developers/Web-API-Adding-tracks-to-a-public-playlist-requires-playlist/td-p/5913347
            clientId = config.spotifyClientId,
            redirectUri = config.redirectUrl,
            state = state)
    }
}

fun Application.module() {
    routing {
        get("/") {
            val code = call.parameters["code"]
            val state = call.parameters["state"]
            val consumer = webServer.authConsumers[state]

            if(consumer != null) {
                try {
                    val api = spotifyClientApi(
                        clientId = config.spotifyClientId,
                        clientSecret = config.spotifySecret,
                        redirectUri = config.redirectUrl,
                        authorization = SpotifyUserAuthorization(authorizationCode = code)
                    ).build()

                    api.shutdown()
                    consumer.invoke(api.token.refreshToken)

                    call.respondHtml(HttpStatusCode.OK) {
                        head {
                            link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                            title {
                                +"Linked Successfully!"
                            }
                        }
                        body {
                            div("card") {
                                h1 {
                                    +"✅ Spotify linked successfully!"
                                }
                                p {
                                    +"You can now close this tab"
                                }
                            }
                        }
                    }
                } catch (ex: Exception) {
                    webServer.logger.error("Error while trying to creating user client for state $state", ex)
                    consumer.invoke(null)
                    call.respondText(ex.toString())
                } finally {
                    webServer.authConsumers.remove(state)
                }
            } else {
                call.respondHtml(HttpStatusCode.OK) {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                        title {
                            +"Invalid request"
                        }
                    }
                    body {
                        div("card") {
                            h1 {
                                +"❌ Failed to link Spotify"
                            }
                            p {
                                +"Please get a new link via the bot on Discord"
                            }
                        }
                    }
                }
            }
        }

        get("/styles.css") {
            call.respondCss {
                body {
                    backgroundColor = Color("#25262a")
                    margin(0.px)
                    fontFamily = "\"Open sans\", arial, serif"
                }
                rule(".card") {
                    backgroundColor = Color("#485160")
                    margin(5.rem, 20.pct, 0.px, 20.pct)
                    padding(2.rem)
                    color = Color.whiteSmoke
                }
                rule(".card h1") {
                    color = Color.white
                }
            }
        }
    }
}