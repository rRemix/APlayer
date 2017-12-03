package remix.myplayer.adapter;

import android.content.Context;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.model.mp3.Album;
import remix.myplayer.model.mp3.Song;
import remix.myplayer.request.AlbumUriRequest;
import remix.myplayer.request.RequestConfig;

import static remix.myplayer.request.ImageUriRequest.LIST_IMAGE_SIZE;

/**
 * Created by Remix on 2016/1/23.
 */

/**
 * 搜索结果的适配器
 */
public class SearchResAdapter extends BaseAdapter<Song,SearchResAdapter.SearchResHolder> {
    public SearchResAdapter(Context context,int layoutId){
        super(context,layoutId);
    }


    @Override
    protected void convert(final SearchResHolder holder, Song song, int position) {
        holder.mName.setText(song.getTitle());
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
        //封面
        new AlbumUriRequest(holder.mImage,
                new Album(song.getAlbumId(),song.getAlbum(),0,song.getArtist()),
                new RequestConfig.Builder(LIST_IMAGE_SIZE,LIST_IMAGE_SIZE).build()).load();
        if(mOnItemClickLitener != null && holder.mRooView != null){
            holder.mRooView.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition()));
        }
    }

    static class SearchResHolder extends BaseViewHolder {
        @BindView(R.id.reslist_item)
        RelativeLayout mRooView;
        @BindView(R.id.search_image)
        SimpleDraweeView mImage;
        @BindView(R.id.search_name)
        TextView mName;
        @BindView(R.id.search_detail)
        TextView mOther;
        SearchResHolder(View itemView){
           super(itemView);
        }
    }
}
