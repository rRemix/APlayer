package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;

/**
 * Created by taeja on 16-6-24.
 */
public class ChildHolderAdapter extends RecyclerView.Adapter<ChildHolderAdapter.ViewHoler> {
    private ArrayList<MP3Info> mInfoList;
    private Context mContext;
    private int mType;
    private String mArg;
    private OnItemClickListener mOnItemClickLitener;
    public ChildHolderAdapter(Context context, int type, String arg){
        this.mContext = context;
        this.mType = type;
        this.mArg = arg;
    }

    public void setList(ArrayList<MP3Info> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }


    @Override
    public ViewHoler onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHoler(LayoutInflater.from(mContext).inflate(R.layout.child_holder_item,null,false));
    }

    @Override
    public void onBindViewHolder(final ViewHoler holder, int position) {
        final MP3Info temp = mInfoList.get(position);
        if(temp == null)
            return;

        //获得正在播放的歌曲
        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean flag = temp.getId() == currentMP3.getId();
            holder.mTitle.setTextColor(flag ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
            holder.mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);

            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && flag){
                holder.mColumnView.startAnim();
            }
            else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                holder.mColumnView.stopAnim();
            }
        }

        //设置标题
        holder.mTitle.setText(CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE));

        if(holder.mButton != null) {
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Info", temp);
                    if (mType == Constants.PLAYLIST_HOLDER) {
                        intent.putExtra("IsDeletePlayList", true);
                        intent.putExtra("PlayListName", mArg);
                    }
                    mContext.startActivity(intent);
                }
            });
        }

        if(holder.mRootView != null && mOnItemClickLitener != null){
            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition());
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mInfoList == null ? 0 : mInfoList.size();
    }

    public static class ViewHoler extends RecyclerView.ViewHolder{
        public TextView mTitle;
        public ImageButton mButton;
        public ColumnView mColumnView;
        public View mRootView;
        public ViewHoler(View itemView) {
            super(itemView);
            mRootView = itemView;
            mTitle = (TextView)itemView.findViewById(R.id.album_holder_item_title);
            mButton = (ImageButton)itemView.findViewById(R.id.song_item_button);
            mColumnView = (ColumnView)itemView.findViewById(R.id.columnview);
        }
    }
}
