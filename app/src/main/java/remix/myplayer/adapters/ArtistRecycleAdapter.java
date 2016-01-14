package remix.myplayer.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

import java.lang.ref.WeakReference;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.AlbumRecyleFragment;
import remix.myplayer.fragments.ArtistRecycleFragment;
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
    class AsynLoadImage extends AsyncTask<Integer,Integer,Bitmap>
    {
//        private final WeakReference mImageView;
        private final ImageView mImage;
        public AsynLoadImage(ImageView imageView)
        {
//            mImageView = new WeakReference(imageView);
            mImage = imageView;
        }
        @Override
        protected Bitmap doInBackground(Integer... params) {
            return Utility.getBitmapByArtistId(params[0]);
        }
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if(bitmap != null && mImage != null)
                mImage.setImageBitmap(bitmap);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.artist_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
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
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                switch (item.getItemId()) {
                                    case R.id.exit:
                                        popupMenu.dismiss();
                                        break;
                                    default:
                                        Toast.makeText(mContext, "单击了 " + item.getTitle(), Toast.LENGTH_SHORT).show();
                                }
                                return true;
                            }
                        });
                        popupMenu.setGravity(Gravity.END );
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
        public final ImageView mImage;
        public final ImageButton mButton;
        public ViewHolder(View v) {
            super(v);
            mText1 = (TextView)v.findViewById(R.id.recycleview_text1);
            mImage = (ImageView)v.findViewById(R.id.recycleview_image);
            mButton = (ImageButton)v.findViewById(R.id.recycleview_button);
        }
    }
}
