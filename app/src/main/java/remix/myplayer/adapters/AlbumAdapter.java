package remix.myplayer.adapters;

import android.content.Context;
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
import remix.myplayer.fragments.AlbumFragment;
import remix.myplayer.utils.AlbumInfo;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/4.
 */
public class AlbumAdapter extends SimpleCursorAdapter {
    private Context mContext;

    public AlbumAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = new ViewHolder();

        if(convertView == null)
        {
            convertView = super.getView(position, convertView, parent);
            holder = new ViewHolder();
            holder.mName = (TextView)convertView.findViewById(R.id.artist_album_item_name);
            holder.mNum = (TextView)convertView.findViewById(R.id.artist_album_item_count);
            holder.mImage = (ImageView)convertView.findViewById(R.id.artist_album_item_image);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if((getItem(position)) == null)
            return convertView;
        String name = ((Cursor)getItem(position)).getString(AlbumFragment.mAlbumIndex);
        String num = ((Cursor)getItem(position)).getString(AlbumFragment.mNumofSongsIndex);
        int id = ((Cursor)getItem(position)).getInt(AlbumFragment.mAlbumIdIndex);

        holder.mName.setText(name);
        holder.mNum.setText(num + "首");
        AsynLoadImage task = new AsynLoadImage(holder.mImage);
        task.execute(id);

        return convertView;

    }

    //<Params, Progress, Result>
    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final WeakReference mImageView;
//        private ImageView mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = new WeakReference(imageView);
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return Utility.CheckBitmapByAlbumId(params[0],true);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                ((ImageView)mImageView.get()).setImageBitmap(bitmap);
            else
                ((ImageView)mImageView.get()).setImageResource(R.drawable.default_album_list);
        }
    }

    class ViewHolder
    {
        private TextView mName;
        private TextView mNum;
        private ImageView mImage;
    }
}
