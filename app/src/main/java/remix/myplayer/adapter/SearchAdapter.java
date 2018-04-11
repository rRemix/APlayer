package remix.myplayer.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.menu.SongPopupListener;
import remix.myplayer.request.LibraryUriRequest;
import remix.myplayer.request.RequestConfig;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.DensityUtil;

import static remix.myplayer.request.ImageUriRequest.SMALL_IMAGE_SIZE;
import static remix.myplayer.util.ImageUriUtil.getSearchRequestWithAlbumType;

/**
 * Created by Remix on 2016/1/23.
 */

/**
 * 搜索结果的适配器
 */
public class SearchAdapter extends BaseAdapter<Song,SearchAdapter.SearchResHolder> {

    private final GradientDrawable mDefaultDrawable;
    private final GradientDrawable mSelectDrawable;

    public SearchAdapter(Context context, int layoutId){
        super(context,layoutId);
        int size = DensityUtil.dip2px(mContext,60);
        mDefaultDrawable = Theme.getShape(GradientDrawable.OVAL,Color.TRANSPARENT,size,size);
        mSelectDrawable = Theme.getShape(GradientDrawable.OVAL,ThemeStore.getSelectColor(),size,size);
    }

    @SuppressLint("RestrictedApi")
    @Override
    protected void convert(final SearchResHolder holder, Song song, int position) {
        holder.mName.setText(song.getTitle());
        holder.mOther.setText(String.format("%s-%s", song.getArtist(), song.getAlbum()));
        //封面
        new LibraryUriRequest(holder.mImage,
                getSearchRequestWithAlbumType(song),
                new RequestConfig.Builder(SMALL_IMAGE_SIZE, SMALL_IMAGE_SIZE).build()).load();

        //设置按钮着色
        int tintColor = ThemeStore.THEME_MODE == ThemeStore.DAY ? ColorUtil.getColor(R.color.gray_6c6a6c) : Color.WHITE;
        Theme.TintDrawable(holder.mButton,R.drawable.icon_player_more,tintColor);

        //按钮点击效果
        holder.mButton.setBackground(Theme.getPressDrawable(
                mDefaultDrawable,
                mSelectDrawable,
                ThemeStore.getRippleColor(),
                null,null));

        holder.mButton.setOnClickListener(v -> {
            Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton, Gravity.END);
            popupMenu.getMenuInflater().inflate(R.menu.menu_song_item, popupMenu.getMenu());
            popupMenu.setOnMenuItemClickListener(new SongPopupListener(mContext,song,false,""));
            popupMenu.show();
        });

        if(mOnItemClickLitener != null && holder.mRooView != null){
            holder.mRooView.setOnClickListener(v -> mOnItemClickLitener.onItemClick(v,holder.getAdapterPosition()));
        }
    }

    static class SearchResHolder extends BaseViewHolder {
        @BindView(R.id.reslist_item)
        RelativeLayout mRooView;
        @BindView(R.id.search_image)
        SimpleDraweeView mImage;
        @BindView(R.id.search_name)
        TextView mName;
        @BindView(R.id.search_detail)
        TextView mOther;
        @BindView(R.id.search_button)
        ImageButton mButton;
        SearchResHolder(View itemView){
           super(itemView);
        }
    }
}
