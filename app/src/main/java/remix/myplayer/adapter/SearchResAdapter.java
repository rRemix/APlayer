package remix.myplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.activity.SearchActivity;

/**
 * Created by Remix on 2016/1/23.
 */

/**
 * 搜索结果的适配器
 */
public class SearchResAdapter extends BaseAdapter<SearchResAdapter.SearchResHolder> {
    public SearchResAdapter(Context context){
        super(context);
    }

    @Override
    public SearchResHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SearchResHolder(LayoutInflater.from(mContext).inflate(R.layout.item_search_reulst,null));
    }

    @Override
    public void onBindViewHolder(final SearchResHolder holder, final int position) {
        if(mCursor != null && mCursor.moveToPosition(position)) {
            try{
                String name = mCursor.getString(SearchActivity.mDisplayNameIndex);
                holder.mName.setText(name.substring(0,name.lastIndexOf(".")));
                holder.mOther.setText(mCursor.getString(SearchActivity.mArtistIndex) + "-" + mCursor.getString(SearchActivity.mAlbumIndex));
                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), mCursor.getInt(SearchActivity.mAlbumIdIndex)))
                        .setOldController(holder.mImage.getController())
                        .setAutoPlayAnimations(false)
                        .build();
                holder.mImage.setController(controller);
                if(mOnItemClickLitener != null && holder.mRooView != null){
                    holder.mRooView.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class SearchResHolder extends BaseViewHolder {
        @BindView(R.id.reslist_item)
        public RelativeLayout mRooView;
        @BindView(R.id.search_image)
        public SimpleDraweeView mImage;
        @BindView(R.id.search_name)
        public TextView mName;
        @BindView(R.id.search_detail)
        public TextView mOther;
        public SearchResHolder(View itemView){
           super(itemView);
        }
    }
}
