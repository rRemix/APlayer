package remix.myplayer.compose

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContextWrapper
import android.media.MediaScannerConnection
import android.os.Build
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.compose.activity.base.BaseActivity
import remix.myplayer.databinding.DialogSongDetailBinding
import remix.myplayer.databinding.DialogSongEditBinding
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.theme.TextInputLayoutUtil
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.misc.TextInputEditWatcher
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.ref.WeakReference
import java.util.EnumMap


class AudioTagEditor(activity: BaseActivity, private val song: Song) : ContextWrapper(activity) {

  private val actRef = WeakReference(activity)
  private var title: String = ""
  private var album: String = ""
  private var artist: String = ""
  private var year: String = ""
  private var genre: String = ""
  private var track: String = ""

  fun detail() {
    var disposable: Disposable? = null

    val detailDialog = Theme.getBaseDialog(this)
      .title(R.string.song_detail)
      .customView(R.layout.dialog_song_detail, true)
      .positiveText(R.string.close)
      .onPositive { _, _ -> disposable?.dispose() }
      .build()

    val binding = DialogSongDetailBinding.bind(detailDialog.customView!!)

    binding.songDetailPath.text = song.data
    binding.songDetailName.text = song.showName
    binding.songDetailSize.text = getString(R.string.cache_size, 1.0f * song.size / MB)
    binding.songDetailDuration.text = Util.getTime(song.duration)

    arrayOf(
      binding.songDetailPath,
      binding.songDetailName,
      binding.songDetailSize,
      binding.songDetailMime,
      binding.songDetailDuration,
      binding.songDetailBitRate,
      binding.songDetailSampleRate
    ).forEach {
      TintHelper.setTint(it, ThemeStore.accentColor, false)
    }

    if (song.isLocal()) {
      disposable = Single.fromCallable { AudioFileIO.read(File(song.data)).audioHeader }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribeOn(Schedulers.io())
        .subscribe(
          { audioHeader ->
            binding.songDetailMime.text = audioHeader.format
            @SuppressLint("SetTextI18n")
            binding.songDetailBitRate.text = "${audioHeader.bitRate} kb/s"
            @SuppressLint("SetTextI18n")
            binding.songDetailSampleRate.text = "${audioHeader.sampleRate} Hz"
          }, {
            ToastUtil.show(this, getString(R.string.init_failed, it))
          })
    } else if (song is Song.Remote) {
      binding.songDetailMime.text = song.data.substringAfterLast('.')
      @SuppressLint("SetTextI18n")
      binding.songDetailBitRate.text = "${song.bitRate} kb/s"
      @SuppressLint("SetTextI18n")
      binding.songDetailSampleRate.text = "${song.sampleRate} Hz"
    }


    detailDialog.show()
  }

  fun edit() {
    val editDialog = Theme.getBaseDialog(this)
      .title(R.string.song_edit)
      .customView(R.layout.dialog_song_edit, true)
      .negativeText(R.string.cancel)
      .positiveText(R.string.confirm)
      .onPositive { dialog, _ ->
        dialog.customView!!.let { root ->
          val binding = DialogSongEditBinding.bind(root)
          title = binding.songLayout.editText!!.text.toString()
          artist = binding.artistLayout.editText!!.text.toString()
          album = binding.albumLayout.editText!!.text.toString()
          genre = binding.genreLayout.editText!!.text.toString()
          year = binding.yearLayout.editText!!.text.toString()
          track = binding.trackLayout.editText!!.text.toString()
          saveTag()
        }
      }
      .build()

    editDialog.customView!!.let { root ->
      val binding = DialogSongEditBinding.bind(root)
      val textInputTintColor = ThemeStore.accentColor
      val editTintColor = ThemeStore.accentColor
      for (layout in arrayOf(
        binding.songLayout,
        binding.albumLayout,
        binding.artistLayout,
        binding.yearLayout,
        binding.trackLayout,
        binding.genreLayout
      )) {
        TextInputLayoutUtil.setAccent(layout, textInputTintColor)
        TintHelper.setTintAuto(layout.editText!!, editTintColor, false)
      }
      binding.songLayout.editText!!.addTextChangedListener(
        TextInputEditWatcher(
          binding.songLayout,
          getString(R.string.song_not_empty)
        )
      )
      binding.songLayout.editText!!.setText(song.title)
      binding.albumLayout.editText!!.setText(song.album)
      binding.artistLayout.editText!!.setText(song.artist)
      binding.yearLayout.editText!!.setText(song.year)
      binding.trackLayout.editText!!.setText(song.track)
      binding.genreLayout.editText!!.setText(song.genre)
    }

    editDialog.show()
  }

  fun saveTag() {
    val activity = actRef.get() ?: return
    fun saveTag(file: File) {
      val audioFile = AudioFileIO.read(file)
      val fieldKeyValueMap = EnumMap<FieldKey, String>(FieldKey::class.java)
      fieldKeyValueMap[FieldKey.ALBUM] = album
      fieldKeyValueMap[FieldKey.TITLE] = title
      fieldKeyValueMap[FieldKey.YEAR] = year
      fieldKeyValueMap[FieldKey.GENRE] = genre
      fieldKeyValueMap[FieldKey.ARTIST] = artist
      fieldKeyValueMap[FieldKey.TRACK] = track
      val tag = audioFile.tagOrCreateAndSetDefault
      for ((key, value) in fieldKeyValueMap) {
        try {
          tag.setField(key, value)
        } catch (e: Exception) {
          Timber.v("setField($key, $value) failed: $e")
        }
      }
      audioFile.commit()
    }

    try {
      try {
        saveTag(File(song.data))
        ToastUtil.show(this, R.string.save_success)
      } catch (e: CannotWriteException) {
        val cacheDir = DiskCache.getDiskCacheDir(this, CACHE_DIR_NAME)
        val tmpFile =
          File(cacheDir, song.data.substring(song.data.lastIndexOf(File.separatorChar) + 1))
        try {
          var songFD =
            activity.contentResolver.openFileDescriptor(
              song.contentUri,
              "w"
            )!! // test if we can write
          songFD.close()

          cacheDir.mkdirs()
          tmpFile.createNewFile()

          songFD = activity.contentResolver.openFileDescriptor(song.contentUri, "r")!!
          var inputStream = FileInputStream(songFD.fileDescriptor)
          var outputStream = FileOutputStream(tmpFile)
          inputStream.copyTo(outputStream, inputStream.available())
          songFD.close()
          outputStream.close()

          saveTag(tmpFile)

          songFD = activity.contentResolver.openFileDescriptor(song.contentUri, "w")!!
          inputStream = FileInputStream(tmpFile)
          outputStream = FileOutputStream(songFD.fileDescriptor)
          inputStream.copyTo(outputStream, inputStream.available())
          inputStream.close()
          songFD.close()

          ToastUtil.show(this, R.string.save_success)
        } catch (securityException: SecurityException) {
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
            activity.audioTag = this
            startIntentSenderForResult(
              activity,
              securityException.userAction.actionIntent.intentSender,
              REQUEST_WRITE_PERMISSION,
              null,
              0,
              0,
              0,
              null
            )
            return
          }
          throw securityException
        } catch (e: Exception) {
          throw e
        } finally {
          tmpFile.delete()
        }
      }
      MediaScannerConnection.scanFile(
        activity,
        arrayOf(song.data), null
      ) { _, uri ->
        activity.contentResolver.notifyChange(uri, null)
      }
    } catch (e: Exception) {
      Timber.v("Fail to save tag")
      e.printStackTrace()
      ToastUtil.show(activity, R.string.save_error_arg, e.toString())
    }
  }

  companion object {

    private const val CACHE_DIR_NAME = "tag"
    const val REQUEST_WRITE_PERMISSION = 0x100
  }
}

