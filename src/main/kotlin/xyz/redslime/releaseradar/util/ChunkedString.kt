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

    suspend fun <T> chunked(head: suspend (String) -> T, tails: suspend (Int, T, String) -> Unit, limit: Int = 4000, delimiter: String = "\n"): T {
        val chunks = getChunks(limit, delimiter)
        val first: T = head.invoke(chunks[0])
        chunks.stream().skip(1).toList().forEachIndexed { index, str -> tails.invoke(index, first, str) }
        return first
    }
}