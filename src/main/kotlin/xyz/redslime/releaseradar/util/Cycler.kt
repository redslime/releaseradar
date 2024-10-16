package xyz.redslime.releaseradar.util

/**
 * @author redslime
 * @version 2024-08-26
 */
class Cycler<T>(private val items: MutableList<T>): Iterable<T> {

    private var index = 0

    fun add(item: T) {
        items.add(item)
    }

    fun next(): T {
        return items[++index % items.size]
    }

    fun next(filter: (T) -> Boolean): T? {
        repeat(items.size) {
            val item = next()

            if(filter.invoke(item))
                return item
        }

        return null
    }

    override fun iterator(): Iterator<T> {
        return items.iterator()
    }
}