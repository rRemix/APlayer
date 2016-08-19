package remix.myplayer.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import remix.myplayer.theme.Theme;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.ui.activity.PlayListActivity;
import remix.myplayer.model.PlayListItem;
import remix.myplayer.listener.PopupListener;
import remix.myplayer.util.ColorUtil;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DBUtil;

/**
 * Created by taeja on 16-1-15.
 */

/**
 * 播放列表的适配器
 */
public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.PlayListHolder> {
    private Context mContext;
    public PlayListAdapter(Context context) {
        this.mContext = context;
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
        try {
            //根据当前索引，获得歌曲列表
            Object o = PlayListActivity.getPlayList();
            Iterator it = PlayListActivity.getPlayList().keySet().iterator();
            String name = "";
            for(int i = 0 ; i<= position ;i++) {
                it.hasNext();
                name = it.next().toString();
            }
            //设置播放列表名字
            holder.mName.setText(name);
            //设置背景
            holder.mContainer.setBackgroundResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.art_bg_day : R.drawable.art_bg_night);
            //设置专辑封面
            new AsynLoadImage(holder.mImage).execute(name);
//            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(name);
//            if(list != null && list.size() > 0) {
//                for(PlayListItem item : list){
//                    String url = DBUtil.getImageUrl(item.getAlbumId() + "",Constants.URL_ALBUM);
//                    if(url != null && !url.equals("")) {
//                        File file = new File(url);
//                        if(!file.exists())
//                            continue;
//                        holder.mImage.setImageURI(Uri.parse(url));
//                        break;
//                    }
//                }
//            }

            if(mOnItemClickLitener != null) {
                holder.mImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mImage,position);
                    }
                });
            }

            if(holder.mButton != null) {
                boolean isLove = name.equals(mContext.getString(R.string.my_favorite));
                Drawable drawable = mContext.getResources().getDrawable(isLove ? R.drawable.playlist_love : R.drawable.list_icn_more);
                Theme.TintDrawable(drawable, ColorUtil.getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.gray_6c6a6c : R.color.white));
                holder.mButton.setImageDrawable(drawable);
                holder.mButton.setClickable(!isLove);
                if(isLove){
                    holder.mButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Context wrapper = new ContextThemeWrapper(MainActivity.mInstance, R.style.PopupMenuDayStyle);
                            final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton,Gravity.END);
                            popupMenu.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                            popupMenu.setOnMenuItemClickListener(new PopupListener(mContext, position, Constants.PLAYLIST_HOLDER, ""));
                            popupMenu.show();
                        }
                    });
                }
            }
        } catch (Exception e){
            e.toString();
        }

    }

    @Override
    public int getItemCount() {
        return PlayListActivity.getPlayList() == null ? 0 : PlayListActivity.getPlayList().size();
    }

    public static class PlayListHolder extends BaseViewHolder {
        @BindView(R.id.playlist_item_name)
        public TextView mName;
        @BindView(R.id.recycleview_simpleiview)
        public SimpleDraweeView mImage;
        @BindView(R.id.recycleview_button)
        public ImageView mButton;
        @BindView(R.id.playlist_item_container)
        public RelativeLayout mContainer;
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
            ArrayList<PlayListItem> list = PlayListActivity.getPlayList().get(params[0]);
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
