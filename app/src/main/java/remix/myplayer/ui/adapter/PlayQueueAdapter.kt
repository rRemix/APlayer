package remix.myplayer.ui.adapter

import android.view.View
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemPlayqueueBinding
import remix.myplayer.db.room.DatabaseRepository.Companion.getInstance
import remix.myplayer.helper.MusicServiceRemote.getCurrentSong
import remix.myplayer.request.network.RxUtil
import remix.myplayer.service.Command
import remix.myplayer.theme.ThemeStore
import remix.myplayer.theme.ThemeStore.textColorPrimary
import remix.myplayer.ui.adapter.PlayQueueAdapter.PlayQueueHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.Util

/**
 * Created by Remix on 2015/12/2.
 */
/**
 * 正在播放列表的适配器
 */
class PlayQueueAdapter(layoutId: Int) : BaseAdapter<Song, PlayQueueHolder>(layoutId) {
  private val accentColor: Int = ThemeStore.accentColor
  private val textColor: Int = textColorPrimary

  override fun convert(holder: PlayQueueHolder, song: Song?, position: Int) {
    if (song == null) {
      return
    }
    if (song == Song.EMPTY_SONG) {
      //歌曲已经失效
      holder.binding.playlistItemName.setText(R.string.song_lose_effect)
      holder.binding.playlistItemArtist.visibility = View.GONE
      return
    }
    //设置歌曲与艺术家
    holder.binding.playlistItemName.text = song.showName
    holder.binding.playlistItemArtist.text = song.artist
    holder.binding.playlistItemArtist.visibility = View.VISIBLE
    //高亮
    if (getCurrentSong().id == song.id) {
      holder.binding.playlistItemName.setTextColor(accentColor)
    } else {
//                holder.mSong.setTextColor(Color.parseColor(ThemeStore.isDay() ? "#323335" : "#ffffff"));
      holder.binding.playlistItemName.setTextColor(textColor)
    }
    //删除按钮
    holder.binding.playqueueDelete.setOnClickListener { v: View? ->
      getInstance()
          .deleteFromPlayQueue(listOf(song.id))
          .compose(RxUtil.applySingleScheduler())
          .subscribe { num: Int ->
            //删除的是当前播放的歌曲
            if (num > 0 && getCurrentSong().id == song.id) {
              Util.sendCMDLocalBroadcast(Command.NEXT)
            }
          }
    }
    holder.binding.itemRoot.setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, holder.adapterPosition) }
  }

  class PlayQueueHolder(view: View) : BaseViewHolder(view) {
    val binding: ItemPlayqueueBinding = ItemPlayqueueBinding.bind(view)
  }
}