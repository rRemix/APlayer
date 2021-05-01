package remix.myplayer.ui.adapter

import android.net.Uri
import android.view.View
import android.widget.CompoundButton
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemSongChooseBinding
import remix.myplayer.glide.GlideApp
import remix.myplayer.misc.interfaces.OnSongChooseListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.accentColor
import remix.myplayer.theme.ThemeStore.isLightTheme
import remix.myplayer.theme.TintHelper
import remix.myplayer.ui.adapter.SongChooseAdapter.SongChooseHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.ImageUriUtil
import java.util.*

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/21 10:02
 */
class SongChooseAdapter(layoutID: Int, private val checkListener: OnSongChooseListener) : BaseAdapter<Song, SongChooseHolder>(layoutID) {

  val checkedSong: ArrayList<Int> = ArrayList()

  override fun convert(holder: SongChooseHolder, song: Song?, position: Int) {
    if (song == null) {
      return
    }

    //歌曲名
    holder.binding.itemSong.text = song.showName
    //艺术家
    holder.binding.itemArtist.text = song.artist
    //封面
    GlideApp.with(holder.itemView)
        .load(song)
        .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .into(holder.binding.iv)

    //选中歌曲
    holder.binding.root.setOnClickListener { v: View? ->
      holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
      checkListener.OnSongChoose(checkedSong.size > 0)
    }
    val audioId = song.id
    TintHelper.setTint(holder.binding.checkbox, accentColor, !isLightTheme)
    holder.binding.checkbox.setOnCheckedChangeListener(null)
    holder.binding.checkbox.isChecked = true && checkedSong.contains(audioId)
    holder.binding.checkbox.setOnCheckedChangeListener { buttonView: CompoundButton?, isChecked: Boolean ->
      if (isChecked && !checkedSong.contains(audioId)) {
        checkedSong.add(audioId)
      } else if (!isChecked) {
        checkedSong.remove(Integer.valueOf(audioId))
      }
      checkListener.OnSongChoose(checkedSong.size > 0)
    }
  }

  class SongChooseHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemSongChooseBinding = ItemSongChooseBinding.bind(itemView)

  }
}