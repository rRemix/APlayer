package remix.myplayer.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.db.PlayListNewInfo;
import remix.myplayer.interfaces.OnItemClickListener;
import remix.myplayer.util.Global;
import remix.myplayer.util.PlayListUtil;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的适配器
 */
public class PlayListAddtoAdapter extends RecyclerView.Adapter<PlayListAddtoAdapter.PlayListAddToHolder>{
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;
    private Cursor mCursor;

    public PlayListAddtoAdapter(Context Context) {
        this.mContext = Context;
    }

    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }
    public void setCursor(Cursor cursor) {
        mCursor = cursor;
        notifyDataSetChanged();
    }

//    @Override
//    public View getView(int position, View convertView, ViewGroup parent) {
//        PlayListAddToHolder holder;
//        //检查缓存
//        if(convertView == null) {
//            convertView = LayoutInflater.from(mContext).inflate(R.layout.playlist_addto_item,null);
//            holder = new PlayListAddToHolder(convertView);
//            convertView.setTag(holder);
//        }
//        else
//            holder = (PlayListAddToHolder)convertView.getTag();
//
//        //根据索引显示播放列表名
//        String name = null;
//        Iterator it = Global.mPlaylist.keySet().iterator();
//        for(int i = 0 ; i<= position ;i++) {
//            it.hasNext();
//            name = it.next().toString();
//        }
//        holder.mText.setText(name);
//
//        return convertView;
//    }

    @Override
    public PlayListAddToHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new PlayListAddToHolder(LayoutInflater.from(mContext).inflate(R.layout.playlist_addto_item,null));
    }

    @Override
    public void onBindViewHolder(PlayListAddToHolder holder, final int position) {
        if(mCursor.moveToPosition(position)){
            PlayListNewInfo info = PlayListUtil.getPlayListInfo(mCursor);
            if(info == null) {
                holder.mText.setText(R.string.load_playlist_error);
                return;
            }
            holder.mText.setText(info.Name);
            holder.mText.setTag(info._Id);
            if(mOnItemClickLitener != null){
                holder.mContainer.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mOnItemClickLitener.onItemClick(v,position);
                    }
                });
            }

        }
    }

    @Override
    public int getItemCount() {
        return mCursor != null ? mCursor.getCount() : 0;
    }

    public static class PlayListAddToHolder extends BaseViewHolder{
        @BindView(R.id.playlist_addto_text)
        TextView mText;
        @BindView(R.id.item_root)
        LinearLayout mContainer;
        public PlayListAddToHolder(View itemView){
           super(itemView);
        }
    }
}
