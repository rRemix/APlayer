package remix.myplayer.adapter;

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
    private final String[] mTitles = new String[]{"扫描文件大小","均衡器设置","通知栏底色","意见和反馈","关于APlayer","检查更新"};
    private Context mContext;

    public SettingAdapter(Context Context) {
        this.mContext = Context;
    }

    @Override
    public int getCount() {
        return mTitles == null ? 0 : mTitles.length;
    }

    @Override
    public Object getItem(int position) {
        return mTitles == null ? null : mTitles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(convertView == null){
            convertView = LayoutInflater.from(mContext).inflate(R.layout.item_setting,null);
        }
        TextView mText = (TextView)convertView.findViewById(R.id.setting_item_text);
        mText.setText(getItem(position).toString());
        return convertView;
    }
}
