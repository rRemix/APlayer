package remix.myplayer.helper

import android.os.ParcelFileDescriptor
import com.kyant.taglib.AudioProperties
import com.kyant.taglib.Metadata
import com.kyant.taglib.Picture
import com.kyant.taglib.PropertyMap
import com.kyant.taglib.TagLib
import java.io.File
import java.io.IOException

/**
 * File-based access to audio metadata through TagLib.
 *
 * TagLib takes ownership of the supplied raw file descriptor, so every call uses a detached
 * duplicate and leaves the [ParcelFileDescriptor] owned by Android untouched.
 */
object AudioTagFile {

  const val TITLE = "TITLE"
  const val ALBUM = "ALBUM"
  const val ARTIST = "ARTIST"
  const val ALBUM_ARTIST = "ALBUMARTIST"
  const val COMPOSER = "COMPOSER"
  const val GENRE = "GENRE"
  const val DATE = "DATE"
  const val TRACK_NUMBER = "TRACKNUMBER"
  const val DISC_NUMBER = "DISCNUMBER"
  const val LYRICS = "LYRICS"

  fun readAudioProperties(file: File): AudioProperties? =
    withFileDescriptor(file, ParcelFileDescriptor.MODE_READ_ONLY) { fd ->
      TagLib.getAudioProperties(fd)
    }

  fun readMetadata(file: File, readPictures: Boolean = true): Metadata? =
    withFileDescriptor(file, ParcelFileDescriptor.MODE_READ_ONLY) { fd ->
      TagLib.getMetadata(fd, readPictures)
    }

  fun readFrontCover(file: File): Picture? =
    withFileDescriptor(file, ParcelFileDescriptor.MODE_READ_ONLY) { fd ->
      TagLib.getFrontCover(fd)
    }

  fun savePropertyMap(file: File, propertyMap: PropertyMap): Boolean =
    withFileDescriptor(file, ParcelFileDescriptor.MODE_READ_WRITE) { fd ->
      TagLib.savePropertyMap(fd, propertyMap)
    }

  fun savePictures(file: File, pictures: Array<Picture>): Boolean =
    withFileDescriptor(file, ParcelFileDescriptor.MODE_READ_WRITE) { fd ->
      TagLib.savePictures(fd, pictures)
    }

  fun firstValue(propertyMap: Map<String, Array<String>>?, key: String): String =
    propertyMap?.get(key)?.firstOrNull().orEmpty()

  fun setValue(propertyMap: PropertyMap, key: String, value: String) {
    if (value.isBlank()) {
      propertyMap.remove(key)
    } else {
      propertyMap[key] = arrayOf(value)
    }
  }

  fun requireSaved(saved: Boolean, operation: String) {
    if (!saved) {
      throw IOException("TagLib failed to $operation")
    }
  }

  private inline fun <T> withFileDescriptor(
    file: File,
    mode: Int,
    block: (Int) -> T
  ): T = ParcelFileDescriptor.open(file, mode).use { descriptor ->
    block(descriptor.dup().detachFd())
  }
}
