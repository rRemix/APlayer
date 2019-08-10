package remix.myplayer.ui.adapter

import android.net.Uri
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import io.reactivex.disposables.Disposable
import remix.myplayer.R
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.ui.adapter.holder.BaseViewHolder
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType

/**
 * Created by Remix on 2018/3/15.
 */
class CustomSortAdapter(layoutId: Int) : BaseAdapter<Song, CustomSortAdapter.CustomSortHolder>(layoutId) {

  override fun convert(holder: CustomSortHolder?, song: Song?, position: Int) {
    if (song == null || holder == null)
      return
    holder.mTitle.text = song.title
    holder.mAlbum.text = song.album
    //封面

    val disposable = LibraryUriRequest(holder.mImage, getSearchRequestWithAlbumType(song), RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load()
    holder.mImage.tag = disposable
  }

  override fun onViewRecycled(holder: CustomSortHolder) {
    super.onViewRecycled(holder)
    holder.let {
      if (it.mImage.tag != null) {
        val disposable = it.mImage.tag as Disposable
        if (!disposable.isDisposed)
          disposable.dispose()
      }
      holder.mImage.setImageURI(Uri.EMPTY)
    }

  }

  class CustomSortHolder(itemView: View) : BaseViewHolder(itemView) {
    @BindView(R.id.item_img)
    lateinit var mImage: SimpleDraweeView
    @BindView(R.id.item_song)
    lateinit var mTitle: TextView
    @BindView(R.id.item_album)
    lateinit var mAlbum: TextView
  }
}