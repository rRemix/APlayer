package remix.myplayer.misc

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.Looper
import android.provider.MediaStore
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.thegrizzlylabs.sardineandroid.DavResource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import remix.myplayer.bean.mp3.Album
import remix.myplayer.bean.mp3.Artist
import remix.myplayer.bean.mp3.Folder
import remix.myplayer.bean.mp3.Genre
import remix.myplayer.util.MediaStoreUtil
import timber.log.Timber
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

fun Album.getSongIds(): List<Long> {
  return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ALBUM_ID + "=?", arrayOf((albumID.toString())))
}

fun Artist.getSongIds(): List<Long> {
  return MediaStoreUtil.getSongIds(MediaStore.Audio.Media.ARTIST_ID + "=?", arrayOf(artistID.toString()))
}

fun Folder.getSongIds(): List<Long> {
  return MediaStoreUtil.getSongsByParentPath(path).map { it.id }
}

fun Genre.getSongIds(): List<Long> {
  return MediaStoreUtil.getSongsByGenreId(id).map { it.id }
}

fun Context.isPortraitOrientation(): Boolean {
  val configuration = this.resources.configuration //获取设置的配置信息
  val orientation = configuration.orientation //获取屏幕方向
  return orientation == Configuration.ORIENTATION_PORTRAIT
}

fun CoroutineScope.tryLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> Unit,
    catch: ((e: Exception) -> Unit)? = { Timber.w(it) }) {
  launch(context, start) {
    try {
      block()
    } catch (e: Exception) {
      catch?.invoke(e)
    }
  }
}

fun CoroutineScope.tryLaunch(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend () -> Unit) {
  tryLaunch(context = context,
      start = start,
      block = block,
      catch = {
        Timber.w(it)
      })
}

fun Any?.checkMainThread() {
  if (Looper.myLooper() != Looper.getMainLooper()) {
    throw RuntimeException("$this should be used only from the application's main thread")
  }
}

fun Any?.checkWorkerThread() {
  if (Looper.myLooper() == Looper.getMainLooper()) {
    throw RuntimeException("$this should be used only from the worker thread")
  }
}


fun File.zipInputStream() = ZipInputStream(this.inputStream())

fun File.zipOutputStream() = ZipOutputStream(this.outputStream())

fun ZipOutputStream.zipFrom(vararg paths: String) {
  use {
    val files = paths.map { File(it) }
    files.forEach {
      if (it.isFile) {
        zip(arrayOf(it), null)
      } else if (it.isDirectory) {
        zip(it.listFiles(), it.name)
      }
    }
  }

}

private fun ZipOutputStream.zip(files: Array<File>, path: String?) {
  //前缀,用于构造路径
  val prefix = if (path == null) "" else "$path/"

  if (files.isEmpty()) createEmptyFolder(prefix)

  files.forEach {
    if (it.isFile) {
      val entry = ZipEntry("$prefix${it.name}")
      val ins = it.inputStream().buffered()
      putNextEntry(entry)
      ins.writeTo(this, DEFAULT_BUFFER_SIZE, closeOutput = false)
      closeEntry()
    } else {
      zip(it.listFiles(), "$prefix${it.name}")
    }
  }
}

/**
 * inputstream内容写入outputstream
 */
fun InputStream.writeTo(outputStream: OutputStream, bufferSize: Int = 1024 * 2,
                        closeInput: Boolean = true, closeOutput: Boolean = true) {

  val buffer = ByteArray(bufferSize)
  val br = this.buffered()
  val bw = outputStream.buffered()
  var length = 0

  while ({ length = br.read(buffer);length != -1 }()) {
    bw.write(buffer, 0, length)
  }

  bw.flush()

  if (closeInput) {
    close()
  }

  if (closeOutput) {
    outputStream.close()
  }
}

/**
 * 生成一个压缩文件的文件夹
 */
private fun ZipOutputStream.createEmptyFolder(location: String) {
  putNextEntry(ZipEntry(location))
  closeEntry()
}

fun getPendingIntentFlag() =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT


private val musicExt = setOf("wav","aif","au","mp3","ram","wma","mmf","amr","aac","flac")
fun DavResource.isAudio(): Boolean {
  if (isDirectory || path.isNullOrEmpty()) {
    return false
  }
  val ext = path.substringAfterLast(".")
  return musicExt.contains(ext.lowercase(Locale.getDefault())) &&
      (contentType == "application/octet-stream" || contentType.startsWith("audio"))
}

// glide加载图片之前检查activity是否被销毁
fun Context.isValidGlideContext() = this !is Activity || (!this.isDestroyed && !this.isFinishing)

fun Color.toHexString(withAlpha: Boolean = false): String {
  val argb = this.toArgb()
  return if (withAlpha) {
    String.format(
      "%08X",
      argb
    )
  } else {
    String.format(
      "%06X",
      argb and 0xFFFFFF  // 移除Alpha通道
    )
  }
}