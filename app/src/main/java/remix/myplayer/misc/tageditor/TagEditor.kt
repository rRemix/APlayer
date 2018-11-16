package remix.myplayer.misc.tageditor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import io.reactivex.Observable
import org.jaudiotagger.audio.AudioFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.AudioHeader
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.App
import remix.myplayer.bean.mp3.Song
import remix.myplayer.util.MediaStoreUtil
import java.io.File
import java.util.*

/**
 * 标签相关
 */
class TagEditor(path: String) {
    private val audioFile: AudioFile? = try {
        AudioFileIO.read(File(path))
    } catch (e: Exception) {
        AudioFile()
    }
    private val audioHeader: AudioHeader?

    init {
        audioHeader = audioFile?.audioHeader
    }


    val format: String
        get() = if (audioHeader != null) audioHeader.format else ""

    val bitrate: String
        get() = if (audioHeader != null) audioHeader.bitRate else ""

    val samplingRate: String
        get() = if (audioHeader != null) audioHeader.sampleRate else ""

    val songTitle: String?
        get() = getFiledValue(FieldKey.TITLE)

    val albumTitle: String?
        get() = getFiledValue(FieldKey.ALBUM)

    val artistName: String?
        get() = getFiledValue(FieldKey.ARTIST)

    val albumArtistName: String?
        get() = getFiledValue(FieldKey.ALBUM_ARTIST)

    val genreName: String?
        get() = getFiledValue(FieldKey.GENRE)

    val songYear: String?
        get() = getFiledValue(FieldKey.YEAR)

    val trackNumber: String?
        get() = getFiledValue(FieldKey.TRACK)

    val lyric: String?
        get() = getFiledValue(FieldKey.LYRICS)

    val albumArt: Bitmap?
        get() {
            if (audioFile == null)
                return null
            try {
                val artworkTag = audioFile.tagOrCreateAndSetDefault.firstArtwork
                if (artworkTag != null) {
                    val artworkBinaryData = artworkTag.binaryData
                    return BitmapFactory.decodeByteArray(artworkBinaryData, 0, artworkBinaryData.size)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            return null
        }


    private fun getFiledValue(field: FieldKey): String? {
        if (audioFile == null)
            return ""
        return try {
            audioFile.tagOrCreateAndSetDefault.getFirst(field)
        } catch (e: Exception) {
            ""
        }

    }

    private fun getFiledValue(path: String, field: String): String? {
        if (audioFile == null)
            return ""
        return try {
            audioFile.tagOrCreateAndSetDefault.getFirst(field)
        } catch (e: Exception) {
            ""
        }

    }

    fun save(song: Song, title: String, album: String, artist: String, year: String, genre: String, trackNumber: String, lyric: String): Observable<Song> {
        return Observable.create { e ->
            if (audioFile == null) {
                throw IllegalArgumentException("AudioFile is null")
            }

            val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
            fieldKeyValueMap[FieldKey.ALBUM] = album
            fieldKeyValueMap[FieldKey.TITLE] = title
            fieldKeyValueMap[FieldKey.YEAR] = year
            fieldKeyValueMap[FieldKey.GENRE] = genre
            fieldKeyValueMap[FieldKey.ARTIST] = artist
            fieldKeyValueMap[FieldKey.TRACK] = trackNumber

            val tag = audioFile.tagOrCreateAndSetDefault
            for ((key, value) in fieldKeyValueMap) {
                try {
                    tag.setField(key, value ?: "")
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
            }

            audioFile.commit()

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
