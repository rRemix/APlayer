package remix.myplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.AllSongHolder>{
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;
    private ArrayList<MP3Item> mInfoList;
    private ArrayList<String> mSortList;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;
    private static int mCurrentAnimPosition = 0;//当前播放高亮动画的索引

    public SongAdapter(Context context, int type) {
        this.mContext = context;
        this.mType = type;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }

    public void setSortList(ArrayList<String> list){
        this.mSortList = list;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }

    public void setInfoList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public AllSongHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AllSongHolder(LayoutInflater.from(mContext).inflate(R.layout.song_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final AllSongHolder holder, final int position) {
        final boolean allsong = mType == ALLSONG;
        if(allsong && (mCursor == null || !mCursor.moveToPosition(position))){
            return;
        }
        if(!allsong && (mInfoList == null || mInfoList.get(position) == null))
            return;

        final MP3Item temp = allsong ? new MP3Item(mCursor.getInt(SongFragment.mSongId),
                mCursor.getString(SongFragment.mDisPlayNameIndex),
                mCursor.getString(SongFragment.mTitleIndex),
                mCursor.getString(SongFragment.mAlbumIndex),
                mCursor.getInt(SongFragment.mAlbumIdIndex),
                mCursor.getString(SongFragment.mArtistIndex),0,"","",0,"") :
                mInfoList.get(position);

        //获得当前播放的歌曲
        final MP3Item currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean highlight = temp.getId() == MusicService.getCurrentMP3().getId();
            holder.mName.setTextColor(highlight ?
                    ColorUtil.getColor(ThemeStore.isDay() ? ThemeStore.MATERIAL_COLOR_PRIMARY : R.color.purple_782899):
                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
            holder.mColumnView.setVisibility(highlight ? View.VISIBLE : View.GONE);
            if(highlight)
                mCurrentAnimPosition = position;
            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !holder.mColumnView.getStatus() && highlight){
                holder.mColumnView.startAnim();
            }

            else if(!MusicService.getIsplay() && holder.mColumnView.getStatus()){
                holder.mColumnView.stopAnim();
            }
        }

        try {
            boolean isDay = ThemeStore.THEME_MODE == ThemeStore.DAY;
            //设置歌曲名
            String name = CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE);
            holder.mName.setText(name);

            //艺术家与专辑
            String artist = CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE);
            String album = CommonUtil.processInfo(temp.getAlbum(),CommonUtil.ALBUMTYPE);
            //封面
            holder.mOther.setText(artist + "-" + album);

        } catch (Exception e){
            e.printStackTrace();
        }

        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"),temp.getAlbumId()))
                .setOldController(holder.mImage.getController())
                .setAutoPlayAnimations(false)
                .build();
        holder.mImage.setController(controller);

        //选项Dialog
        if(holder.mItemButton != null) {
            //设置按钮着色
            Drawable drawable = mContext.getResources().getDrawable(R.drawable.list_icn_more);
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            holder.mItemButton.setImageDrawable(Theme.TintDrawable(drawable,tintColor));
            holder.mItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    MP3Item temp = allsong ? DBUtil.getMP3InfoById(Global.mAllSongList.get(position)) : mInfoList.get(position);
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Item", temp);
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
        if(mType == ALLSONG) {
            return mCursor != null && !mCursor.isClosed() ?mCursor.getCount() : 0;
        }
        else
            return mInfoList != null ? mInfoList.size() : 0;
    }

    public static class AllSongHolder extends RecyclerView.ViewHolder{
        public TextView mName;
        public TextView mOther;
        public SimpleDraweeView mImage;
        public ColumnView mColumnView;
        public ImageButton mItemButton;
        public TextView mIndex;
        public View mRootView;
        public AllSongHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            mImage = (SimpleDraweeView)itemView.findViewById(R.id.song_head_image);
            mName = (TextView)itemView.findViewById(R.id.song_title);
            mOther = (TextView)itemView.findViewById(R.id.song_other);
            mColumnView = (ColumnView)itemView.findViewById(R.id.song_columnview);
            mItemButton = (ImageButton)itemView.findViewById(R.id.song_button);
            mIndex = (TextView)itemView.findViewById(R.id.song_index_letter);

        }
    }
}
