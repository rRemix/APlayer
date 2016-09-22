package remix.myplayer.adapter;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
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
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.ui.activity.RecetenlyActivity;
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
public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder>{
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;
    private ArrayList<MP3Item> mInfoList;
    private ArrayList<String> mSortList;
    private int mType;
    public static final int ALLSONG = 0;
    public static final int RECENTLY = 1;
    private static int mCurrentAnimPosition = 0;//当前播放高亮动画的索引
    private static int mOldAnimPostion = 0;

    public SongAdapter(Context context, int type) {
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

    public void setInfoList(ArrayList<MP3Item> list){
        mInfoList = list;
        notifyDataSetChanged();
    }

    @Override
    public SongViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new SongViewHolder(LayoutInflater.from(mContext).inflate(R.layout.song_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final SongViewHolder holder, final int position) {
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
            holder.mOther.setText(artist + "-" + album);
            //封面
//            holder.mImage.setImageURI(Uri.EMPTY);
//            new AsynLoadImage(holder.mImage).execute((int)temp.getAlbumId(),Constants.URL_ALBUM);
//            String urlPath = DBUtil.getImageUrl(temp.getAlbumId() + "", Constants.URL_ALBUM);
//            if(!TextUtils.isEmpty(urlPath)){
//                DraweeController controller = Fresco.newDraweeControllerBuilder()
//                        .setUri(Uri.fromFile(new File(urlPath)))
//                        .setOldController(holder.mImage.getController())
//                        .setAutoPlayAnimations(false)
//                        .build();
//                holder.mImage.setController(controller);
//            }

            DraweeController controller = Fresco.newDraweeControllerBuilder()
                    .setUri(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"),temp.getAlbumId()))
                    .setOldController(holder.mImage.getController())
                    .setAutoPlayAnimations(false)
                    .build();
            holder.mImage.setController(controller);

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
                    if(MainActivity.MultiChoice.isShow())
                        return;
                    MP3Item temp = allsong ? DBUtil.getMP3InfoById(Global.mAllSongList.get(holder.getAdapterPosition())) : mInfoList.get(holder.getAdapterPosition());
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
                    mOnItemClickLitener.onItemClick(v, holder.getAdapterPosition());
                }
            });
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(v,holder.getAdapterPosition());
                    return true;
                }
            });

        }

//        if(holder.mContainer.getTag(0) != null && (boolean)(holder.mContainer.getTag(0))){
//            MainActivity.MultiChoice.AddView(holder.mContainer);
//        } else {
//            holder.mContainer.setSelected(false);
//        }

        if(mType == ALLSONG){
            if(MainActivity.MultiChoice.getTag().equals(SongFragment.TAG) &&
                    MainActivity.MultiChoice.mSelectedPosition.contains(position)){
                MainActivity.MultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
        } else {
            if(RecetenlyActivity.MultiChoice.getTag().equals(RecetenlyActivity.TAG) &&
                    RecetenlyActivity.MultiChoice.mSelectedPosition.contains(position)){
                RecetenlyActivity.MultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }
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

    class AsynLoadImage extends AsyncTask<Object,Integer,String> {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView) {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(Object... params) {
            return DBUtil.getImageUrl(params[0].toString(), (int)params[1]);
        }
        @Override
        protected void onPostExecute(String url) {
            if(mImage != null && url != null) {
                DraweeController controller = Fresco.newDraweeControllerBuilder()
                        .setUri(Uri.parse("file:///" + url))
                        .setOldController(mImage.getController())
                        .setAutoPlayAnimations(false)
                        .build();
                mImage.setController(controller);

            }
        }
    }


    public static class SongViewHolder extends RecyclerView.ViewHolder{
        public TextView mName;
        public TextView mOther;
        public SimpleDraweeView mImage;
        public ColumnView mColumnView;
        public ImageButton mItemButton;
        public TextView mIndex;
        public View mContainer;
        public SongViewHolder(View itemView) {
            super(itemView);
            mContainer = itemView;
            mImage = (SimpleDraweeView)itemView.findViewById(R.id.song_head_image);
            mName = (TextView)itemView.findViewById(R.id.song_title);
            mOther = (TextView)itemView.findViewById(R.id.song_other);
            mColumnView = (ColumnView)itemView.findViewById(R.id.song_columnview);
            mItemButton = (ImageButton)itemView.findViewById(R.id.song_button);
            mIndex = (TextView)itemView.findViewById(R.id.song_index_letter);
        }
    }
}
