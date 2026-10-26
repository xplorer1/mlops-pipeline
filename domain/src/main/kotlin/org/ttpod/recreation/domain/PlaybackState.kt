package org.ttpod.recreation.domain

data class QueueTrack(val entryId: String, val uri: String, val title: String, val artist: String)
data class QueueSnapshot(
    val tracks: List<QueueTrack> = emptyList(), val index: Int = 0, val positionMs: Long = 0,
    val repeatMode: Int = 0, val shuffle: Boolean = false, val shuffleSeed: Long = 42L,
    val shuffleOrder: List<Int> = emptyList()
)
interface QueueStore {
    suspend fun load(): QueueSnapshot?
    suspend fun save(snapshot: QueueSnapshot)
}

object QueueRules {
    fun previous(index: Int, positionMs: Long): Pair<Int, Long> =
        (if (positionMs > 3000) index else (index - 1).coerceAtLeast(0)) to 0L
    fun seek(positionMs: Long, durationMs: Long): Long = positionMs.coerceIn(0, durationMs.coerceAtLeast(0))
}
