package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;
import remix.myplayer.ui.customviews.CircleImageView;
import remix.myplayer.ui.customviews.ColumnView;
import remix.myplayer.ui.popupwindow.OptionDialog;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/11/30.
 */
public class AllSongAdapter extends SimpleCursorAdapter implements ImpAdapter{
    public static AllSongAdapter mInstance;
    private Context mContext;
    private Cursor mCursor;
    private ColumnView mColumnView;
    //0:专辑 1:歌手
    private int mType = 0;
    public AllSongAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mInstance = this;
        mContext = context;
    }
    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
    }
    public void setType(int mType) {
        this.mType = mType;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null)
        {
            convertView = super.getView(position, convertView, parent);
            holder = new ViewHolder();
            holder.mImage = (CircleImageView)convertView.findViewById(R.id.song_head_image);
            holder.mName = (TextView)convertView.findViewById(R.id.displayname);
            holder.mOther = (TextView)convertView.findViewById(R.id.detail);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if(!mCursor.moveToPosition(position))
            return convertView;

        String name = mCursor.getString(AllSongFragment.mDisPlayNameIndex);
        name = name.substring(0, name.lastIndexOf("."));
        final MP3Info currentMP3 = MusicService.getCurrentMP3();
        if(currentMP3 != null){
            boolean flag = mCursor.getInt(AllSongFragment.mSongId) == MusicService.getCurrentMP3().getId();
            holder.mName.setTextColor(flag ? Color.parseColor("#782899") : Color.parseColor("#ffffffff"));
            mColumnView = (ColumnView)convertView.findViewById(R.id.columnview);
            mColumnView.setVisibility(flag ? View.VISIBLE : View.GONE);
            if(flag){
                Log.d("AllSongAdapter","song:" + name);
                Log.d("AllSongAdapter","isplay:" + MusicService.getIsplay());
            }
            //根据当前播放状态以及动画是否在播放，开启或者暂停的高亮动画
            if(MusicService.getIsplay() && !mColumnView.getStatus() && flag){
                mColumnView.startAnim();
            }

            else if(!MusicService.getIsplay() && mColumnView.getStatus()){
                Log.d("AllSongAdapter","停止动画 -- 歌曲名字:" + mCursor.getString(AllSongFragment.mDisPlayNameIndex));
                mColumnView.stopAnim();
            }
        }
        holder.mName.setText(name);

        String artist = mCursor.getString(AllSongFragment.mArtistIndex);
        String album = mCursor.getString(AllSongFragment.mAlbumIndex);
        holder.mOther.setText(artist + "-" + album);
        ImageLoader.getInstance().displayImage("content://media/external/audio/albumart/" + mCursor.getString(AllSongFragment.mAlbumIdIndex),
                holder.mImage);

        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.allsong_item_button);
        mItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MP3Info temp = DBUtil.getMP3InfoById(DBUtil.mAllSongList.get(position));
                Intent intent = new Intent(mContext, OptionDialog.class);
//                intent.putExtra("Position",position);
                intent.putExtra("MP3Info",temp);
                mContext.startActivity(intent);
            }
        });
        return convertView;
    }

    @Override
    public void UpdateColumnView(boolean isplay) {
        if(mColumnView != null){
            if(isplay)
                mColumnView.startAnim();
            else
                mColumnView.stopAnim();
        }
    }

    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final WeakReference mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = new WeakReference(imageView);
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return DBUtil.CheckBitmapBySongId(params[0],true);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                ((ImageView)mImageView.get()).setImageBitmap(bitmap);
        }
    }
    class ViewHolder
    {
        public TextView mName;
        public TextView mOther;
        public CircleImageView mImage;
    }
}




