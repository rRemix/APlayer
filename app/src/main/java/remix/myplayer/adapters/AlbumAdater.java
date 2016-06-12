package remix.myplayer.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
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

import com.facebook.drawee.view.SimpleDraweeView;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.fragments.AlbumFragment;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.CommonUtil;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdater extends RecyclerView.Adapter<AlbumAdater.ViewHolder>  {
    private Cursor mCursor;
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;

    public AlbumAdater(Cursor cursor, Context context) {
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
    class AsynLoadImage extends AsyncTask<Integer,Integer,Object> {
//        private final WeakReference mImageView;
//        private final ImageView mImage;
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView) {
//            mImageView = new WeakReference(imageView);
            mImage = imageView;
        }
        @Override
        protected Object doInBackground(Integer... params) {
//            return CommonUtil.CheckBitmapByAlbumId(params[0],true);
            return DBUtil.getImageUrl(String.valueOf(params[0]),Constants.URL_ALBUM);
//            return DBUtil.CheckUrlByAlbumId(params[0]);
        }
        @Override
        protected void onPostExecute(Object url) {
            Uri uri = Uri.parse("file:///" + (String)url);
            if(mImage != null);{
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
        if(mCursor.moveToPosition(position)) {
            try {
                //获得并设置专辑与艺术家
                String artist = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                String album = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mAlbumIndex),CommonUtil.ALBUMTYPE);
//            artist = artist.indexOf("unknown") > 0 ? mContext.getString(R.string.unknow_artist) : artist;
//            album = album.indexOf("unknown") > 0 ? mContext.getString(R.string.unknow_album) : album;
                holder.mText1.setText(album);
                holder.mText2.setText(artist);
            } catch (Exception e){
                e.printStackTrace();
            }


            //设置封面
            holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mCursor.getInt(AlbumFragment.mAlbumIdIndex)));
            //
            if(mOnItemClickLitener != null) {
                holder.mImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        int pos = holder.getAdapterPosition();
                        mOnItemClickLitener.onItemClick(holder.mImage,pos);
                    }
                });
            }
            //popupmenu
            if(holder.mButton != null) {
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context wrapper = new ContextThemeWrapper(mContext,R.style.MyPopupMenu);
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton,Gravity.END);
                        popupMenu.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(position);
                        popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                                mCursor.getInt(AlbumFragment.mAlbumIdIndex),
                                Constants.ALBUM_HOLDER,
                                mCursor.getString(AlbumFragment.mAlbumIdIndex)));
                        popupMenu.show();
                    }
                });
            }
        }
    }
    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mText1;
        public final TextView mText2;
        public final ImageButton mButton;
        public final SimpleDraweeView mImage;
        public ViewHolder(View v) {
            super(v);
            mText1 = (TextView)v.findViewById(R.id.recycleview_text1);
            mText2 = (TextView)v.findViewById(R.id.recycleview_text2);
            mImage = (SimpleDraweeView)v.findViewById(R.id.recycleview_simpleiview);

            mButton = (ImageButton)v.findViewById(R.id.recycleview_button);
        }

    }

}
