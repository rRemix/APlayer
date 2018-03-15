package remix.myplayer.adapter

import android.content.Context
import android.view.View
import android.widget.TextView
import butterknife.BindView
import com.facebook.drawee.view.SimpleDraweeView
import remix.myplayer.R
import remix.myplayer.adapter.holder.BaseViewHolder
import remix.myplayer.bean.mp3.Song
import remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE
import remix.myplayer.request.LibraryUriRequest
import remix.myplayer.request.RequestConfig
import remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType

/**
 * Created by Remix on 2018/3/15.
 */
class CustomSortAdapter (context: Context,layoutId: Int) : BaseAdapter<Song, CustomSortAdapter.CustomSortHolder>(context,layoutId){

    override fun convert(holder: CustomSortHolder?, song: Song?, position: Int) {
        if(song == null || holder == null)
            return
        holder.mTitle.text = song.Title
        holder.mAlbum.text = song.album
        //封面
        LibraryUriRequest(holder.mImage, getSearchRequestWithAlbumType(song), RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load()
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