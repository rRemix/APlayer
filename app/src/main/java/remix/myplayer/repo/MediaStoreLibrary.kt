package remix.myplayer.repo

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio
import android.provider.MediaStore.Audio.AudioColumns
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import remix.myplayer.data.db.room.dao.PlayListDao
import remix.myplayer.data.db.room.dao.PlayQueueDao
import remix.myplayer.data.model.audio.Song
import remix.myplayer.data.model.audio.SongIdentity
import remix.myplayer.data.model.audio.SourceId
import remix.myplayer.data.prefs.CoverPrefs
import remix.myplayer.data.prefs.SettingPrefs
import remix.myplayer.util.Util
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/** Owns the shared MediaStore snapshot and serializes scans and reference reconciliation. */
@Singleton
class MediaStoreLibrary @Inject constructor(
  @param:ApplicationContext private val context: Context,
  private val settings: SettingPrefs,
  private val playListDao: PlayListDao,
  private val playQueueDao: PlayQueueDao,
  private val coverPrefs: CoverPrefs
) {
  private val mutex = Mutex()
  data class Snapshot(val songs: List<Song> = emptyList(), val revision: Long = 0)
  private val state = MutableStateFlow(Snapshot())
  val snapshots = state.asStateFlow()

  /** Initial reads share one scan. Subsequent reads never silently rescan MediaStore. */
  suspend fun ensureLoaded() = withContext(Dispatchers.IO) {
    mutex.withLock { if (state.value.revision == 0L) scanAndPublish() }
  }

  suspend fun refresh() = withContext(Dispatchers.IO) {
    mutex.withLock { scanAndPublish() }
  }

  private suspend fun scanAndPublish() {
    val songs = scanSongs() ?: return
    // User filters only affect the snapshot; cleanup needs every existing MediaStore ID.
    queryAllIds()?.let { existing ->
      val missing = playListDao.localKeys().filterNot { it in existing }
        .mapTo(hashSetOf()) { SongIdentity(SourceId.MEDIASTORE, it) }
      if (missing.isNotEmpty()) {
        val affected = playListDao.selectAllOnce().map { it.toModel() }
          .filter { list -> list.songIdentities.any { it in missing } }
        playListDao.removeSongsFromAll(missing)
        affected.forEach { coverPrefs.invalidate("playlist:${it.id}") }
      }
      val missingQueue = playQueueDao.get().filter {
        it.sourceId == SourceId.MEDIASTORE.value && it.sourceKey !in existing
      }
      if (missingQueue.isNotEmpty()) playQueueDao.delete(missingQueue)
    }
    // A completed scan also refreshes genre membership when the song fields are unchanged.
    val songsChanged = state.value.songs != songs
    state.value = Snapshot(songs.toList(), state.value.revision + 1)
    // Publish first so new cover requests resolve the current filtered candidates.
    if (songsChanged) coverPrefs.refreshMemory()
  }

  /** Null means incomplete/denied; an empty set confirms there are no existing songs. */
  private fun queryAllIds(): Set<String>? {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else Manifest.permission.READ_EXTERNAL_STORAGE
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return null
    return try {
      val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else Audio.Media.EXTERNAL_CONTENT_URI
      context.contentResolver.query(uri, arrayOf(AudioColumns._ID), null, null, null)?.use { cursor ->
        buildSet { while (cursor.moveToNext()) add(cursor.getLong(0).toString()) }
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Timber.w(error, "Unable to query MediaStore IDs")
      null
    }
  }

  /** Null means incomplete/denied; an empty list is a successful empty scan. */
  private fun scanSongs(): List<Song>? {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      Manifest.permission.READ_MEDIA_AUDIO
    } else Manifest.permission.READ_EXTERNAL_STORAGE
    if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) return null
    return try {
      val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else Audio.Media.EXTERNAL_CONTENT_URI
      val excludedIds = settings.deleteIds
      val blacklist = settings.blacklist
      val selection = buildList {
        add("${Audio.Media.DATA} != ''")
        add("${Audio.Media.SIZE} > ${settings.scanSize}")
        if (excludedIds.isNotEmpty()) add("${Audio.Media._ID} NOT IN (${excludedIds.joinToString()})")
        repeat(blacklist.size) { add("${Audio.Media.DATA} NOT LIKE ?") }
      }.joinToString(" AND ")
      val args = blacklist.map { "$it%" }.toTypedArray()
      context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
        buildList { while (cursor.moveToNext()) add(resolveSong(cursor)) }
      }
    } catch (error: CancellationException) {
      throw error
    } catch (error: Exception) {
      Timber.w(error, "Unable to scan MediaStore")
      null
    }
  }

  private val projection: Array<String> = run {
    val projection = ArrayList(
      listOf(
        AudioColumns._ID,
        AudioColumns.TITLE,
        AudioColumns.DISPLAY_NAME,
        AudioColumns.SIZE,
        AudioColumns.YEAR,
        AudioColumns.TRACK,
        AudioColumns.DURATION,
        AudioColumns.DATE_MODIFIED,
        AudioColumns.DATE_ADDED,
        AudioColumns.DATA,
        AudioColumns.ALBUM_ID,
        AudioColumns.ALBUM,
        AudioColumns.ARTIST_ID,
        AudioColumns.ARTIST
      )
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      projection.add(AudioColumns.GENRE)
    }
    projection.toTypedArray()
  }

  private fun resolveSong(cursor: Cursor): Song {
    return Song(
      sourceId = SourceId.MEDIASTORE,
      sourceKey = cursor.getLong(cursor.getColumnIndex(AudioColumns._ID)).toString(),
      displayName = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.DISPLAY_NAME)),
        Util.TYPE_DISPLAYNAME
      ),
      title = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.TITLE)),
        Util.TYPE_SONG
      ),
      album = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.ALBUM)),
        Util.TYPE_ALBUM
      ),
      albumId = cursor.getLong(cursor.getColumnIndex(AudioColumns.ALBUM_ID)),
      artist = Util.processInfo(
        cursor.getString(cursor.getColumnIndex(AudioColumns.ARTIST)),
        Util.TYPE_ARTIST
      ),
      artistId = cursor.getLong(cursor.getColumnIndex(AudioColumns.ARTIST_ID)),
      duration = cursor.getLong(cursor.getColumnIndex(AudioColumns.DURATION)),
      data = cursor.getString(cursor.getColumnIndex(AudioColumns.DATA)),
      size = cursor.getLong(cursor.getColumnIndex(AudioColumns.SIZE)),
      year = cursor.getLong(cursor.getColumnIndex(AudioColumns.YEAR)).toString(),
      genre = cursor.getColumnIndex(AudioColumns.GENRE).let {
        if (it != -1) {
          cursor.getString(it).orEmpty()
        } else {
          ""
        }
      },
      track = cursor.getLong(cursor.getColumnIndex(AudioColumns.TRACK)).toString(),
      dateModified = cursor.getLong(cursor.getColumnIndex(AudioColumns.DATE_MODIFIED)) * 1000L,
      addedAt = cursor.getLong(cursor.getColumnIndex(AudioColumns.DATE_ADDED)) * 1000L
    )
  }
}
