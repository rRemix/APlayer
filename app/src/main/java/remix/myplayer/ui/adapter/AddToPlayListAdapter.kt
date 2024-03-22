package remix.myplayer.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import remix.myplayer.databinding.ItemPlaylistAddtoBinding
import remix.myplayer.db.room.model.PlayList
import remix.myplayer.ui.adapter.AddToPlayListAdapter.PlayListAddToHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder

/**
 * Created by taeja on 16-2-1.
 */
/**
 * 将歌曲添加到播放列表的适配器
 */
class AddToPlayListAdapter(layoutId: Int) : BaseAdapter<PlayList, PlayListAddToHolder>(layoutId) {

  override fun convert(holder: PlayListAddToHolder, data: PlayList?, position: Int) {
    if (data == null) {
      return
    }
    holder.binding.playlistAddtoText.text = data.name
    holder.binding.playlistAddtoText.tag = data.id
    holder.binding.itemRoot.setOnClickListener { v: View? ->
      onItemClickListener?.onItemClick(v, position)
    }
  }

  class PlayListAddToHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemPlaylistAddtoBinding = ItemPlaylistAddtoBinding.bind(itemView)
  }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlayListAddToHolder {
    return PlayListAddToHolder(LayoutInflater.from(parent.context).inflate(layoutId, parent, false))
  }
}