package remix.myplayer.ui

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.support.design.widget.TextInputLayout
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import com.facebook.common.util.ByteConstants
import kotlinx.android.synthetic.main.dialog_song_detail.view.*
import kotlinx.android.synthetic.main.dialog_song_edit.view.*
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.misc.tageditor.TagEditor
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.Command
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore
import remix.myplayer.util.Constants
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
            root.song_detail_name.text = song.displayname
            //歌曲大小
            root.song_detail_size.text = getString(R.string.cache_size, 1.0f * song.size / ByteConstants.MB)
            //歌曲格式
            root.song_detail_mime.text = tagEditor.format
            //歌曲时长
            root.song_detail_duration.text = Util.getTime(song.duration)
            //歌曲码率
            root.song_detail_bit_rate.text = String.format("%s kb/s", tagEditor.bitrate)
            //歌曲采样率
            root.song_detail_sample_rate.text = String.format("%s Hz", tagEditor.samplingRate)
        }

    }

    fun edit() {
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

                        tagEditor.save(song, title, album, artist, year, genre, track, "")
                                .compose(RxUtil.applyScheduler())
                                .subscribe({ song ->
                                    sendCMDLocalBroadcast(Command.CHANGE_LYRIC)
                                    sendLocalBroadcast(Intent(Constants.TAG_EDIT)
                                            .putExtra("newSong", song))
//                                    setCurrentSong(song)
                                    ToastUtil.show(this, R.string.save_success)
                                }, { throwable -> ToastUtil.show(this, R.string.save_error_arg, throwable.toString()) })
                    }

                }.build()
        editDialog.show()

        editDialog.customView?.let { root ->
            if (!ThemeStore.isLight()) {
                root.song_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
//                root.song_layout.editText?.background?.setColorFilter(Color.WHITE,PorterDuff.Mode.SRC_ATOP)
                root.album_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
                root.artist_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
                root.year_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
                root.genre_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
                root.track_layout.editText?.setTextColor(ThemeStore.getTextColorPrimary())
            }
            root.song_layout.editText?.addTextChangedListener(TextInputEditWatcher(root.song_layout, getString(R.string.song_not_empty)))
            root.song_layout.editText?.setText(song.title)
            root.album_layout.editText?.setText(song.album)
            root.artist_layout.editText?.setText(song.artist)
            root.year_layout.editText?.setText(song.year)
            root.genre_layout.editText?.setText(tagEditor.genreName)
            root.genre_layout.editText?.setText(tagEditor.trackNumber)
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

