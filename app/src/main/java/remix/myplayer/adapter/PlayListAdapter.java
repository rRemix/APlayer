package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.asynctask.AsynLoadImage;
import remix.myplayer.fragment.PlayListFragment;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.model.PlayListInfo;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.PlayListUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends HeaderAdapter {
    private MultiChoice mMultiChoice;

    public PlayListAdapter(Context context,MultiChoice multiChoice) {
        super(context,null,multiChoice,R.layout.layout_topbar_2);
        ListModel =  SPUtil.getValue(context,"Setting","PlayListModel",Constants.GRID_MODEL);
        this.mMultiChoice = multiChoice;
    }

    @Override
    public BaseViewHolder onCreateHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            return new AlbumAdater.HeaderHolder(mHeaderView);
        }
        return viewType == Constants.LIST_MODEL ?
                new PlayListListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_recycle_list,parent,false)) :
                new PlayListGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_recycle_grid,parent,false));
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

        if(!(baseHolder instanceof PlayListHolder)){
            return;
        }
        final PlayListHolder holder = (PlayListHolder) baseHolder;
        if(mCursor.moveToPosition(position - 1)){
            final PlayListInfo info = PlayListUtil.getPlayListInfo(mCursor);
            if(info == null)
                return;
            holder.mName.setText(info.Name);
            holder.mOther.setText(info.Count + "首歌曲");
            //设置专辑封面
            new AsynLoadImage(holder.mImage).execute(info._Id,Constants.URL_PLAYLIST);

            if(mOnItemClickLitener != null) {
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(holder.getAdapterPosition() - 1 < 0){
                            ToastUtil.show(mContext,"参数错误");
                            return;
                        }
                        mOnItemClickLitener.onItemClick(holder.mContainer,holder.getAdapterPosition());
                    }
                });
                //多选菜单
                holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if(holder.getAdapterPosition() - 1 < 0){
                            ToastUtil.show(mContext,"参数错误");
                            return true;
                        }
                        mOnItemClickLitener.onItemLongClick(holder.mContainer,holder.getAdapterPosition());
                        return true;
                    }
                });
            }

            if(holder.mButton != null) {
                Theme.TintDrawable(holder.mButton,
                          R.drawable.list_icn_more,
                        ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6c6a6c : R.color.white));

                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        popupMenu.getMenuInflater().inflate(R.menu.playlist_menu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext, info._Id, Constants.PLAYLIST, info.Name));
                        popupMenu.show();
                    }
                });
                //点击效果
                int size = DensityUtil.dip2px(mContext,45);
                Drawable defaultDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, Color.TRANSPARENT, size, size);
                Drawable selectDrawable = Theme.getShape(ListModel == Constants.LIST_MODEL ? GradientDrawable.OVAL : GradientDrawable.RECTANGLE, ThemeStore.getSelectColor(), size, size);
                holder.mButton.setBackground(Theme.getPressDrawable(
                        defaultDrawable,
                        selectDrawable,
                        ThemeStore.getRippleColor(),
                        null,
                        null));

            }

            //背景点击效果
            holder.mContainer.setBackground(
                    Theme.getPressAndSelectedStateListRippleDrawable(ListModel, mContext));

            //是否处于选中状态
            if(MultiChoice.TAG.equals(PlayListFragment.TAG) &&
                    mMultiChoice.mSelectedPosition.contains(new MultiPosition(position - 1))){
                mMultiChoice.AddView(holder.mContainer);
            } else {
                holder.mContainer.setSelected(false);
            }

            //设置padding
            if(ListModel == 2 && holder.mRoot != null){
                if(position % 2 == 0){
                    holder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4));
                } else {
                    holder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4));
                }
            }
        }

    }

    static class PlayListHolder extends BaseViewHolder {
        @BindView(R.id.item_text1)
        TextView mName;
        @BindView(R.id.item_text2)
        TextView mOther;
        @BindView(R.id.item_simpleiview)
        SimpleDraweeView mImage;
        @BindView(R.id.item_button)
        ImageView mButton;
        @BindView(R.id.item_container)
        RelativeLayout mContainer;

        @BindView(R.id.item_root)
        @Nullable
        View mRoot;
        PlayListHolder(View itemView) {
            super(itemView);
        }
    }

    static class PlayListListHolder extends PlayListHolder{
        PlayListListHolder(View itemView) {
            super(itemView);
        }
    }

    static class PlayListGridHolder extends PlayListHolder{
       PlayListGridHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * 先查找是否设置过封面，没有再查找播放列表下所有歌曲，直到有一首歌曲存在封面
     */
//    class AsynLoadImage extends AsyncTask<Integer,Integer,String> {
//        private final SimpleDraweeView mImage;
//        public AsynLoadImage(SimpleDraweeView imageView) {
//            mImage = imageView;
//        }
//        @Override
//        protected String doInBackground(Integer... params) {
//            int playListId = params[0];
//            ArrayList<Integer> list = PlayListUtil.getIDList(playListId);
//            String url = null;
//            File imgFile =  new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + CommonUtil.hashKeyForDisk(Integer.valueOf(playListId) * 255 + ""));
//            if(imgFile != null && imgFile.exists())
//                return imgFile.getAbsolutePath();
//
//            if(list != null && list.size() > 0) {
//                for(Integer id : list){
//                    MP3Item item = MediaStoreUtil.getMP3InfoById(id);
//                    if(item == null)
//                        return "";
//                    url = MediaStoreUtil.getImageUrl(item.getAlbumId() + "",Constants.URL_ALBUM);
//                    if(url != null && !url.equals("")) {
//                        File file = new File(url);
//                        if(file.exists()) {
//                            break;
//                        }
//                    }
//                }
//            }
//            return url;
//        }
//        @Override
//        protected void onPostExecute(String url) {
//            Uri uri = Uri.parse("file:///" + url);
//            if(mImage != null)
//                mImage.setImageURI(uri);
//        }
//    }

}
