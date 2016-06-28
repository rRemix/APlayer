package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.CircleImageView;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.Global;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class AllSongAdapter extends RecyclerView.Adapter<AllSongAdapter.AllSongHolder>{
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;
    private ArrayList<MP3Info> mInfoList;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;

    public AllSongAdapter(Context context,int type) {
        this.mContext = context;
        this.mType = type;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }

    public void setInfoList(ArrayList<MP3Info> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public AllSongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AllSongHolder(LayoutInflater.from(mContext).inflate(R.layout.allsong_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final AllSongHolder holder, final int position) {
        final boolean allsong = mType == ALLSONG;
        final MP3Info temp = mInfoList != null ? mInfoList.get(position) : new MP3Info();
        if(allsong && (mCursor == null || !mCursor.moveToPosition(position))){
            return;
        }


        //获得当前播放的歌曲
        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean highlight = (allsong ? mCursor.getInt(AllSongFragment.mSongId) : temp.getId()) == MusicService.getCurrentMP3().getId();
            holder.mName.setTextColor(highlight ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
            holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.GONE);

            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && highlight){
                holder.mColumnView.startAnim();
            }

            else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                holder.mColumnView.stopAnim();
            }
        }

        try {
            //设置歌曲名
            String name = CommonUtil.processInfo(allsong ? mCursor.getString(AllSongFragment.mTitleIndex) :temp.getTitle(),CommonUtil.SONGTYPE);
            holder.mName.setText(name);

            //艺术家与专辑
            String artist = CommonUtil.processInfo(allsong ? mCursor.getString(AllSongFragment.mArtistIndex) : temp.getArtist(),CommonUtil.ARTISTTYPE);
            String album = CommonUtil.processInfo(allsong ? mCursor.getString(AllSongFragment.mAlbumIndex) : temp.getAlbum(),CommonUtil.ALBUMTYPE);
            //封面
            holder.mOther.setText(artist + "-" + album);

        } catch (Exception e){
            e.printStackTrace();
        }

        ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" +
                        (allsong ? mCursor.getString(AllSongFragment.mAlbumIdIndex) : temp.getAlbumId()),
                holder.mImage);

        //选项Dialog
        if(holder.mItemButton != null) {
            holder.mItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MP3Info temp = allsong ? DBUtil.getMP3InfoById(Global.mAllSongList.get(position)) : mInfoList.get(position);
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Info", temp);
                    mContext.startActivity(intent);
                }
            });
        }

        //
        if(mOnItemClickLitener != null && holder.mRootView != null) {
            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if (pos >= 0)
                        mOnItemClickLitener.onItemClick(v, pos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        if(mType == ALLSONG)
            return mCursor != null ? mCursor.getCount() : 0;
        else
            return mInfoList != null ? mInfoList.size() : 0;
    }

    public static class AllSongHolder extends RecyclerView.ViewHolder{
        public TextView mName;
        public TextView mOther;
        public CircleImageView mImage;
        public ColumnView mColumnView;
        public ImageButton mItemButton;
        public View mRootView;
        public AllSongHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            mImage = (CircleImageView)itemView.findViewById(R.id.song_head_image);
            mName = (TextView)itemView.findViewById(R.id.displayname);
            mOther = (TextView)itemView.findViewById(R.id.detail);
            mColumnView = (ColumnView)itemView.findViewById(R.id.columnview);
            mItemButton = (ImageButton)itemView.findViewById(R.id.allsong_item_button);

        }
    }
}
