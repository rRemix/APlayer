package remix.myplayer.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;
import com.github.promeg.pinyinhelper.Pinyin;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.model.mp3.PlayList;
import remix.myplayer.model.netease.NSearchRequest;
import remix.myplayer.request.ImageUriRequest;
import remix.myplayer.request.PlayListUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScroller;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends HeaderAdapter<PlayList, BaseViewHolder> implements FastScroller.SectionIndexer{
    private MultiChoice mMultiChoice;

    public PlayListAdapter(Context context,int layoutId,MultiChoice multiChoice) {
        super(context,layoutId,multiChoice);
        ListModel =  SPUtil.getValue(context,"Setting","PlayListModel",Constants.GRID_MODEL);
        this.mMultiChoice = multiChoice;
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            return new AlbumAdapter.HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_2,parent,false));
        }
        return viewType == Constants.LIST_MODEL ?
                new PlayListListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_recycle_list,parent,false)) :
                new PlayListGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_playlist_recycle_grid,parent,false));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        if(holder instanceof PlayListHolder){
            ((PlayListHolder) holder).mImage.setImageURI(Uri.EMPTY);
        }
    }


    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(BaseViewHolder baseHolder, final PlayList info, int position) {
        if(position == 0){
            final AlbumAdapter.HeaderHolder headerHolder = (AlbumAdapter.HeaderHolder) baseHolder;
            if(mDatas == null || mDatas.size() == 0){
                headerHolder.mRoot.setVisibility(View.GONE);
                return;
            }
            //设置图标
            headerHolder.mDivider.setVisibility(ListModel == Constants.LIST_MODEL ? View.VISIBLE : View.GONE);
            headerHolder.mListModelBtn.setColorFilter(ListModel == Constants.LIST_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setColorFilter(ListModel == Constants.GRID_MODEL ? ColorUtil.getColor(R.color.select_model_button_color) : ColorUtil.getColor(R.color.default_model_button_color));
            headerHolder.mGridModelBtn.setOnClickListener(v -> switchMode(headerHolder,v));
            headerHolder.mListModelBtn.setOnClickListener(v -> switchMode(headerHolder,v));
            return;
        }

        if(!(baseHolder instanceof PlayListHolder)){
            return;
        }
        final PlayListHolder holder = (PlayListHolder) baseHolder;
        if(info == null)
            return;
        holder.mName.setText(info.Name);
        holder.mOther.setText(mContext.getString(R.string.song_count,info.Count));
        //设置专辑封面
        final int imageSize = ListModel == 1 ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;
        new PlayListUriRequest(holder.mImage,
                new NSearchRequest(info.getId(), info.getName(), 1, ImageUriRequest.URL_PLAYLIST),
                new RequestConfig.Builder(imageSize,imageSize).build()).load();

        holder.mContainer.setOnClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return;
            }
            mOnItemClickLitener.onItemClick(holder.mContainer,holder.getAdapterPosition() - 1);
        });
        //多选菜单
        holder.mContainer.setOnLongClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return true;
            }
            mOnItemClickLitener.onItemLongClick(holder.mContainer,holder.getAdapterPosition() - 1);
            return true;
        });

        Theme.TintDrawable(holder.mButton,
                R.drawable.icon_player_more,
                ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6c6a6c : R.color.white));

        holder.mButton.setOnClickListener(v -> {
            if(mMultiChoice.isShow())
                return;
            Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.playlist_menu, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext, info._Id, Constants.PLAYLIST, info.Name));
            popupMenu.show();
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
            if(position % 2 == 1){
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4));
            } else {
                holder.mRoot.setPadding(DensityUtil.dip2px(mContext,3),DensityUtil.dip2px(mContext,4),DensityUtil.dip2px(mContext,6),DensityUtil.dip2px(mContext,4));
            }
        }
    }


    @Override
    public String getSectionText(int position) {
        if(position == 0)
            return "";
        if(mDatas != null && position - 1 < mDatas.size()){
            String title = mDatas.get(position - 1).Name;
            return !TextUtils.isEmpty(title) ? (Pinyin.toPinyin(title.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
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

    @Override
    protected void saveMode() {
        SPUtil.putValue(mContext,"Setting","PlayListModel",ListModel);
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
//            File imgFile =  new File(DiskCache.getDiskCacheDir(mContext,"thumbnail/playlist") + "/" + Util.hashKeyForDisk(Integer.valueOf(playListId) * 255 + ""));
//            if(imgFile != null && imgFile.exists())
//                return imgFile.getAbsolutePath();
//
//            if(list != null && list.size() > 0) {
//                for(Integer id : list){
//                    Song item = MediaStoreUtil.getMP3InfoById(id);
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
