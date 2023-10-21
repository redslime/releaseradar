package xyz.redslime.releaseradar.util

import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import xyz.redslime.releaseradar.command.ReminderPlaylistCommand
import xyz.redslime.releaseradar.commands
import kotlin.coroutines.CoroutineContext

/**
 * @author redslime
 * @version 2023-09-22
 */
class InteractionManager {

    val buttons: HashMap<String, suspend CoroutineContext.(ButtonInteraction) -> Unit> = HashMap()
    val staticButtons: HashMap<String, suspend CoroutineContext.(ButtonInteraction) -> Unit> = HashMap()
    val selectors: HashMap<String, suspend CoroutineContext.(SelectMenuInteraction) -> Unit> = HashMap()

    init {
        staticButtons["reminderplaylist"] = { i ->
            commands.firstOrNull { it.name == "reminderplaylist" }?.let { (it as ReminderPlaylistCommand).sendPrompt(i) }
        }
    }
}