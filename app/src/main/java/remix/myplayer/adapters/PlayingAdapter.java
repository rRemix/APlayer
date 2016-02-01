package remix.myplayer.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import remix.myplayer.R;
import remix.myplayer.utils.MP3Info;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/2.
 */
public class PlayingAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    private Context mContext;
    public PlayingAdapter(LayoutInflater inflater, Context context)
    {
        mContext = context;
        mInflater = inflater;
    }
    @Override
    public int getCount() {
        if(Utility.mPlayList == null)
            return 0;
        return Utility.mPlayList.size();
    }

    @Override
    public Object getItem(int position)
    {
        if(Utility.mPlayList == null || Utility.mPlayList.size() == 0)
            return null;
        return Utility.mPlayList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View ItemView = mInflater.inflate(R.layout.playlist_item,null);
        if(Utility.mPlayList == null || Utility.mPlayList.size() == 0)
            return ItemView;
        TextView title = (TextView)ItemView.findViewById(R.id.playlist_item_name);
        TextView artist = (TextView)ItemView.findViewById(R.id.playlist_item_artist);

        MP3Info temp = new MP3Info(Utility.getMP3InfoById(Utility.mPlayList.get(position)));
        title.setText(temp.getDisplayname());
        artist.setText(temp.getArtist());

//        ImageView button = (ImageView)ItemView.findViewById(R.id.playlist_button_delete);
//        button.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                MP3Info temp = Utility.getMP3InfoById(Utility.mPlayList.get(position));
//                if (temp != null) {
//                    Utility.mPlayList.remove(position);
//                }
//                Toast.makeText(v.getContext(), "删除了第" + position + "项", Toast.LENGTH_SHORT).show();
//                notifyDataSetChanged();
//            }
//        });
        return ItemView;
    }
}
