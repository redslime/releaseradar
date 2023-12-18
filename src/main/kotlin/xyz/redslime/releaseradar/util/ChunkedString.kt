package xyz.redslime.releaseradar.util

/**
 * @author redslime
 * @version 2023-12-16
 */
class ChunkedString {

    private val lines: MutableList<String> = mutableListOf()

    fun add(str: String) {
        lines.add(str)
    }

    fun addAll(cs: ChunkedString) {
        lines.addAll(cs.lines)
    }

    fun getChunks(limit: Int, delimiter: String): List<String> {
        val parts = mutableListOf<String>()
        var buffer = ""
        var size = 0

        lines.forEach {
            if(it.length + size > limit) {
                parts.add(buffer)
                buffer = ""
                size = 0
            } else {
                buffer += it + delimiter
                size += it.length
            }
        }

        parts.add(buffer)

        return parts
    }
}