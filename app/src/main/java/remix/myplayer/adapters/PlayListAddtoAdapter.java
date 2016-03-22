package remix.myplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.util.Iterator;
import java.util.zip.Inflater;

import remix.myplayer.R;
import remix.myplayer.activities.PlayListActivity;

/**
 * Created by taeja on 16-2-1.
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
        if(convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.playlist_addto_item,null);
            holder = new ViewHolder();
            holder.mText = (TextView)convertView.findViewById(R.id.playlist_addto_text);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();

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
