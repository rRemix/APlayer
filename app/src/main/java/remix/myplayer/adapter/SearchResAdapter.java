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
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.MediaStoreUtil;

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
            holder.mName.setText(CommonUtil.processInfo(mCursor.getString(SearchActivity.mTitleIndex),CommonUtil.SONGTYPE));
            holder.mOther.setText(mCursor.getString(SearchActivity.mArtistIndex) + "-" + mCursor.getString(SearchActivity.mAlbumIndex));
            //封面
            MediaStoreUtil.setImageUrl(holder.mImage,mCursor.getInt(SearchActivity.mAlbumIdIndex));
            if(mOnItemClickLitener != null && holder.mRooView != null){
                holder.mRooView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                    }
                });
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
