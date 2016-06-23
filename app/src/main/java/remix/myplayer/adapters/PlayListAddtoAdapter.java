package remix.myplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.ui.activities.PlayListActivity;

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
        return PlayListActivity.getPlayList().size();
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
        ViewHolder holder;
        //检查缓存
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playlist_addto_item,null);
            holder = new ViewHolder();
            holder.mText = (TextView)convertView.findViewById(R.id.playlist_addto_text);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

        //根据索引显示播放列表名
        String name = null;
        Iterator it = PlayListActivity.getPlayList().keySet().iterator();
        for(int i = 0 ; i<= position ;i++) {
            it.hasNext();
            name = it.next().toString();
        }
        holder.mText.setText(name);

        return convertView;
    }

    class ViewHolder {
        private TextView mText;
    }
}
