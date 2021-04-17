package remix.myplayer.ui.adapter

import android.annotation.SuppressLint
import android.net.Uri
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import io.reactivex.disposables.Disposable
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemSearchReulstBinding
import remix.myplayer.misc.menu.SongPopupListener
import remix.myplayer.request.ImageUriRequest
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.theme.Theme
import remix.myplayer.theme.ThemeStore.libraryBtnColor
import remix.myplayer.ui.adapter.SearchAdapter.SearchResHolder
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.ImageUriUtil

/**
 * Created by Remix on 2016/1/23.
 */
/**
 * 搜索结果的适配器
 */
class SearchAdapter(layoutId: Int) : BaseAdapter<Song, SearchResHolder>(layoutId) {
  override fun onViewRecycled(holder: SearchResHolder) {
    super.onViewRecycled(holder)
    if (holder.binding.searchImage.tag != null) {
      val disposable = holder.binding.searchImage.tag as Disposable
      if (!disposable.isDisposed) {
        disposable.dispose()
      }
    }
    holder.binding.searchImage.setImageURI(Uri.EMPTY)
  }

  @SuppressLint("RestrictedApi")
  override fun convert(holder: SearchResHolder, song: Song?, position: Int) {
    if(song == null){
      return
    }
    holder.binding.searchName.text = song.title
    holder.binding.searchDetail.text = String.format("%s-%s", song.artist, song.album)
    //封面
    val disposable = LibraryUriRequest(holder.binding.searchImage,
        ImageUriUtil.getSearchRequestWithAlbumType(song),
        RequestConfig.Builder(ImageUriRequest.SMALL_IMAGE_SIZE, ImageUriRequest.SMALL_IMAGE_SIZE).build()).load()
    holder.binding.searchImage.tag = disposable

    //设置按钮着色
    val tintColor = libraryBtnColor
    Theme.tintDrawable(holder.binding.searchButton, R.drawable.icon_player_more, tintColor)
    holder.binding.searchButton.setOnClickListener { v: View? ->
      val popupMenu = PopupMenu(holder.itemView.context, holder.binding.searchButton, Gravity.END)
      popupMenu.menuInflater.inflate(R.menu.menu_song_item, popupMenu.menu)
      popupMenu.setOnMenuItemClickListener(
          SongPopupListener((holder.itemView.context as AppCompatActivity), song, false, ""))
      popupMenu.show()
    }
    if (onItemClickListener != null) {
      holder.binding.reslistItem.setOnClickListener { v: View? -> onItemClickListener?.onItemClick(v, holder.adapterPosition) }
    }
  }

  class SearchResHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding: ItemSearchReulstBinding = ItemSearchReulstBinding.bind(itemView)

  }
}