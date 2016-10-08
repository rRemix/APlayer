package remix.myplayer.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.BindView;
import butterknife.ButterKnife;
import remix.myplayer.R;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-2-1.
 */

/**
 * 将歌曲添加到播放列表的适配器
 */
public class PlayListAddtoAdapter extends BaseAdapter{
    private Context mContext;

    public PlayListAddtoAdapter(Context Context) {
        this.mContext = Context;
    }

    @Override
    public int getCount() {
        return Global.mPlaylist != null ? Global.mPlaylist.size() : 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        PlayListAddToHolder holder;
        //检查缓存
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playlist_addto_item,null);
            holder = new PlayListAddToHolder(convertView);
            convertView.setTag(holder);
        }
        else
            holder = (PlayListAddToHolder)convertView.getTag();

        //根据索引显示播放列表名
        String name = null;
        Iterator it = Global.mPlaylist.keySet().iterator();
        for(int i = 0 ; i<= position ;i++) {
            it.hasNext();
            name = it.next().toString();
        }
        holder.mText.setText(name);

        return convertView;
    }

    public static class PlayListAddToHolder {
        @BindView(R.id.playlist_addto_text)
        public TextView mText;
        public PlayListAddToHolder(View itemView){
            ButterKnife.bind(this,itemView);
        }
    }
}
