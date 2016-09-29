package remix.myplayer.adapter;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.model.MultiPosition;
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.listener.AlbArtFolderPlaylistListener;
import remix.myplayer.ui.MultiChoice;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.PlayListHolder> {
    private Context mContext;
    private MultiChoice mMultiChoice;
    public PlayListAdapter(Context context,MultiChoice multiChoice) {
        this.mContext = context;
        this.mMultiChoice = multiChoice;
    }

    private OnItemClickLitener mOnItemClickLitener;
    public interface OnItemClickLitener {
        void onItemClick(View view, int position);
        void onItemLongClick(View view , int position);
    }
    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener) {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public PlayListHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlayListHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_recycle_item, null, false));
    }

    @Override
    public void onBindViewHolder(final PlayListHolder holder, final int position) {
        String name = "";
        try {
            //根据当前索引，获得歌曲列表
            Iterator it = Global.mPlaylist.keySet().iterator();
            for(int i = 0 ; i<= position ;i++) {
                it.hasNext();
                name = it.next().toString();
            }
        } catch (Exception e){
            e.toString();
        }
        //设置播放列表名字
        holder.mName.setText(name);
        //设置背景
        holder.mContainer.setBackgroundResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.art_bg_day : R.drawable.art_bg_night);
        //设置专辑封面
        new AsynLoadImage(holder.mImage).execute(name);

        if(mOnItemClickLitener != null) {
            holder.mContainer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(holder.mCardBackground,holder.getAdapterPosition());
                }
            });
            //多选菜单
            holder.mContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    mOnItemClickLitener.onItemLongClick(holder.mCardBackground,holder.getAdapterPosition());
                    return true;
                }
            });
        }

        if(holder.mButton != null) {
            boolean isLove = name.equals(mContext.getString(R.string.my_favorite));
//            Theme.TintDrawable(holder.mButton,
//                    isLove ? R.drawable.playlist_love : R.drawable.list_icn_more,
//                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6c6a6c : R.color.white));
            Theme.TintDrawable(holder.mButton, R.drawable.list_icn_more,
                    ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6c6a6c : R.color.white));
//            if(!isLove){
                final String finalName = name;
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if(mMultiChoice.isShow())
                            return;
                        Context wrapper = new ContextThemeWrapper(mContext,Theme.getPopupMenuStyle());
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        popupMenu.getMenuInflater().inflate(R.menu.playlist_menu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new AlbArtFolderPlaylistListener(mContext, holder.getAdapterPosition(), Constants.PLAYLIST, finalName));
                        popupMenu.show();
                    }
                });
//            }
        }
        if(MultiChoice.TAG.equals(PlayListActivity.TAG) &&
                mMultiChoice.mSelectedPosition.contains(new MultiPosition(position))){
            mMultiChoice.AddView(holder.mCardBackground);
        } else {
            holder.mCardBackground.setSelected(false);
        }
    }

    @Override
    public int getItemCount() {
        return Global.mPlaylist == null ? 0 :Global.mPlaylist.size();
    }

    public static class PlayListHolder extends BaseViewHolder {
        @BindView(R.id.playlist_item_name)
        public TextView mName;
        @BindView(R.id.recycleview_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.recycleview_button)
        public ImageView mButton;
        @BindView(R.id.item_container)
        public RelativeLayout mContainer;
        @BindView(R.id.recycleview_card)
        public Button mCardBackground;

        public PlayListHolder(View itemView) {
            super(itemView);

        }
    }

    class AsynLoadImage extends AsyncTask<String,Integer,String> {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(String... params) {
            ArrayList<PlayListItem> list = Global.mPlaylist.get(params[0]);
            String url = null;
            if(list != null && list.size() > 0) {
                for(PlayListItem item : list){
                    url = DBUtil.getImageUrl(item.getAlbumId() + "",Constants.URL_ALBUM);
                    if(url != null && !url.equals("")) {
                        File file = new File(url);
                        if(file.exists()) {
                            break;
                        }
                    }
                }
            }
            return url;
        }
        @Override
        protected void onPostExecute(String url) {
            Uri uri = Uri.parse("file:///" + url);
            if(mImage != null)
                mImage.setImageURI(uri);
        }
    }

}
