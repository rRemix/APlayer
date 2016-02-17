package remix.myplayer.adapters;

import android.content.Context;
import android.database.Cursor;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.AlbumRecyleFragment;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/12/20.
 */
public class AlbumRecycleAdater extends RecyclerView.Adapter<AlbumRecycleAdater.ViewHolder>  {
    private Cursor mCursor;
    private Context mContext;
    private static int mCount = 0;
    private OnItemClickListener mOnItemClickLitener;

    public AlbumRecycleAdater(Cursor cursor, Context context) {
        this.mCursor = cursor;
        this.mContext = context;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }
    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }


    //<Params, Progress, Result>
    class AsynLoadImage extends AsyncTask<Integer,Integer,Object>
    {
//        private final WeakReference mImageView;
//        private final ImageView mImage;
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
//            mImageView = new WeakReference(imageView);
            mImage = imageView;
        }
        @Override
        protected Object doInBackground(Integer... params) {
//            return CommonUtil.CheckBitmapByAlbumId(params[0],true);
            return DBUtil.CheckUrlByAlbumId(params[0]);
        }
        @Override
        protected void onPostExecute(Object url) {
            Uri uri = Uri.parse("file:///" + (String)url);
            if(url != null && mImage != null);{
                mImage.setImageURI(uri);
            }

        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.album_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if(mCursor.moveToPosition(position))
        {
            holder.mText1.setText(mCursor.getString(AlbumRecyleFragment.mAlbumIndex));
            holder.mText2.setText(mCursor.getString(AlbumRecyleFragment.mArtistIndex));

            AsynLoadImage task = new AsynLoadImage(holder.mImage);
//            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,mCursor.getInt(AlbumRecyleFragment.mAlbumIdIndex));
            task.execute(mCursor.getInt(AlbumRecyleFragment.mAlbumIdIndex));
//            holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mCursor.getInt(AlbumRecyleFragment.mAlbumIdIndex)));
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
                        Context wrapper = new ContextThemeWrapper(mContext,R.style.MyPopupMenu);
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        MainActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());

                        mCursor.moveToPosition(position);
                        popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                                mCursor.getInt(AlbumRecyleFragment.mAlbumIdIndex),
                                Constants.ALBUM_HOLDER,
                                mCursor.getString(AlbumRecyleFragment.mAlbumIdIndex)));
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
        public final TextView mText2;
//        public final ImageView mImage;
        public final ImageButton mButton;
        public final SimpleDraweeView mImage;
        public ViewHolder(View v) {
            super(v);
            mText1 = (TextView)v.findViewById(R.id.recycleview_text1);
            mText2 = (TextView)v.findViewById(R.id.recycleview_text2);
//            mImage = (ImageView)v.findViewById(R.id.recycleview_simpleiview);
            mImage = (SimpleDraweeView)v.findViewById(R.id.recycleview_simpleiview);

            mButton = (ImageButton)v.findViewById(R.id.recycleview_button);
        }

    }

    class DraweeListener implements ControllerListener
    {

        @Override
        public void onSubmit(String id, Object callerContext) {

        }

        @Override
        public void onFinalImageSet(String id, Object imageInfo, Animatable animatable) {

        }

        @Override
        public void onIntermediateImageSet(String id, Object imageInfo) {

        }

        @Override
        public void onIntermediateImageFailed(String id, Throwable throwable) {

        }

        @Override
        public void onFailure(String id, Throwable throwable) {
        }

        @Override
        public void onRelease(String id) {

        }
    }
}
