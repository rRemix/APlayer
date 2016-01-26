package remix.myplayer.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.AlbumRecyleFragment;
import remix.myplayer.fragments.ArtistRecycleFragment;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/22.
 */
public class ArtistRecycleAdapter extends RecyclerView.Adapter<ArtistRecycleAdapter.ViewHolder>{
    private Cursor mCursor;
    private Context mContext;
    private Bitmap mDefaultBmp;
    public interface OnItemClickLitener
    {
        void onItemClick(View view, int position);
        void onItemLongClick(View view , int position);
    }
    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener)
    {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    private OnItemClickLitener mOnItemClickLitener;

    public ArtistRecycleAdapter(Cursor cursor, Context context) {
        this.mCursor = cursor;
        this.mContext = context;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }


    //<Params, Progress, Result>
    class AsynLoadImage extends AsyncTask<Integer,Integer,String>
    {
//        private final WeakReference mImageView;
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
//            mImageView = new WeakReference(imageView);
            mImage = imageView;
        }
        @Override
        protected String doInBackground(Integer... params) {
            return Utility.CheckUrlByArtistId(params[0]);
        }
        @Override
        protected void onPostExecute(String url) {
            Uri uri = Uri.parse("file:///" + url);
            if(url != null && mImage != null)
                mImage.setImageURI(uri);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.artist_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if(mCursor.moveToPosition(position))
        {
            holder.mText1.setText(mCursor.getString(ArtistRecycleFragment.mArtistIndex));
            AsynLoadImage task = new AsynLoadImage(holder.mImage);
//            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mCursor.getInt(AlbumRecyleFragment.mAlbumIdIndex));
            task.execute(mCursor.getInt(ArtistRecycleFragment.mArtistIdIndex));
            if(mOnItemClickLitener != null)
            {
                holder.mImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = holder.getAdapterPosition();
                        mOnItemClickLitener.onItemClick(holder.mImage,pos);
                    }
                });
            }
            if(holder.mButton != null)
            {
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final PopupMenu popupMenu = new PopupMenu(mContext,holder.mButton);
                        MainActivity.mInstance.getMenuInflater().inflate(R.menu.pop_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(position);
                        popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                                mCursor.getInt(ArtistRecycleFragment.mArtistIdIndex),
                                Utility.ARTIST_HOLDER,
                                mCursor.getString(ArtistRecycleFragment.mArtistIdIndex)));
                        popupMenu.setGravity(Gravity.END);
                        popupMenu.show();
                    }
                });
            }
        }
    }
    @Override
    public int getItemCount() {
        if(mCursor != null)
            return mCursor.getCount();
        return 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mText1;
//        public final ImageView mImage;
        public final SimpleDraweeView mImage;
        public final ImageButton mButton;
        public ViewHolder(View v) {
            super(v);
            mText1 = (TextView)v.findViewById(R.id.recycleview_text1);
            mImage = (SimpleDraweeView)v.findViewById(R.id.recycleview_simpleiview);
            mButton = (ImageButton)v.findViewById(R.id.recycleview_button);
        }
    }
}
