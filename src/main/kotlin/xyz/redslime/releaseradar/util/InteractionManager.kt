package xyz.redslime.releaseradar.util

import dev.kord.core.entity.interaction.ButtonInteraction
import dev.kord.core.entity.interaction.SelectMenuInteraction
import kotlin.coroutines.CoroutineContext

/**
 * @author redslime
 * @version 2023-09-22
 */
class InteractionManager {

    val buttons: HashMap<String, suspend CoroutineContext.(ButtonInteraction) -> Unit> = HashMap()
    val selectors: HashMap<String, suspend CoroutineContext.(SelectMenuInteraction) -> Unit> = HashMap()

}