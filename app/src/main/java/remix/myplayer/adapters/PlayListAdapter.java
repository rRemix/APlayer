package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import java.security.PrivateKey;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import remix.myplayer.R;
import remix.myplayer.activities.ChildHolderActivity;
import remix.myplayer.activities.PlayListActivity;
import remix.myplayer.utils.Utility;

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
        String name = null;
        for(int i = 0 ; i<= position ;i++) {
            it.hasNext();
            name = it.next().toString();
        }
        holder.mName.setText(name);

        ArrayList<String> list = PlayListActivity.mPlaylist.get(name);
        if(list != null && list.size() > 0)
        {
            AsynLoadImage task = new AsynLoadImage(holder.mImage);
            task.execute(list.get(0));
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
    }

    @Override
    public int getItemCount() {
        return PlayListActivity.mPlaylist.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public final TextView mName;
        public final SimpleDraweeView mImage;
        public ViewHolder(View itemView) {
            super(itemView);
            mName = (TextView) itemView.findViewById(R.id.playlist_item_name);
            mImage = (SimpleDraweeView)itemView.findViewById(R.id.recycleview_simpleiview);
        }
    }

    class AsynLoadImage extends AsyncTask<String,Integer,String>
    {
        private final SimpleDraweeView mImage;
        public AsynLoadImage(SimpleDraweeView imageView)
        {
            mImage = imageView;
        }
        @Override
        protected String doInBackground(String... params) {
            return Utility.getImageUrl(params[0],Utility.URL_NAME);
        }
        @Override
        protected void onPostExecute(String url) {
            Uri uri = Uri.parse("file:///" + url);
            if(url != null && mImage != null)
                mImage.setImageURI(uri);
        }
    }
}
