package remix.myplayer.adapters;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Utility;

/**
 * Created by Remix on 2015/12/5.
 */
public class FolderAdapter extends BaseAdapter {
    private LayoutInflater mInflater;
    public static FolderAdapter mInstance;
    private Context mContext;
    private static int mIndex = 0;

    public FolderAdapter(Context Context, LayoutInflater Inflater) {
        mInstance = this;
        this.mContext = Context;
        this.mInflater = Inflater;
    }

    @Override
    public int getCount() {
        if(Utility.mFolderList == null)
            return 0;
        return Utility.mFolderList.size();
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
    public View getView(final int position, View convertView, ViewGroup parent) {
        View ItemView = mInflater.inflate(R.layout.folder_item,null);
        TextView name = (TextView)ItemView.findViewById(R.id.folder_name);
        TextView num = (TextView)ItemView.findViewById(R.id.folder_num);
        TextView path = (TextView)ItemView.findViewById(R.id.folder_path);
        final ImageView button = (ImageView)ItemView.findViewById(R.id.folder_button);
        if(Utility.mFolderList == null || Utility.mFolderList.size() < 0)
            return ItemView;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(mContext,button);
                MainActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                        position,
                        Utility.FOLDER_HOLDER,
                        Utility.mFolderList.get(position)));
                popupMenu.setGravity(Gravity.END );
                popupMenu.show();
            }
        });
        Cursor cursor = null;
        try
        {
            cursor = mContext.getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    new String[]{MediaStore.Video.Media.BUCKET_DISPLAY_NAME,MediaStore.Audio.Media.DATA},
                    MediaStore.Video.Media.BUCKET_DISPLAY_NAME + "=?",
                    new String[]{Utility.mFolderList.get(position)},null);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        cursor.moveToFirst();
        if(cursor != null && cursor.getCount() > 0)
        {
            name.setText(cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)));
            String displayname  = cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DATA));
            path.setText(displayname.substring(0,displayname.lastIndexOf("/")));
            num.setText(String.valueOf(cursor.getCount()) + "首");
        }
        cursor.close();
        return ItemView;
    }



    class ViewHolder
    {
        public TextView mName;
        public TextView mPath;
        public TextView mCount;
        public ImageView mButton;

        public ViewHolder(TextView name, TextView path, TextView count, ImageView button) {
            this.mName = name;
            this.mPath = path;
            this.mCount = count;
            this.mButton = button;
        }
    }
}
