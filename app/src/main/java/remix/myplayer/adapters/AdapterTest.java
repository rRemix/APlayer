package remix.myplayer.adapters;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import remix.myplayer.R;
import remix.myplayer.ui.SelectedPopupWindow;

/**
 * Created by Remix on 2015/12/11.
 */
public class AdapterTest extends SimpleCursorAdapter
{
    public static AdapterTest mInstance;
    private LayoutInflater mInflater;
    private Context mContext;

    public AdapterTest(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
        super(context, layout, c, from, to, flags);
        mContext = context;
//        mInflater =
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        return null;
//        ViewHolder holder;
//        if(convertView == null)
//        {
//            convertView = super.getView(position,convertView,parent);
//            holder = new ViewHolder();
//            holder.mName = (TextView)convertView.findViewById(R.id.displayname);
//            holder.mOther = (TextView)convertView.findViewById(R.id.detail);
//            convertView.setTag(holder);
//        }
//        else
//            holder = (ViewHolder)convertView.getTag();
//
//        Cursor mCursor = (Cursor)getItem(position);
//        String name = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
//        name = name.substring(0, name.lastIndexOf("."));
//        String artist = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST));
//        String album = mCursor.getString(mCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM));
//
//
//        holder.mName.setText(name);
//        holder.mOther.setText(artist + "-" + album);
//        ImageView mItemButton = (ImageView)convertView.findViewById(R.id.allsong_item_button);
//
//        mItemButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(mContext, SelectedPopupWindow.class);
//                intent.putExtra("Position",position);
//                mContext.startActivity(intent);
//            }
//        });
//        return convertView;
    }
}
