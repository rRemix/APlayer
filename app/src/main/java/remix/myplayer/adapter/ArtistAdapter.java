package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
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
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.asynctask.AsynLoadSongNum;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家界面的适配器
 */
public class ArtistAdapter extends BaseAdapter<ArtistAdapter.ArtistHolder>{
    private MultiChoice mMultiChoice;

    public ArtistAdapter(Cursor cursor, Context context,MultiChoice multiChoice) {
        super(context,cursor);
        this.mMultiChoice = multiChoice;
    }

    @Override
    public int getItemViewType(int position) {
        return ArtistFragment.getModel();
    }
    @Override
    public ArtistHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == Constants.LIST_MODEL ?
                new ArtistListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_list,parent,false)) :
                new ArtistGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_grid,parent,false));
    }

    @Override
    public void onBindViewHolder(final ArtistHolder holder, final int position) {
        if(mCursor.moveToPosition(position)) {
            try {
                //设置歌手名
                String artist = CommonUtil.processInfo(mCursor.getString(ArtistFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                holder.mText1.setText(artist);
                int artistId = mCursor.getInt(ArtistFragment.mArtistIdIndex);
                if(holder instanceof ArtistListHolder){
                    new AsynLoadSongNum(holder.mText2,Constants.ARTIST).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,artistId);
                }
                //设置封面
                holder.mImage.setImageURI(Uri.EMPTY);
                new AsynLoadImage(holder.mImage).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,artistId,Constants.URL_ARTIST,true);
            } catch (Exception e){
                e.printStackTrace();
            }

            if(mOnItemClickLitener != null) {
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mContainer,position);
                    }
                });
                //多选菜单
                holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(holder.mContainer,position);
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
                        if(mMultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        popupMenu.getMenuInflater().inflate(R.menu.artist_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(holder.getAdapterPosition());
                        popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                                mCursor.getInt(ArtistFragment.mArtistIdIndex),
                                Constants.ARTIST,
                                mCursor.getString(ArtistFragment.mArtistIndex)));
                        popupMenu.setGravity(Gravity.END);
                        popupMenu.show();
                    }
                });
            }
        }

        //是否处于选中状态
        if(MultiChoice.TAG.equals(ArtistFragment.TAG) &&
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            mMultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }

        //设置padding
        if(ArtistFragment.ListModel == 2 && holder.mRoot != null){
            if(position % 2 == 0){
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),0,DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,8));
            } else {
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),0,DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,8));
            }
        }
    }

    public static class ArtistHolder extends BaseViewHolder {
        @BindView(R.id.item_text1)
        public TextView mText1;
        @BindView(R.id.item_text2)
        @Nullable
        public TextView mText2;
        @BindView(R.id.item_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.item_button)
        public ImageButton mButton;
        @BindView(R.id.item_container)
        public RelativeLayout mContainer;
        @BindView(R.id.item_root)
        @Nullable
        public View mRoot;
        public ArtistHolder(View v) {
            super(v);
        }
    }

    public static class ArtistListHolder extends ArtistHolder{
        public ArtistListHolder(View v) {
            super(v);
        }
    }

    public static class ArtistGridHolder extends ArtistHolder{
        public ArtistGridHolder(View v) {
            super(v);
        }
    }

}
