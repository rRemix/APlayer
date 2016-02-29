package remix.myplayer.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.activities.SearchActivity;
import remix.myplayer.fragments.AlbumFragment;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2016/1/23.
 */
public class SearchAdapter extends SimpleCursorAdapter
{
    private Cursor mCurosr;
    private Context mContext;
    public SearchAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
    }
    public void setCursor(Cursor cursor){
        mCurosr = cursor;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null)
        {
            convertView = super.getView(position,convertView,parent);
            holder = new ViewHolder();
            holder.mImage = (SimpleDraweeView)convertView.findViewById(R.id.search_image);
            holder.mName = (TextView)convertView.findViewById(R.id.search_name);
            holder.mOther = (TextView)convertView.findViewById(R.id.search_detail);
            convertView.setTag(holder);
        }
        holder = (ViewHolder)convertView.getTag();

        if(mCurosr != null && mCurosr.moveToPosition(position))
        {
            try{
                String name = mCurosr.getString(SearchActivity.mDisplayNameIndex);
                holder.mName.setText(name.substring(0,name.lastIndexOf(".")));
                holder.mOther.setText(mCurosr.getString(SearchActivity.mArtistIndex) + "-" + mCurosr.getString(SearchActivity.mAlbumIndex));
                holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mCurosr.getInt(SearchActivity.mAlbumIdIndex)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return convertView;
    }
    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final ImageView mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = imageView;
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return DBUtil.CheckBitmapBySongId(params[0], true);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                mImageView.setImageBitmap(bitmap);
            else
                mImageView.setImageBitmap(BitmapFactory.decodeResource(mContext.getResources(),R.drawable.default_recommend));
        }
    }
    class ViewHolder
    {
        public SimpleDraweeView mImage;
        public TextView mName;
        public TextView mOther;
    }
}
