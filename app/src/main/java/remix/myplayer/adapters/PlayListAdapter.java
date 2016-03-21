package remix.myplayer.adapters;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.support.v7.view.ContextThemeWrapper;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Constants;
import remix.myplayer.infos.PlayListItem;
import remix.myplayer.utils.ErrUtil;

/**
 * Created by taeja on 16-1-15.
 */
public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.ViewHolder> {
    private Context mContext;
    public PlayListAdapter(Context context)
    {
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        try {
            Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
            PlayListItem item = null;
            String name = "";
            for(int i = 0 ; i<= position ;i++) {
                it.hasNext();
                name = it.next().toString();
            }
            holder.mName.setText(name);

            ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
            if(list != null && list.size() > 0) {
                holder.mImage.setImageURI(ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), list.get(0).getAlbumId()));
            }

            if(mOnItemClickLitener != null) {
                holder.mImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(holder.mImage,position);
                    }
                });
                holder.mImage.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        mOnItemClickLitener.onItemLongClick(holder.mImage,position);
                        return true;
                    }
                });
            }

            if(holder.mButton != null) {
                //第一次个列表为收藏列表，需要做点处理
                if(position == 0){
                    holder.mButton.setImageResource(R.drawable.rcd_icn_love);
                    holder.mButton.setClickable(false);
                    holder.mButton.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
                    return;
                }
                holder.mButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Context wrapper = new ContextThemeWrapper(mContext, R.style.MyPopupMenu);
                        final PopupMenu popupMenu = new PopupMenu(wrapper,holder.mButton);
                        PlayListActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupListener(mContext, position, Constants.PLAYLIST_HOLDER, ""));
                        popupMenu.setGravity(Gravity.END);
                        popupMenu.show();
                    }
                });
            }
        } catch (Exception e){
            ErrUtil.writeError("PlayListAdapter" + "---onBindViewHolder---" + e.toString());
        }

    }

    @Override
    public int getItemCount() {
        return PlayListActivity.mPlaylist == null ? 0 : PlayListActivity.mPlaylist.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mName;
        public final SimpleDraweeView mImage;
        public final ImageView mButton;
        public ViewHolder(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.playlist_item_name);
            mImage = (SimpleDraweeView)itemView.findViewById(R.id.recycleview_simpleiview);
            mButton = (ImageView)itemView.findViewById(R.id.recycleview_button);
        }
    }

}
