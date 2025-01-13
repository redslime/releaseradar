package xyz.redslime.releaseradar.util

/**
 * @author redslime
 * @version 2025-01-11
 */

import dev.kord.common.Color
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

val MIN_SATURATION = 0.35f
val MIN_LIGHTNESS = 0.4f

data class RGB(val r: Int, val g: Int, val b: Int) {
    fun hsv(): HSV {
        return rgbToHsv(r, g, b)
    }

    fun score(): Int {
        return r + g + b
    }

    fun kord(): Color {
        return Color(r, g, b)
    }

    fun shouldSkip(): Boolean {
        if(r + g + b <= 3*55) {
            // too dark
            return true
        }

        if(r + g + b >= 3*200) {
            // too bright
            return true
        }

        if(r == g && g == b) {
            // greyscale
            return true
        }

        val (_, saturation, lightness) = hsv()
        return saturation < MIN_SATURATION || lightness < MIN_LIGHTNESS
    }
}

data class HSV(val hue: Float, val saturation: Float, val lightness: Float)

fun Int.toRGB(): RGB {
    val r = (this shr 16) and 0xFF
    val g = (this shr 8) and 0xFF
    val b = this and 0xFF
    return RGB(r, g, b)
}

fun rgbToHsv(r: Int, g: Int, b: Int): HSV {
    // Normalize the RGB values to the range [0, 1]
    val rNorm = r / 255.0f
    val gNorm = g / 255.0f
    val bNorm = b / 255.0f

    // Find the minimum and maximum values among rNorm, gNorm, and bNorm
    val max = maxOf(rNorm, gNorm, bNorm) // = lightness
    val min = minOf(rNorm, gNorm, bNorm)
    val delta = max - min

    // Calculate Hue
    val hue = when {
        delta == 0f -> 0f
        max == rNorm -> ((gNorm - bNorm) / delta) % 6
        max == gNorm -> ((bNorm - rNorm) / delta) + 2
        else -> ((rNorm - gNorm) / delta) + 4
    }.let { h ->
        (h * 60).let { if (it < 0) it + 360 else it }
    }

    // Calculate Saturation
    val saturation = if (max == 0f) 0f else delta / max

    return HSV(hue, saturation, max)
}

fun analyzeImageColors(url: String): Color? {
    try {
        val image: BufferedImage = ImageIO.read(URL(url))
        val avgColors = mutableMapOf<RGB, Int>()
        val skippedColors = mutableMapOf<RGB, Int>()

        for (x in 0 until image.width) {
            for (y in 0 until image.height) {
                // Average a chunk of 4x4 pixels into one color
                if (x % 4 == 0 && y % 4 == 0 && x + 3 < image.width && y + 3 < image.height) {
                    var red = 0
                    var green = 0
                    var blue = 0
                    var count = 0

                    for (dx in 0..3) {
                        for (dy in 0..3) {
                            val rgb = image.getRGB(x + dx, y + dy).toRGB()

                            // Skip black-ish and white-ish pixels
                            if (rgb.shouldSkip()) {
                                skippedColors[rgb] = skippedColors.getOrDefault(rgb, 0) + 1
                                continue
                            }

                            red += rgb.r
                            green += rgb.g
                            blue += rgb.b
                            count++
                        }
                    }

                    if (count > 0) {
                        val avgRed = red / count
                        val avgGreen = green / count
                        val avgBlue = blue / count
                        val avgRGB = RGB(avgRed, avgGreen, avgBlue)
                        avgColors[avgRGB] = avgColors.getOrDefault(avgRGB, 0) + 1
                    }
                }
            }
        }

        val all = avgColors.entries.sumOf { it.value }.toFloat()
        val include = (all * 0.15).toInt()
        var included = 0

        val final = avgColors.entries.sortedByDescending { it.value }.take(15).takeWhile { (_, count) ->
            var cont = false

            if (included < include) {
                included += count
                cont = true
            }

            cont
        }.maxByOrNull { it.key.score() }?.key?.kord()
            ?: skippedColors.entries.sortedByDescending { it.value }.take(10).maxByOrNull { (color, _) ->
                val (_, sat, light) = color.hsv()
                sat + (10 * light)
            }?.key?.kord()

        return final
    } catch (e: Exception) {
        return null
    }
}

fun getArtworkColor(url: String): Color? {
    if(url.isBlank())
        return null

    return analyzeImageColors(url)
}