package remix.myplayer.data.prefs

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stores cover cache revisions. Changing a revision makes Coil use a new cache key,
 * without clearing the image cache itself.
 *
 * Global and per-cover revisions survive app restarts. The memory refresh count does not.
 */
@Singleton
class CoverPrefs @Inject constructor(
  @ApplicationContext context: Context
) : AbstractPref(context, "CoverCache") {
  // Combines global and per-cover revisions with a memory-only refresh count.
  private val _revisions = MutableStateFlow(
    sp.all.filterValues { it is Long }.mapValues { it.value as Long }
  )
  val revisions = _revisions.asStateFlow()

  fun memoryRevision(key: String): String = buildMemoryRevision(_revisions.value, key)

  fun diskRevision(key: String): String = buildDiskRevision(_revisions.value, key)

  fun observeMemoryRevision(key: String): Flow<String> = revisions
    .map { buildMemoryRevision(it, key) }
    .distinctUntilChanged()

  /** Invalidates the specified cover in both memory and disk caches. */
  fun invalidate(key: String) = invalidate(listOf(key))

  /** Invalidates the specified covers in both memory and disk caches. */
  @Synchronized
  fun invalidate(keys: Collection<String>) {
    val distinctKeys = keys.toSet()
    if (distinctKeys.isEmpty()) return

    val currentRevisions = _revisions.value
    val updatedRevisions = distinctKeys.associateWith { key ->
      (currentRevisions[key] ?: 0L) + 1
    }
    sp.edit {
      updatedRevisions.forEach { (key, revision) -> putLong(key, revision) }
    }
    _revisions.value = currentRevisions + updatedRevisions
  }

  /** Invalidates all covers in both memory and disk caches. */
  @Synchronized
  fun invalidateAll() {
    val nextGlobalRevision = (_revisions.value[GLOBAL_REVISION_KEY] ?: 0L) + 1
    sp.edit {
      clear()
      putLong(GLOBAL_REVISION_KEY, nextGlobalRevision)
    }
    _revisions.value = mapOf(GLOBAL_REVISION_KEY to nextGlobalRevision)
  }

  /** Invalidates all memory cache keys while preserving disk cache keys. */
  @Synchronized
  fun refreshMemory() {
    val nextRefreshCount = (_revisions.value[MEMORY_REFRESH_KEY] ?: 0L) + 1
    _revisions.value += MEMORY_REFRESH_KEY to nextRefreshCount
  }

  private fun buildDiskRevision(revisions: Map<String, Long>, key: String): String =
    "${revisions[GLOBAL_REVISION_KEY] ?: 0L}:${revisions[key] ?: 0L}"

  private fun buildMemoryRevision(revisions: Map<String, Long>, key: String): String =
    "${buildDiskRevision(revisions, key)}:${revisions[MEMORY_REFRESH_KEY] ?: 0L}"

  private companion object {
    const val GLOBAL_REVISION_KEY = "*"
    const val MEMORY_REFRESH_KEY = "_memory_refresh"
  }
}
