package remix.myplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.generic.GenericDraweeHierarchy;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.model.MP3Item;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.service.MusicService;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.ColumnView;
import remix.myplayer.ui.dialog.OptionDialog;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;

/**
 * 全部歌曲和最近添加页面所用adapter
 */

/**
 * Created by Remix on 2016/4/11.
 */
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private Cursor mCursor;
    private Context mContext;
    private MultiChoice mMultiChoice;
    private OnItemClickListener mOnItemClickLitener;
    private ArrayList<MP3Item> mInfoList;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;

    public SongAdapter(Context context, MultiChoice multiChoice,int type) {
        this.mContext = context;
        this.mMultiChoice = multiChoice;
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

    public void setInfoList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SongViewHolder(LayoutInflater.from(mContext).inflate(R.layout.item_song_recycle,null,false));
    }

    @Override
    public void onBindViewHolder(final SongViewHolder holder, final int position) {
        if(mCursor == null || !mCursor.moveToPosition(position)){
            return;
        }

        final MP3Item temp = new MP3Item(mCursor.getInt(SongFragment.mSongId),
                mCursor.getString(SongFragment.mDisPlayNameIndex),
                mCursor.getString(SongFragment.mTitleIndex),
                mCursor.getString(SongFragment.mAlbumIndex),
                mCursor.getInt(SongFragment.mAlbumIdIndex),
                mCursor.getString(SongFragment.mArtistIndex),0,"","",0,"");

        //获得当前播放的歌曲
        final MP3Item currentMP3 = MusicService.getCurrentMP3();
        //判断该歌曲是否是正在播放的歌曲
        //如果是,高亮该歌曲，并显示动画
        if(currentMP3 != null){
            boolean highlight = temp.getId() == MusicService.getCurrentMP3().getId();
            holder.mName.setTextColor(highlight ?
                    ThemeStore.getStressColor():
                    ColorUtil.getColor(ThemeStore.isDay() ? R.color.day_textcolor_primary : R.color.night_textcolor_primary));
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
            String name = CommonUtil.processInfo(temp.getTitle(),CommonUtil.SONGTYPE);
            holder.mName.setText(name);

            //艺术家与专辑
            String artist = CommonUtil.processInfo(temp.getArtist(),CommonUtil.ARTISTTYPE);
            String album = CommonUtil.processInfo(temp.getAlbum(),CommonUtil.ALBUMTYPE);
            holder.mOther.setText(artist + "-" + album);
            //封面
//            holder.mImage.setImageURI(Uri.EMPTY);
            new AsynLoadImage(holder.mImage).execute(temp.getAlbumId(), Constants.ALBUM,false);

//            holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"),temp.getAlbumId()));
//

//            DraweeController controller = Fresco.newDraweeControllerBuilder()
//                    .setUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"),temp.getAlbumId()))
//                    .setOldController(holder.mImage.getController())
//                    .setAutoPlayAnimations(false)
//                    .build();
//            holder.mImage.setController(controller);

        } catch (Exception e){
            e.printStackTrace();
        }

        //选项Dialog
        if(holder.mItemButton != null) {
            //设置按钮着色
            int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
            Theme.TintDrawable(holder.mItemButton,R.drawable.list_icn_more,tintColor);
            holder.mItemButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(mMultiChoice.isShow())
                        return;
                    Intent intent = new Intent(mContext, OptionDialog.class);
                    intent.putExtra("MP3Item", temp);
                    mContext.startActivity(intent);
                }
            });
        }

        if(mOnItemClickLitener != null && holder.mContainer != null) {
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(v, holder.getLayoutPosition());
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(v,holder.getLayoutPosition());
                    return true;
                }
            });

        }

        if(mType == ALLSONG){
            if(MultiChoice.TAG.equals(SongFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
//        } else {
//            if(MultiChoice.TAG.equals(RecetenlyActivity.TAG) &&
//                    RecetenlyActivity.MultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
//                RecetenlyActivity.MultiChoice.AddView(holder.mContainer);
//            } else {
//                holder.mContainer.setSelected(false);
//            }
        }
    }

    @Override
    public int getItemCount() {
        return mCursor != null && !mCursor.isClosed() ? mCursor.getCount() : 0;
    }

    class MyImg extends SimpleDraweeView{

        public MyImg(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
            super(context, attrs, defStyleAttr, defStyleRes);
        }

        public MyImg(Context context, GenericDraweeHierarchy hierarchy) {
            super(context, hierarchy);
        }

        public MyImg(Context context) {
            super(context);
        }

        public MyImg(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyImg(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }
    }


    public static class SongViewHolder extends BaseViewHolder{
        @BindView(R.id.song_title)
        TextView mName;
        @BindView(R.id.song_other)
        TextView mOther;
        @BindView(R.id.song_head_image)
        SimpleDraweeView mImage;
        @BindView(R.id.song_columnview)
        ColumnView mColumnView;
        @BindView(R.id.song_button)
        ImageButton mItemButton;
        @BindView(R.id.item_root)
        View mContainer;
        public SongViewHolder(View itemView) {
            super(itemView);
        }
    }
}
