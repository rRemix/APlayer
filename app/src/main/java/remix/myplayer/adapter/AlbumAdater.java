package remix.myplayer.adapter;

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
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.PopupListener;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;

/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdater extends RecyclerView.Adapter<AlbumAdater.AlbumHolder>  {
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
    class AsynLoadImage extends AsyncTask<Object,Integer,String> {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView) {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(Object... params) {
            if(CommonUtil.isAlbumThumbExistInDB((Uri) params[0])){
                return params[0].toString();
            } else {
                return CommonUtil.getCoverInCache((long)params[1]);
            }
//            return new SearchCover("海阔天空","Beyond",SearchCover.COVER).getImgUrl();
        }
        @Override
        protected void onPostExecute(String uri) {
            if(mImage != null && uri != null);{
                mImage.setImageURI(Uri.parse(uri));
            }
        }
    }

    @Override
    public AlbumHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AlbumHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.album_recycle_item, null, false));
    }

    @Override
    public void onBindViewHolder(final AlbumHolder holder, final int position) {
        if(mCursor.moveToPosition(position)) {
            try {
                //获得并设置专辑与艺术家
                String artist = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                String album = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mAlbumIndex),CommonUtil.ALBUMTYPE);

                holder.mText1.setText(album);
                holder.mText1.setTextColor(mContext.getResources().getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.black_1c1b19 : R.color.white));
                holder.mText2.setText(artist);
                holder.mText1.setTextColor(mContext.getResources().getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6d6c69 : R.color.gray_6c6a6c));
                //设置背景
                holder.mContainer.setBackgroundResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.album_bg_day : R.drawable.album_bg);
            } catch (Exception e){
                e.printStackTrace();
            }

            //设置封面
            long albumid = mCursor.getInt(AlbumFragment.mAlbumIdIndex);
//            new AsynLoadImage(holder.mImage).execute(0L);

//            new AsynLoadImage(holder.mImage).execute(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumid), albumid);
//            holder.mImage.setImageURI(Uri.parse(new SearchCover("海阔天空","Beyond",SearchCover.COVER).getImgUrl()));
            holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), mCursor.getInt(AlbumFragment.mAlbumIdIndex)));

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

    public static class AlbumHolder extends BaseViewHolder {
        @BindView(R.id.recycleview_text1)
        public TextView mText1;
        @BindView(R.id.recycleview_text2)
        public TextView mText2;
        @BindView(R.id.recycleview_button)
        public ImageButton mButton;
        @BindView(R.id.recycleview_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.album_item_container)
        public RelativeLayout mContainer;

        public AlbumHolder(View v) {
            super(v);
        }

    }

}
