package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.fragments.AllSongFragment;
import remix.myplayer.ui.CircleImageView;
import remix.myplayer.ui.SelectedPopupWindow;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/11/30.
 */
public class SongListAdapter extends SimpleCursorAdapter {
    public static SongListAdapter mInstance;
    private Context mContext;
    private boolean mScrollState = false;
    //0:专辑 1:歌手
    private int mType = 0;
    public SongListAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mInstance = this;
        mContext = context;
    }
    public void setScrollState(boolean isScroll)
    {
        mScrollState = isScroll;
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
            holder.mImage = (CircleImageView)convertView.findViewById(R.id.homepage_head_image);
            holder.mName = (TextView)convertView.findViewById(R.id.displayname);
            holder.mOther = (TextView)convertView.findViewById(R.id.detail);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if((getItem(position)) == null)
            return convertView;
        String name = ((Cursor)getItem(position)).getString(AllSongFragment.mDisPlayNameIndex);
        name = name.substring(0, name.lastIndexOf("."));
        String artist = ((Cursor)getItem(position)).getString(AllSongFragment.mArtistIndex);
        String album = ((Cursor)getItem(position)).getString(AllSongFragment.mAlbumIndex);

        holder.mName.setText(name);
        holder.mOther.setText(artist + "-" + album);
        AsynLoadImage task = new AsynLoadImage(holder.mImage);
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,((Cursor) getItem(position)).getInt(AllSongFragment.mSongId));

        final ImageView mItemButton = (ImageView)convertView.findViewById(R.id.allsong_item_button);
        mItemButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(mContext, SelectedPopupWindow.class);
                intent.putExtra("Position",position);
                mContext.startActivity(intent);
            }
        });
        return convertView;
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




