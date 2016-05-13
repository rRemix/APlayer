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
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.CircleImageView;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.utils.Global;

/**
 * Created by Remix on 2016/4/11.
 */
public class TestAdapter extends RecyclerView.Adapter<TestAdapter.ViewHolder>{
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;

    public TestAdapter(Cursor cursor, Context context) {
        this.mCursor = cursor;
        this.mContext = context;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }
    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.allsong_item,null,false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if(mCursor.moveToPosition(position)){
            //设置歌曲名
            String name = mCursor.getString(AllSongFragment.mDisPlayNameIndex);
            name = name.substring(0, name.lastIndexOf("."));
            //获得当前播放的歌曲
            final MP3Info currentMP3 = MusicService.getCurrentMP3();
            //判断该歌曲是否是正在播放的歌曲
            //如果是,高亮该歌曲，并显示动画
            if(currentMP3 != null){
                boolean flag = mCursor.getInt(AllSongFragment.mSongId) == MusicService.getCurrentMP3().getId();
                holder.mName.setTextColor(flag ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
                holder.mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);
                if(flag){
                    Log.d("AllSongAdapter", "song:" + name);
                    Log.d("AllSongAdapter","isplay:" + MusicService.getIsplay());
                }
                //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
                if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && flag){
                    holder.mColumnView.startAnim();
                }

                else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                    Log.d("AllSongAdapter","停止动画 -- 歌曲名字:" + mCursor.getString(AllSongFragment.mDisPlayNameIndex));
                    holder.mColumnView.stopAnim();
                }
            }
            name = name.indexOf("unknown") > 0 ? "未知歌曲" : name;
            holder.mName.setText(name);

            //艺术家与专辑
            String artist = mCursor.getString(AllSongFragment.mArtistIndex);
            String album = mCursor.getString(AllSongFragment.mAlbumIndex);
            artist = artist.indexOf("unknown") > 0 ? "未知艺术家" : artist;
            album = album.indexOf("unknown") > 0 ? "未知专辑" : album;
            //封面
            holder.mOther.setText(artist + "-" + album);
            ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" + mCursor.getString(AllSongFragment.mAlbumIdIndex),
                    holder.mImage);
            //选项Dialog
            holder.mItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MP3Info temp = DBUtil.getMP3InfoById(Global.mAllSongList.get(position));
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Info",temp);
                    mContext.startActivity(intent);
                }
            });
            //
            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    if(pos >=0 )
                        mOnItemClickLitener.onItemClick(v,pos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public TextView mName;
        public TextView mOther;
        public CircleImageView mImage;
        public ColumnView mColumnView;
        public ImageView mItemButton;
        public View mRootView;
        public ViewHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            mImage = (CircleImageView)itemView.findViewById(R.id.song_head_image);
            mName = (TextView)itemView.findViewById(R.id.displayname);
            mOther = (TextView)itemView.findViewById(R.id.detail);
            mColumnView = (ColumnView)itemView.findViewById(R.id.columnview);
            mItemButton = (ImageView)itemView.findViewById(R.id.allsong_item_button);
        }
    }
}
