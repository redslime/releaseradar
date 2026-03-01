package xyz.redslime.releaseradar.util


/**
 * @author redslime
 * @version 2025-09-18
 */
class CacheMap<K, V>(private val capacity: Int = 1000) : LinkedHashMap<K, V?>(capacity, 0.75f, true) {

    fun get(key: K, fallback: (K) -> V?): V? {
        if (!containsKey(key)) store(key, fallback.invoke(key))
        return get(key)
    }

    fun store(key: K, value: V?) {
        put(key, value)
    }

    override fun removeEldestEntry(eldest: Map.Entry<K?, V?>?): Boolean {
        return size > capacity
    }
}