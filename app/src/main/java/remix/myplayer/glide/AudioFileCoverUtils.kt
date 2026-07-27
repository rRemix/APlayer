package remix.myplayer.glide

import remix.myplayer.helper.AudioTagFile
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream

object AudioFileCoverUtils {
  private val FALLBACKS = arrayOf("cover.jpg", "album.jpg", "folder.jpg", "cover.png", "album.png", "folder.png")

  @Throws(FileNotFoundException::class)
  fun fallback(path: String?): InputStream? {
    if (path == null) {
      return null
    }
    // Method 1: use embedded high resolution album art if there is any
    try {
      val artwork = AudioTagFile.readFrontCover(File(path))
      if (artwork != null) {
        return ByteArrayInputStream(artwork.data)
      }
      // If there are any exceptions, we ignore them and continue to the other fallback method
    } catch (ignored: Exception) {
    }

    // Method 2: look for album art in external files
    try {
      val parent = File(path).parentFile
      for (fallback in FALLBACKS) {
        val cover = File(parent, fallback)
        if (cover.exists()) {
          return FileInputStream(cover)
        }
      }
    } catch (ignore: Exception) {
    }
    return null
  }
}
