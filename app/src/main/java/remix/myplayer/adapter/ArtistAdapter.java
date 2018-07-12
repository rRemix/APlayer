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
import io.reactivex.disposables.Disposable;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.bean.mp3.Artist;
import remix.myplayer.menu.AlbArtFolderPlaylistListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.customview.fastcroll_recyclerview.FastScroller;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.SPUtil;
import remix.myplayer.util.ToastUtil;

import static remix.myplayer.request.ImageUriRequest.BIG_IMAGE_SIZE;
import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;

/**
 * Created by Remix on 2015/12/22.
 */

/**
 * 艺术家界面的适配器
 */
public class ArtistAdapter extends HeaderAdapter<Artist, BaseViewHolder> implements FastScroller.SectionIndexer{
    public ArtistAdapter(Context context,int layoutId,MultiChoice multiChoice) {
        super(context,layoutId,multiChoice);
        ListModel =  SPUtil.getValue(context,SPUtil.SETTING_KEY.NAME,"ArtistModel",Constants.GRID_MODEL);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if(viewType == TYPE_HEADER){
            return new AlbumAdapter.HeaderHolder(LayoutInflater.from(mContext).inflate(R.layout.layout_topbar_2,parent,false));
        }
        return viewType == Constants.LIST_MODEL ?
                new ArtistAdapter.ArtistListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_list,parent,false)) :
                new ArtistAdapter.ArtistGridHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_recycle_grid,parent,false));
    }

    @Override
    public void onViewRecycled(BaseViewHolder holder) {
        super.onViewRecycled(holder);
        if(holder instanceof ArtistHolder){
            if(((ArtistHolder) holder).mImage.getTag() != null){
                Disposable disposable = (Disposable) ((ArtistHolder) holder).mImage.getTag();
                if(!disposable.isDisposed())
                    disposable.dispose();
            }
            ((ArtistHolder) holder).mImage.setImageURI(Uri.EMPTY);
        }
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(BaseViewHolder baseHolder, Artist artist, final int position) {
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

        if(!(baseHolder instanceof ArtistHolder)){
            return;
        }
        final ArtistHolder holder = (ArtistHolder) baseHolder;
        //设置歌手名
        holder.mText1.setText(artist.getArtist());
        final int artistId = artist.getArtistID();
        if(holder instanceof ArtistListHolder && holder.mText2 != null){
            holder.mText2.setText(mContext.getString(R.string.song_count_1,artist.getCount()));
        }
        //设置封面
        final int imageSize = ListModel == 1 ? SMALL_IMAGE_SIZE : BIG_IMAGE_SIZE;
        Disposable disposable = new LibraryUriRequest(holder.mImage,ImageUriUtil.getSearchRequest(artist),new RequestConfig.Builder(imageSize,imageSize).build()).loadImage();
        holder.mImage.setTag(disposable);

        //item点击效果
        holder.mContainer.setBackground(
                Theme.getPressAndSelectedStateListRippleDrawable(ListModel,mContext));

        holder.mContainer.setOnClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return;
            }
            mOnItemClickLitener.onItemClick(holder.mContainer,position - 1);
        });
        //多选菜单
        holder.mContainer.setOnLongClickListener(v -> {
            if(holder.getAdapterPosition() - 1 < 0){
                ToastUtil.show(mContext,R.string.illegal_arg);
                return true;
            }
            mOnItemClickLitener.onItemLongClick(holder.mContainer,position - 1);
            return true;
        });

        //popupmenu
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

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

        holder.mButton.setOnClickListener(v -> {
            if(mMultiChoice.isShow())
                return;
            Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
            popupMenu.getMenuInflater().inflate(R.menu.menu_artist_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext,
                    artistId,
                    Constants.ARTIST,
                    artist.getArtist()));
            popupMenu.setGravity(Gravity.END);
            popupMenu.show();
        });

        //是否处于选中状态
        if(MultiChoice.TAG.equals(ArtistFragment.TAG) &&
                mMultiChoice.getSelectPos().contains(position - 1)){
            holder.mContainer.setSelected(true);
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
        SPUtil.putValue(mContext,SPUtil.SETTING_KEY.NAME,"ArtistModel",ListModel);
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
        if(mDatas != null && position - 1 < mDatas.size()){
            String artist = mDatas.get(position - 1).getArtist();
            return !TextUtils.isEmpty(artist) ? (Pinyin.toPinyin(artist.charAt(0))).toUpperCase().substring(0,1)  : "";
        }
        return "";
    }

    static class ArtistHolder extends BaseViewHolder {
        @BindView(R.id.item_text1)
        TextView mText1;
        @BindView(R.id.item_text2)
        @Nullable
        TextView mText2;
        @BindView(R.id.item_simpleiview)
        SimpleDraweeView mImage;
        @BindView(R.id.item_button)
        ImageButton mButton;
        @BindView(R.id.item_container)
        RelativeLayout mContainer;
        @BindView(R.id.item_root)
        @Nullable
        View mRoot;
        ArtistHolder(View v) {
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
