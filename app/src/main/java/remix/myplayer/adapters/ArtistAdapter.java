package remix.myplayer.adapters;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import remix.myplayer.R;
import remix.myplayer.fragments.AlbumRecyleFragment;
import remix.myplayer.fragments.ArtistFragment;
import remix.myplayer.utils.ArtistInfo;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/4.
 */
public class ArtistAdapter extends SimpleCursorAdapter {

    //<Params, Progress, Result>
    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
        private final WeakReference mImageView;
        public AsynLoadImage(ImageView imageView)
        {
            mImageView = new WeakReference(imageView);
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return Utility.getBitmapByArtistId(params[0]);
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null)
                ((ImageView)mImageView.get()).setImageBitmap(bitmap);
            else
                ((ImageView)mImageView.get()).setImageResource(R.drawable.default_artist);

        }
    }


    public ArtistAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null)
        {
            convertView = super.getView(position, convertView, parent);
            holder = new ViewHolder();
            holder.mImage = (ImageView)convertView.findViewById(R.id.artist_album_item_image);
            holder.mName = (TextView)convertView.findViewById(R.id.artist_album_item_name);
            holder.mNum = (TextView)convertView.findViewById(R.id.artist_album_item_count);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        if((getItem(position)) == null)
            return convertView;
        String name = ((Cursor)getItem(position)).getString(ArtistFragment.mArtistIndex);

        holder.mName.setText(name);
        int num = Utility.getSongNumByArtistId(((Cursor)getItem(position)).getInt(ArtistFragment.mArtistIdIndex));
        if(num > 0)
            holder.mNum.setText(num + "首");
        AsynLoadImage task = new AsynLoadImage(holder.mImage);
        task.execute(((Cursor)getItem(position)).getInt(AlbumRecyleFragment.mAlbumIdIndex));
        return convertView;
    }

    class ViewHolder
    {
        private ImageView mImage;
        private TextView mName;
        private TextView mNum;

    }

}


