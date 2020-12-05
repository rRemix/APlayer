package remix.myplayer.misc.tageditor

import android.media.MediaScannerConnection
import androidx.annotation.WorkerThread
import io.reactivex.Observable
import io.reactivex.Single
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.util.MediaStoreUtil
import java.io.File
import java.util.*
import kotlin.concurrent.thread

/**
 * 标签相关
 */
class TagEditor(path: String) {
  private var audioFile: AudioFile? = null
  private var audioHeader: AudioHeader? = audioFile?.audioHeader
  var initSuccess: Boolean = false

  private val lock = Any()

  init {
    thread {
      synchronized(lock) {
        audioFile = try {
          AudioFileIO.read(File(path))
        } catch (e: Exception) {
          null
        }
        audioHeader = audioFile?.audioHeader
        initSuccess = audioFile != null && audioHeader != null
      }
    }
  }

  fun formatSingle(): Single<String> {
    return Single.fromCallable {
      synchronized(lock) {
        if (!initSuccess)
          return@fromCallable ""
        audioHeader?.format ?: ""
      }
    }
  }

  fun bitrateSingle(): Single<String> {
    return Single.fromCallable {
      synchronized(lock) {
        if (!initSuccess)
          return@fromCallable ""
        audioHeader?.bitRate ?: ""
      }
    }
  }

  fun samplingSingle(): Single<String> {
    return Single.fromCallable {
      synchronized(lock) {
        if (!initSuccess)
          return@fromCallable ""
        audioHeader?.sampleRate ?: ""
      }
    }
  }

  fun getFieldValueSingle(field: FieldKey): Single<String> {
    return Single.fromCallable {
      getFiledValue(field)
    }
  }

  @WorkerThread
  private fun getFiledValue(field: FieldKey): String? {
    synchronized(lock) {
      if (!initSuccess)
        return ""
      return try {
        audioFile?.tagOrCreateAndSetDefault?.getFirst(field)
      } catch (e: Exception) {
        ""
      }
    }
  }

  fun save(song: Song, title: String, album: String, artist: String, year: String, genre: String, trackNumber: String, lyric: String): Observable<Song> {
    return Observable.create { e ->
      synchronized(lock) {
        if (!initSuccess) {
          throw IllegalArgumentException("init failed")
        }

        val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
        fieldKeyValueMap[FieldKey.ALBUM] = album
        fieldKeyValueMap[FieldKey.TITLE] = title
        fieldKeyValueMap[FieldKey.YEAR] = year
        fieldKeyValueMap[FieldKey.GENRE] = genre
        fieldKeyValueMap[FieldKey.ARTIST] = artist
        fieldKeyValueMap[FieldKey.TRACK] = trackNumber

        val tag = audioFile?.tagOrCreateAndSetDefault
        for ((key, value) in fieldKeyValueMap) {
          try {
            tag?.setField(key, value ?: "")
          } catch (exception: Exception) {
            exception.printStackTrace()
          }
        }

        audioFile?.commit()

        MediaScannerConnection.scanFile(App.getContext(),
            arrayOf(song.url), null
        ) { path, uri ->
          App.getContext().contentResolver.notifyChange(uri, null)
          e.onNext(MediaStoreUtil.getSongById(song.id))
          e.onComplete()
        }
      }
    }
  }
}
