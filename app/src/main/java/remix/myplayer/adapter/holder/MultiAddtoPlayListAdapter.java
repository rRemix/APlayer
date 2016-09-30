package remix.myplayer.adapter.holder;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.DensityUtil;

/**
 * @ClassName MultiAddtoPlayListAdapter
 * @Description 多选菜单添加到播放列表适配器
 * @Author Xiaoborui
 * @Date 2016/9/30 14:47
 */
public class MultiAddtoPlayListAdapter extends RecyclerView.Adapter<MultiAddtoPlayListAdapter.MultiAddtoPlayListAdapterHolder> {
    private final ArrayList<String> mPlaylistName;
    private final int HEADER = 0;
    private final int OTHER = 1;
    private final Context mContext;
    private OnItemClickListener mOnItemClickLitener;

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }

    public MultiAddtoPlayListAdapter(Context context,ArrayList<String> itemList){
        mPlaylistName = itemList;
        mContext = context;
    }

    @Override
    public MultiAddtoPlayListAdapterHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MultiAddtoPlayListAdapterHolder(LayoutInflater.from(mContext).inflate(R.layout.item_playlist,null,false));
    }

    @Override
    public void onBindViewHolder(MultiAddtoPlayListAdapterHolder holder, final int position) {
        if(position == 0){
            holder.mPlayList.setText(R.string.create_playlist);
            holder.mPlayList.setTextSize(TypedValue.COMPLEX_UNIT_SP,16);
            holder.mPlayList.setTextColor(ThemeStore.getTextColor());
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.mPlayList.getLayoutParams();
            lp.gravity = Gravity.CENTER_HORIZONTAL;
            lp.topMargin = DensityUtil.dip2px(mContext,4);
            lp.bottomMargin = DensityUtil.dip2px(mContext,4);
            holder.mPlayList.setLayoutParams(lp);

        } else {
            if(mPlaylistName == null || mPlaylistName.size() <= 0 || position - 1 > mPlaylistName.size())
                return;
            holder.mPlayList.setText(mPlaylistName.get(position - 1));
            holder.mPlayList.setTextColor(ThemeStore.getTextColorPrimary());
            holder.mPlayList.setTextSize(TypedValue.COMPLEX_UNIT_SP,18);
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.mPlayList.getLayoutParams();
            lp.setMargins(DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,10),DensityUtil.dip2px(mContext,4));
            holder.mPlayList.setLayoutParams(lp);
        }

        holder.mContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnItemClickLitener.onItemClick(v,position);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEADER : OTHER;
    }

    @Override
    public int getItemCount() {
        return mPlaylistName != null && mPlaylistName.size() > 0 ? mPlaylistName.size() + 1 : 1;
    }

    public static class MultiAddtoPlayListAdapterHolder extends BaseViewHolder {
        @BindView(R.id.item_playlist_text)
        TextView mPlayList;
        LinearLayout mContainer;
        public MultiAddtoPlayListAdapterHolder(View itemView) {
            super(itemView);
            mContainer = (LinearLayout) itemView;
        }
    }
}
