package remix.myplayer.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import remix.myplayer.data.model.audio.Song
import remix.myplayer.helper.AudioTagFile
import timber.log.Timber
import java.io.File

object ImageUtil {

  fun loadScaledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
      // 1. Decode bounds only
      var input = context.contentResolver.openInputStream(uri) ?: return null
      val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
      }
      BitmapFactory.decodeStream(input, null, options)
      input.close()

      // 2. Calculate inSampleSize
      var inSampleSize = 1
      if (options.outHeight > maxDimension || options.outWidth > maxDimension) {
        val halfHeight = options.outHeight / 2
        val halfWidth = options.outWidth / 2
        while ((halfHeight / inSampleSize) >= maxDimension && (halfWidth / inSampleSize) >= maxDimension) {
          inSampleSize *= 2
        }
      }

      // 3. Decode with inSampleSize
      input = context.contentResolver.openInputStream(uri) ?: return null
      options.inJustDecodeBounds = false
      options.inSampleSize = inSampleSize
      val bitmap = BitmapFactory.decodeStream(input, null, options)
      input.close()
      bitmap
    } catch (e: Exception) {
      Timber.e(e, "Failed to load scaled bitmap")
      null
    }
  }

  suspend fun extractAlbumArt(song: Song): ImageBitmap? = withContext(Dispatchers.IO) {
    if (!song.valid() || !song.isLocal()) {
      return@withContext null
    }

    try {
      val artwork = AudioTagFile.readFrontCover(File(song.data))
      if (artwork != null) {
        val artworkBinaryData = artwork.data
        val bitmap = BitmapFactory.decodeByteArray(
          artworkBinaryData,
          0,
          artworkBinaryData.size
        )
        return@withContext bitmap.asImageBitmap()
      }
      return@withContext null
    } catch (e: Exception) {
      Timber.w(e, "extractAlbumArt")
      return@withContext null
    }
  }
}
