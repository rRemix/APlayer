package remix.myplayer.ui.adapter

import android.net.Uri
import android.view.View
import io.reactivex.disposables.Disposable
import remix.myplayer.bean.mp3.Song
import remix.myplayer.databinding.ItemCustomSortBinding
import remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType

/**
 * Created by Remix on 2018/3/15.
 */
class CustomSortAdapter(layoutId: Int) : BaseAdapter<Song, CustomSortAdapter.CustomSortHolder>(layoutId) {

  override fun convert(holder: CustomSortHolder, data: Song?, position: Int) {
    if(data == null){
      return
    }
    holder.binding.itemSong.text = data.title
    holder.binding.itemAlbum.text = data.album

    //封面
    val disposable = LibraryUriRequest(
        holder.binding.itemImg,
        getSearchRequestWithAlbumType(data),
        RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()
    ).load()
    holder.binding.itemImg.tag = disposable
  }

  override fun onViewRecycled(holder: CustomSortHolder) {
    super.onViewRecycled(holder)
    holder.let {
      if (it.binding.itemImg.tag != null) {
        val disposable = it.binding.itemImg.tag as Disposable
        if (!disposable.isDisposed) disposable.dispose()
      }
      holder.binding.itemImg.setImageURI(Uri.EMPTY)
    }

  }

  class CustomSortHolder(itemView: View) : BaseViewHolder(itemView) {
    val binding = ItemCustomSortBinding.bind(itemView)
  }
}