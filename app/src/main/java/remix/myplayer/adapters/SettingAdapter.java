package remix.myplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import remix.myplayer.R;

/**
 * Created by taeja on 16-3-7.
 */
public class SettingAdapter extends BaseAdapter {
    private final String[] mTitles = new String[]{"扫描文件大小","意见和反馈","关于我们","检查更新"};
    private Context mContext;

    public SettingAdapter(Context Context) {
        this.mContext = Context;
    }

    @Override
    public int getCount() {
        return mTitles.length;
    }

    @Override
    public Object getItem(int position) {
        return mTitles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.setting_item,null);
        }
        TextView mText = (TextView)convertView.findViewById(R.id.setting_item_text);
        mText.setText(getItem(position).toString());
        return convertView;
    }
}
