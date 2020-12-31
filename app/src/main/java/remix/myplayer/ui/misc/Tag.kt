package remix.myplayer.ui.misc

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import com.google.android.material.textfield.TextInputLayout
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import com.facebook.common.util.ByteConstants
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_song_detail.view.*
import kotlinx.android.synthetic.main.dialog_song_edit.view.*
import org.jaudiotagger.tag.FieldKey
import remix.myplayer.App
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.misc.tageditor.TagEditor
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.Command
import remix.myplayer.service.MusicService
import remix.myplayer.theme.TextInputLayoutUtil
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_NEW_SONG
import remix.myplayer.ui.activity.base.BaseMusicActivity.Companion.EXTRA_OLD_SONG
import remix.myplayer.util.ToastUtil
import remix.myplayer.util.Util
import remix.myplayer.util.Util.sendCMDLocalBroadcast
import remix.myplayer.util.Util.sendLocalBroadcast


class Tag(context: Context, song: Song?) : ContextWrapper(context) {
  private val song: Song = song ?: getCurrentSong()
  private val tagEditor: TagEditor

  init {
    tagEditor = TagEditor(this.song.url)
  }

  fun detail() {
    if (!tagEditor.initSuccess) {
      ToastUtil.show(this, R.string.init_failed)
      return
    }
    R.string.export_fail
    val detailDialog = Theme.getBaseDialog(this)
        .title(R.string.song_detail)
        .customView(R.layout.dialog_song_detail, true)
        .positiveText(R.string.confirm)
        .build()
    detailDialog.show()
    detailDialog.customView?.let { root ->
      //歌曲路径
      root.song_detail_path.text = song.url
      //歌曲名称
      root.song_detail_name.text = song.displayName
      //歌曲大小
      root.song_detail_size.text = getString(R.string.cache_size, 1.0f * song.size / ByteConstants.MB)
      //歌曲时长
      root.song_detail_duration.text = Util.getTime(song.getDuration())
      //歌曲格式
      tagEditor.formatSingle()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(Schedulers.io())
          .subscribe(Consumer {
            root.song_detail_mime.text = it
          })
      //歌曲码率
      tagEditor.bitrateSingle()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(Schedulers.io())
          .subscribe(Consumer {
            root.song_detail_bit_rate.text = String.format("%s kb/s", it)
          })
      //歌曲采样率
      tagEditor.samplingSingle()
          .observeOn(AndroidSchedulers.mainThread())
          .subscribeOn(Schedulers.io())
          .subscribe(Consumer {
            root.song_detail_sample_rate.text = String.format("%s Hz", it)
          })

    }

  }

  fun edit() {
    if (!tagEditor.initSuccess) {
      ToastUtil.show(this, R.string.init_failed)
      return
    }
    val editDialog = Theme.getBaseDialog(this)
        .title(R.string.song_edit)
        .customView(R.layout.dialog_song_edit, true)
        .negativeText(R.string.cancel)
        .positiveText(R.string.confirm)
        .onPositive { dialog, which ->
          dialog.customView?.let { root ->
            val title = root.song_layout.editText?.text.toString().trim()
            val artist: String = root.artist_layout.editText?.text.toString().trim()
            val album: String = root.album_layout.editText?.text.toString().trim()
            val genre: String = root.genre_layout.editText?.text.toString().trim()
            val year: String = root.year_layout.editText?.text.toString().trim()
            val track: String = root.track_layout.editText?.text.toString().trim()
            if (TextUtils.isEmpty(title)) {
              ToastUtil.show(this, R.string.song_not_empty)
              return@onPositive
            }

            val oldSong = song
            tagEditor.save(song, title, album, artist, year, genre, track, "")
                .compose(RxUtil.applyScheduler())
                .subscribe({ song ->
                  sendCMDLocalBroadcast(Command.CHANGE_LYRIC)
                  sendLocalBroadcast(Intent(MusicService.TAG_CHANGE)
                      .putExtra(EXTRA_NEW_SONG, song)
                      .putExtra(EXTRA_OLD_SONG, oldSong))
                  ToastUtil.show(this, R.string.save_success)
                }, { throwable -> ToastUtil.show(this, R.string.save_error_arg, throwable.toString()) })
          }

        }.build()
    editDialog.show()

    editDialog.customView?.let { root ->
      val textInputTintColor = ThemeStore.getAccentColor()
      val editTintColor = ThemeStore.getAccentColor()
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
      root.track_layout.editText?.setText(tagEditor.getFieldValueSingle(FieldKey.TRACK).blockingGet())

      TextInputLayoutUtil.setAccent(root.genre_layout, textInputTintColor)
      TintHelper.setTintAuto(root.genre_layout.editText!!, editTintColor, false)
      root.genre_layout.editText?.setText(tagEditor.getFieldValueSingle(FieldKey.GENRE).blockingGet())

    }

  }
}


private class TextInputEditWatcher internal constructor(private val mInputLayout: TextInputLayout, private val mError: String) : TextWatcher {

  override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

  override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}

  override fun afterTextChanged(s: Editable?) {
    if (s == null || TextUtils.isEmpty(s.toString())) {
      mInputLayout.error = mError
    } else {
      mInputLayout.error = ""
    }
  }
}

