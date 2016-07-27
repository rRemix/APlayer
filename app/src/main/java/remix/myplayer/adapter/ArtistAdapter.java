package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.PopupListener;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家界面的适配器
 */
public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistHolder>{
    private Cursor mCursor;
    private Context mContext;

    public void setOnItemClickLitener(OnItemClickListener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    private OnItemClickListener mOnItemClickLitener;

    public ArtistAdapter(Cursor cursor, Context context) {
        this.mCursor = cursor;
        this.mContext = context;
    }

    public void setCursor(Cursor mCursor) {
        this.mCursor = mCursor;
        notifyDataSetChanged();
    }


    //<Params, Progress, Result>
    class AsynLoadImage extends AsyncTask<String,Integer,String> {
        private final SimpleDraweeView mImage;
        private String mArtist = "";
        public AsynLoadImage(SimpleDraweeView imageView)
        {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(String... params) {
            mArtist = params[1];
            return DBUtil.getImageUrl(params[0], Constants.URL_ARTIST);

        }
        @Override
        protected void onPostExecute(String url) {
            Log.d("ArtistAdapter","url:" + url + " artist:" + mArtist);
            Uri uri = Uri.parse("file:///" + url);
            if(mImage != null) {
                mImage.setImageURI(uri);
            }
        }
    }

    @Override
    public ArtistHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ArtistHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.artist_recycle_item, null, false));
    }

    @Override
    public void onBindViewHolder(final ArtistHolder holder, final int position) {
        if(mCursor.moveToPosition(position)) {
            try {
                //设置歌手名
                String artist = CommonUtil.processInfo(mCursor.getString(ArtistFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                holder.mText1.setText(artist);
                //设置封面
                holder.mImage.setImageURI(Uri.EMPTY);
                AsynLoadImage task = new AsynLoadImage(holder.mImage);
                task.execute(mCursor.getString(ArtistFragment.mArtistIdIndex),artist);
            } catch (Exception e){
                e.printStackTrace();
            }

//            Uri uri = Uri.parse("content://media/external/audio/media/" + mCursor.getString(ArtistFragment.mArtistIndex) + "/albumart");
//            holder.mImage.setImageURI(uri);
//            String path = DBUtil.getImageUrl(mCursor.getString(ArtistFragment.mArtistIdIndex), Constants.URL_ARTIST);
//            Uri uri = Uri.parse("file:///" + path);
//            holder.mImage.setImageURI(uri);
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
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        MainActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(position);
                        popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                                mCursor.getInt(ArtistFragment.mArtistIdIndex),
                                Constants.ARTIST_HOLDER,
                                mCursor.getString(ArtistFragment.mArtistIdIndex)));
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

    public static class ArtistHolder extends BaseViewHolder {
        @BindView(R.id.recycleview_text1)
        public TextView mText1;
        @BindView(R.id.recycleview_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.recycleview_button)
        public ImageButton mButton;
        public ArtistHolder(View v) {
            super(v);
        }
    }

}
