package remix.myplayer.ui.adapter

import android.view.View
import com.bumptech.glide.Glide
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemCustomSortBinding
import remix.myplayer.theme.Theme
import remix.myplayer.ui.adapter.holder.BaseViewHolder

/**
 * Created by Remix on 2018/3/15.
 */
class CustomSortAdapter(layoutId: Int) : BaseAdapter<Song, CustomSortAdapter.CustomSortHolder>(layoutId) {

  override fun convert(holder: CustomSortHolder, data: Song?, position: Int) {
    if (data == null) {
      return
    }
    holder.binding.itemSong.text = data.title
    holder.binding.itemAlbum.text = data.album

    //封面
    Glide.with(holder.itemView)
        .load(data)
        .centerCrop()
        .placeholder(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .error(Theme.resolveDrawable(holder.itemView.context, R.attr.default_album))
        .into(holder.binding.iv)
  }

  class CustomSortHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemCustomSortBinding.bind(itemView)
  }
}