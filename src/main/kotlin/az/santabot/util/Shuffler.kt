package az.santabot.util

fun <T> shuffle(ids: List<T>): Map<T, T> {
    if (ids.size < 2) {
        return ids.zip(ids).toMap()
    }

    val map = ids.zip(ids.shuffled()).toMap().toMutableMap()

    val toReshuffle = map.filter { entry -> entry.key == entry.value }.keys
    val processed = mutableSetOf<T>()

    toReshuffle
        .forEach {
            if (!processed.contains(it)) {
                var key: T
                do {
                    key = map.keys.random()
                } while (key == it)

                map[it] = map[key]!!
                map[key] = it

                processed.add(it)
                processed.add(key)
            }
        }

    return map
}