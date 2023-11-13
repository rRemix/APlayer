package remix.myplayer.ui.misc

import android.annotation.SuppressLint
import android.app.RecoverableSecurityException
import android.content.ContextWrapper
import android.media.MediaScannerConnection
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import com.google.android.material.textfield.TextInputLayout
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_song_edit.view.*
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.DialogSongDetailBinding
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.misc.cache.DiskCache
import remix.myplayer.theme.TextInputLayoutUtil
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.activity.base.BaseActivity
import remix.myplayer.util.Constants.MB
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*
import java.util.function.Consumer


class AudioTag(val activity: BaseActivity, song: Song?) : ContextWrapper(activity) {
  var song: Song = song ?: getCurrentSong()

  private var title: String = ""
  private var album: String = ""
  private var artist: String = ""
  private var year: String = ""
  private var genre: String = ""
  private var track: String = ""

  fun detail() {
    val song = song
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
    } else if (song is Song.Remote){
      binding.songDetailMime.text = song.data.substring(song.data.lastIndexOf('.') + 1)
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
        .onPositive { dialog, which ->
          dialog.customView?.let { root ->
            title = root.song_layout.editText?.text.toString()
            artist = root.artist_layout.editText?.text.toString()
            album = root.album_layout.editText?.text.toString()
            genre = root.genre_layout.editText?.text.toString()
            year = root.year_layout.editText?.text.toString()
            track = root.track_layout.editText?.text.toString()
            if (TextUtils.isEmpty(title)) {
              ToastUtil.show(this, R.string.song_not_empty)
              return@onPositive
            }
            saveTag()
          }
        }
        .build()

    editDialog.customView?.let { root ->
      val textInputTintColor = ThemeStore.accentColor
      val editTintColor = ThemeStore.accentColor
      TextInputLayoutUtil.setAccent(root.song_layout, textInputTintColor)
      TintHelper.setTintAuto(root.song_layout.editText!!, editTintColor, false)
      root.song_layout.editText?.addTextChangedListener(TextInputEditWatcher(root.song_layout, getString(R.string.song_not_empty)))
      root.song_layout.editText?.setText(song.title)

      TextInputLayoutUtil.setAccent(root.album_layout, textInputTintColor)
      TintHelper.setTintAuto(root.album_layout.editText!!, editTintColor, false)
      root.album_layout.editText?.setText(song.album)

      TextInputLayoutUtil.setAccent(root.artist_layout, textInputTintColor)
      TintHelper.setTintAuto(root.artist_layout.editText!!, editTintColor, false)
      root.artist_layout.editText?.setText(song.artist)

      TextInputLayoutUtil.setAccent(root.year_layout, textInputTintColor)
      TintHelper.setTintAuto(root.year_layout.editText!!, editTintColor, false)
      root.year_layout.editText?.setText(song.year)

      TextInputLayoutUtil.setAccent(root.track_layout, textInputTintColor)
      TintHelper.setTintAuto(root.track_layout.editText!!, editTintColor, false)
      root.track_layout.editText?.setText(song.track)

      TextInputLayoutUtil.setAccent(root.genre_layout, textInputTintColor)
      TintHelper.setTintAuto(root.genre_layout.editText!!, editTintColor, false)
      root.genre_layout.editText?.setText(song.genre)
    }

    editDialog.show()
  }

  fun saveTag() {
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


class TextInputEditWatcher(private val inputLayout: TextInputLayout, private val error: String) : TextWatcher {

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

  override fun afterTextChanged(s: Editable?) {
    if (s == null || TextUtils.isEmpty(s.toString())) {
      inputLayout.error = error
    } else {
      inputLayout.error = ""
    }
  }
}

