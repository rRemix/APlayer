package remix.myplayer.adapters;

import android.content.Context;
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
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.util.ArrayList;
import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;
import remix.myplayer.infos.PlayListItem;

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
    public interface OnItemClickLitener
    {
        void onItemClick(View view, int position);
        void onItemLongClick(View view , int position);
    }
    public void setOnItemClickLitener(OnItemClickLitener mOnItemClickLitener)
    {
        this.mOnItemClickLitener = mOnItemClickLitener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        ViewHolder holder = new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.playlist_recycle_item, null, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Iterator it = PlayListActivity.mPlaylist.keySet().iterator();
        PlayListItem item = null;
        String name = null;
        for(int i = 0 ; i<= position ;i++) {
            it.hasNext();
            name = it.next().toString();
        }
        holder.mName.setText(name);

        ArrayList<PlayListItem> list = PlayListActivity.mPlaylist.get(name);
        if(list != null && list.size() > 0)
        {
            AsynLoadImage task = new AsynLoadImage(holder.mImage);
            task.execute(list.get(0).getAlbumId());
        }

        if(mOnItemClickLitener != null)
        {
            holder.mImage.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickLitener.onItemClick(holder.mImage,position);
                }
            });
        }
        if(holder.mButton != null)
        {
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
    }

    @Override
    public int getItemCount() {
        return PlayListActivity.mPlaylist.size();
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

    class AsynLoadImage extends AsyncTask<Integer,Integer,String>
    {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(Integer... params) {
            return DBUtil.getImageUrl(params[0] + "", Constants.URL_ALBUM);
        }
        @Override
        protected void onPostExecute(String url) {
            Uri uri = Uri.parse("file:///" + url);
            if(url != null && mImage != null)
                mImage.setImageURI(uri);
        }
    }
}
