package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.asynctask.AsynLoadSongNum;
import remix.myplayer.fragment.ArtistFragment;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.mp3.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScroller;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.CommonUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家界面的适配器
 */
public class ArtistAdapter extends HeaderAdapter implements FastScroller.SectionIndexer{
    public ArtistAdapter(Cursor cursor, Context context,MultiChoice multiChoice) {
        super(context,cursor,multiChoice);
        ListModel =  SPUtil.getValue(context,"Setting","ArtistModel",Constants.GRID_MODEL);
    }

    @Override
    public BaseViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            return new AlbumAdater.HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_2,parent,false));
        }
        return viewType == Constants.LIST_MODEL ?
                new ArtistAdapter.ArtistListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_list,parent,false)) :
                new ArtistAdapter.ArtistGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_grid,parent,false));
    }

    @Override
    public void onBind(final BaseViewHolder baseHolder, final int position) {
        if(position == 0){
            final AlbumAdater.HeaderHolder headerHolder = (AlbumAdater.HeaderHolder) baseHolder;
            if(mCursor == null || mCursor.getCount() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }
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

        if(!(baseHolder instanceof ArtistHolder)){
            return;
        }
        final ArtistHolder holder = (ArtistHolder) baseHolder;
        if(mCursor.moveToPosition(position - 1)) {
            try {
                //设置歌手名
                String artist = CommonUtil.processInfo(mCursor.getString(ArtistFragment.mArtistIndex),CommonUtil.ARTISTTYPE);
                holder.mText1.setText(artist);
                int artistId = mCursor.getInt(ArtistFragment.mArtistIdIndex);
                if(holder instanceof ArtistListHolder){
                    new AsynLoadSongNum(holder.mText2,Constants.ARTIST).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,artistId);
                }
                //设置封面
                new AsynLoadImage(holder.mImage).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,artistId,Constants.URL_ARTIST);

                //item点击效果
                holder.mContainer.setBackground(
                        Theme.getPressAndSelectedStateListRippleDrawable(ListModel,mContext));

            } catch (Exception e){
                e.printStackTrace();
            }

            if(mOnItemClickLitener != null) {
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(holder.getAdapterPosition() - 1 < 0){
                            ToastUtil.show(mContext,R.string.illegal_arg);
                            return;
                        }
                        mOnItemClickLitener.onItemClick(holder.mContainer,position - 1);
                    }
                });
                //多选菜单
                holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if(holder.getAdapterPosition() - 1 < 0){
                            ToastUtil.show(mContext,R.string.illegal_arg);
                            return true;
                        }
                        mOnItemClickLitener.onItemLongClick(holder.mContainer,position - 1);
                        return true;
                    }
                });
            }
            //popupmenu
            if(holder.mButton != null) {
                int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
                Theme.TintDrawable(holder.mButton,R.drawable.list_icn_more,tintColor);

                //按钮点击效果
                int size = DensityUtil.dip2px(mContext,45);
                Drawable defaultDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, Color.TRANSPARENT, size, size);
                Drawable selectDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, ThemeStore.getSelectColor(), size, size);
                holder.mButton.setBackground(Theme.getPressDrawable(
                        defaultDrawable,
                        selectDrawable,
                        ThemeStore.getRippleColor(),
                        null,
                        null));

                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow() || !mCursor.moveToPosition(holder.getAdapterPosition() - 1))
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        popupMenu.getMenuInflater().inflate(R.menu.artist_menu, popupMenu.getMenu());
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
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position - 1))){
            mMultiChoice.AddView(holder.mContainer);
        } else {
            holder.mContainer.setSelected(false);
        }

        //设置padding
        if(ListModel == 2 && holder.mRoot != null){
            if(position % 2 == 1){
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4));
            } else {
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4));
            }
        }
    }

    @Override
    public void saveMode() {
        SPUtil.putValue(mContext,"Setting","ArtistModel",ListModel);
    }

//    @NonNull
//    @Override
//    public String getSectionName(int position) {
//        if(position == 0)
//            return "";
//        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position - 1)){
//            String artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
//            return !TextUtils.isEmpty(artist) ? (Pinyin.toPinyin(artist.charAt(0))).toUpperCase().substring(0,1)  : "";
//        }
//        return "";
//    }

    @Override
    public String getSectionText(int position) {
        if(position == 0)
            return "";
        if(mCursor != null && !mCursor.isClosed() && mCursor.moveToPosition(position - 1)){
            String artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
            return !TextUtils.isEmpty(artist) ? (Pinyin.toPinyin(artist.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
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

    static class ArtistListHolder extends ArtistHolder{
        ArtistListHolder(View v) {
            super(v);
        }
    }

    static class ArtistGridHolder extends ArtistHolder{
        ArtistGridHolder(View v) {
            super(v);
        }
    }

}
