package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.asynctask.AsynLoadSongNum;
import remix.myplayer.fragment.AlbumFragment;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;


/**
 * Created by Remix on 2015/12/20.
 */

/**
 * 专辑界面的适配器
 */
public class AlbumAdater extends HeaderAdapter  {
    public AlbumAdater(Cursor cursor, Context context,MultiChoice multiChoice) {
        super(context,cursor,multiChoice);
        ListModel =  SPUtil.getValue(context,"Setting","AlbumModel",Constants.GRID_MODEL);
    }

    @Override
    public BaseViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            return new HeaderHolder(mHeaderView);
        }
        return viewType == Constants.LIST_MODEL ?
                new AlbumListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_list,parent,false)) :
                new AlbumGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_album_recycle_grid,parent,false));
    }

    @Override
    public void onBind(final BaseViewHolder baseHolder, final int position) {
        if(position == 0){
            final HeaderHolder headerHolder = (HeaderHolder) baseHolder;
            //设置图标
            headerHolder.mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
            headerHolder.mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMode(headerHolder,v);
                }
            });
            headerHolder.mListModelBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchMode(headerHolder,v);
                }
            });
            return;
        }

        if(!(baseHolder instanceof AlbumHolder)){
            return;
        }
        final AlbumHolder albumHolder = (AlbumHolder) baseHolder;
        if(mCursor.moveToPosition(position - 1)) {
            try {

                //获得并设置专辑与艺术家
                String artist = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                String album = CommonUtil.processInfo(mCursor.getString(AlbumFragment.mAlbumIndex),CommonUtil.ALBUMTYPE);

                albumHolder.mText1.setText(album);
                albumHolder.mText2.setText(artist);
                //设置封面
                int albumid = mCursor.getInt(AlbumFragment.mAlbumIdIndex);
                albumHolder.mImage.setImageURI(Uri.EMPTY);
                new AsynLoadImage(albumHolder.mImage).execute(albumid,Constants.URL_ALBUM);
                if(albumHolder instanceof AlbumListHolder){
                    new AsynLoadSongNum(albumHolder.mText2,Constants.ALBUM).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,albumid);
                }
            } catch (Exception e){
                e.printStackTrace();
            }

           //背景点击效果
            albumHolder.mContainer.setBackground(
                    Theme.getPressAndSelectedStateListRippleDrawable(ListModel, mContext));

            if(mOnItemClickLitener != null) {
                albumHolder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(albumHolder.mContainer,albumHolder.getAdapterPosition());
                    }
                });
                //多选菜单
                albumHolder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(albumHolder.mContainer,albumHolder.getAdapterPosition());
                        return true;
                    }
                });
            }

            if(albumHolder.mButton != null) {
                //着色
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(albumHolder.mButton,R.drawable.list_icn_more,tintColor);

                //点击效果
                int size = DensityUtil.dip2px(mContext,45);
                Drawable defaultDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, Color.TRANSPARENT, size, size);
                Drawable selectDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, ThemeStore.getSelectColor(), size, size);
                albumHolder.mButton.setBackground(Theme.getPressDrawable(
                        defaultDrawable,
                        selectDrawable,
                        ThemeStore.getRippleColor(),
                        null,
                        null));

                albumHolder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,albumHolder.mButton,Gravity.END);
                        popupMenu.getMenuInflater().inflate(R.menu.album_menu, popupMenu.getMenu());
                        mCursor.moveToPosition(albumHolder.getAdapterPosition());
                        popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                                mCursor.getInt(AlbumFragment.mAlbumIdIndex),
                                Constants.ALBUM,
                                mCursor.getString(AlbumFragment.mAlbumIndex)));
                        popupMenu.show();
                    }
                });
            }

            //是否处于选中状态
            if(MultiChoice.TAG.equals(AlbumFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
                mMultiChoice.AddView(albumHolder.mContainer);
            } else {
                albumHolder.mContainer.setSelected(false);
            }

            //半圆着色
            if(ListModel == Constants.GRID_MODEL){
                Theme.TintDrawable(albumHolder.mHalfCircle,R.drawable.icon_half_circular_left,
                        ColorUtil.getColor(ThemeStore.isDay() ? R.color.white : R.color.night_background_color_main));
            }

            //设置padding
            if(ListModel == 2 && albumHolder.mRoot != null){
                if(position % 2 == 0){
                    albumHolder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4));
                } else {
                    albumHolder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4));
                }
            }
        }
    }

    @Override
    public void saveMode() {
        SPUtil.putValue(mContext,"Setting","AlbumModel",ListModel);
    }

    static class AlbumHolder extends BaseViewHolder {
        @BindView(R.id.item_half_circle)
        @Nullable
        ImageView mHalfCircle;
        @BindView(R.id.item_text1)
        TextView mText1;
        @BindView(R.id.item_text2)
        TextView mText2;
        @BindView(R.id.item_button)
        ImageButton mButton;
        @BindView(R.id.item_simpleiview)
        SimpleDraweeView mImage;
        @BindView(R.id.item_container)
        RelativeLayout mContainer;
        @BindView(R.id.item_root)
        @Nullable
        View mRoot;
        AlbumHolder(View v) {
            super(v);
        }
    }

    static class AlbumGridHolder extends AlbumHolder {
        AlbumGridHolder(View v) {
            super(v);
        }
    }

    static class AlbumListHolder extends AlbumHolder {
        AlbumListHolder(View v) {
            super(v);
        }
    }

    static class HeaderHolder extends BaseViewHolder{
        //列表显示与网格显示切换
        @BindView(R.id.list_model)
        ImageButton mListModelBtn;
        @BindView(R.id.grid_model)
        ImageButton mGridModelBtn;
        @BindView(R.id.divider)
        View mDivider;
        HeaderHolder(View itemView) {
            super(itemView);
        }
    }
}
