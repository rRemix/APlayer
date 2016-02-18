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

import java.util.Iterator;
import java.util.LinkedList;

import remix.myplayer.R;
import remix.myplayer.activities.MainActivity;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.DBUtil;

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
        if(DBUtil.mFolderMap == null)
            return 0;
        return DBUtil.mFolderMap.size();
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
        TextView mNameView = (TextView)ItemView.findViewById(R.id.folder_name);
        TextView mNumView = (TextView)ItemView.findViewById(R.id.folder_num);
        TextView mPathView = (TextView)ItemView.findViewById(R.id.folder_path);
        final ImageView button = (ImageView)ItemView.findViewById(R.id.folder_button);

        if(DBUtil.mFolderMap == null || DBUtil.mFolderMap.size() < 0)
            return ItemView;
        Iterator it = DBUtil.mFolderMap.keySet().iterator();
        String temp = null;
        for(int i = 0 ; i <= position ; i++)
            temp = it.next().toString();

        if(temp != null){
            mNameView.setText(temp.substring(temp.lastIndexOf("/")+ 1,temp.length()));
            mPathView.setText(temp);
            mNumView.setText(DBUtil.mFolderMap.get(temp).size()+ "首");
        }
        final String full_path = temp;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(mContext,button);
                MainActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                        position,
                        Constants.FOLDER_HOLDER,
                        full_path));
                popupMenu.setGravity(Gravity.END );
                popupMenu.show();
            }
        });

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
