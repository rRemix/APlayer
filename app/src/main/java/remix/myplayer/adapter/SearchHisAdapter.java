package remix.myplayer.adapter;

import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;

/**
 * Created by taeja on 16-3-1.
 */
public class SearchHisAdapter extends BaseAdapter {
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            notifyDataSetChanged();
        }
    };
    @Override
    public int getCount() {
//        return SearchActivity.mSearchHisKeyList.size();
        return 0;
    }

    @Override
    public Object getItem(int position) {
//        return SearchActivity.mSearchHisKeyList.get(position);
        return 0;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        SearchHisHolder holder = null;
        if(convertView == null){
            convertView = LayoutInflater.from(MainActivity.mInstance.getApplicationContext()).inflate(R.layout.search_history_item,null);
            holder = new SearchHisHolder();
            holder.mSearchKey = (TextView)convertView.findViewById(R.id.search_history_item_text);
            holder.mDelete = (ImageButton)convertView.findViewById(R.id.search_history_item_delete);
            convertView.setTag(holder);
        }
        else
            holder = (SearchHisHolder)convertView.getTag();

        if(getItem(position) == null)
            return convertView;
        holder.mSearchKey.setText(getItem(position).toString());
        holder.mDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                XmlUtil.deleteKey(getItem(position).toString());
                mHandler.sendEmptyMessage(Constants.NOTIFYDATACHANGED);
            }
        });

        return convertView;
    }

    class SearchHisHolder {
        public TextView mSearchKey;
        public ImageButton mDelete;
    }
}
