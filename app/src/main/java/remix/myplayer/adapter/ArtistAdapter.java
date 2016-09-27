package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.fragment.SongFragment;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.AlbumArtistFolderListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.thumb.AsynLoadImage;

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
                //设置背景
                holder.mContainer.setBackgroundResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.art_bg_day : R.drawable.art_bg_night);
                //设置封面
                holder.mImage.setImageURI(Uri.EMPTY);
                new AsynLoadImage(holder.mImage).execute(mCursor.getInt(ArtistFragment.mArtistIdIndex),Constants.URL_ARTIST,true);
            } catch (Exception e){
                e.printStackTrace();
            }

            if(mOnItemClickLitener != null) {
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mCardBackground,position);
                    }
                });
                //多选菜单
                holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(holder.mCardBackground,position);
                        return true;
                    }
                });
            }
            //popupmenu
            if(holder.mButton != null) {
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(MainActivity.MultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        popupMenu.getMenuInflater().inflate(R.menu.artist_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(holder.getAdapterPosition());
                        popupMenu.setOnMenuItemClickListener(new AlbumArtistFolderListener(mContext,
                                mCursor.getInt(ArtistFragment.mArtistIdIndex),
                                Constants.ARTIST_HOLDER,
                                mCursor.getString(ArtistFragment.mArtistIndex)));
                        popupMenu.setGravity(Gravity.END);
                        popupMenu.show();
                    }
                });
            }
        }
        if(MultiChoice.TAG.equals(ArtistFragment.TAG) &&
                MainActivity.MultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            MainActivity.MultiChoice.AddView(holder.mCardBackground);
        } else {
            holder.mCardBackground.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public static class ArtistHolder extends BaseViewHolder {
        @BindView(R.id.recycleview_text1)
        public TextView mText1;
        @BindView(R.id.recycleview_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.recycleview_button)
        public ImageButton mButton;
        @BindView(R.id.item_container)
        public RelativeLayout mContainer;
        @BindView(R.id.recycleview_card)
        public Button mCardBackground;

        public ArtistHolder(View v) {
            super(v);
        }
    }

}
