package remix.myplayer.adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import java.util.ArrayList;
import remix.myplayer.R;
import remix.myplayer.infos.MP3Info;
import remix.myplayer.services.MusicService;

/**
 * Created by Remix on 2015/12/4.
 */
public class ChildHolderAdapter extends BaseAdapter {
    private ArrayList<MP3Info> mInfoList;
    private LayoutInflater mInflater;
    public ChildHolderAdapter(ArrayList<MP3Info> mInfoList, LayoutInflater mInflater) {
        this.mInfoList = mInfoList;
        this.mInflater = mInflater;
    }
    @Override
    public int getCount() {
        if(mInfoList == null)
            return 0;
        return mInfoList.size();
    }
    @Override
    public Object getItem(int position) {
        if(mInfoList == null || mInfoList.size() == 0)
            return null;
        return mInfoList.get(position);
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if(convertView == null)
        {
            convertView = mInflater.inflate(R.layout.child_holder_item,null);
            holder = new ViewHolder();
            holder.mArtist = (TextView)convertView.findViewById(R.id.album_holder_item_artist);
            holder.mTitle = (TextView)convertView.findViewById(R.id.album_holder_item_title);
            convertView.setTag(holder);
        }
        else
            holder = (ViewHolder)convertView.getTag();
        if(mInfoList == null || mInfoList.size() == 0 )
            return convertView;
        MP3Info temp = mInfoList.get(position);

        if(temp.getDisplayname().equals(MusicService.getCurrentMP3().getDisplayname()))
            holder.mTitle.setTextColor(Color.parseColor("#ff0030"));
        else
            holder.mTitle.setTextColor(Color.parseColor("#1b1c19"));
        holder.mTitle.setText(temp.getDisplayname());
        holder.mArtist.setText(temp.getArtist());
        return convertView;
    }
    class ViewHolder
    {
        private TextView mTitle;
        private TextView mArtist;
    }
}