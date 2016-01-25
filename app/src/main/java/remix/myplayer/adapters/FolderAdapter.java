package remix.myplayer.adapters;

import android.content.Context;
import android.os.Environment;
import android.support.v7.widget.PopupMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;
import java.security.Key;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.NamespaceContext;

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
    public FolderAdapter(LayoutInflater Inflater,Context context) {
        this.mInflater = Inflater;
        mInstance = this;
        mContext = context;
    }

    @Override
    public int getCount() {
        if(Utility.mFolderMap == null)
            return 0;
        return Utility.mFolderMap.size();
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
        if(Utility.mFolderMap == null || Utility.mFolderMap.size() < 0)
            return ItemView;
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final PopupMenu popupMenu = new PopupMenu(mContext,button);
                MainActivity.mInstance.getMenuInflater().inflate(R.menu.pop_menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                        position,
                        Utility.FOLDER_HOLDER));
                popupMenu.setGravity(Gravity.END );
                popupMenu.show();
            }
        });
        Set set = Utility.mFolderMap.keySet();
        Iterator it = set.iterator();
        int i = 0;
        while(it.hasNext())
        {
            if(i++ == position)
            {
                String RootPath = (String)it.next();
                int Count = Utility.mFolderMap.get(RootPath).size();
                String Name = RootPath.substring(RootPath.lastIndexOf('/') + 1,RootPath.length());
                name.setText(Name);
                num.setText(Count + "首歌曲");
                path.setText(RootPath);
                break;
            }
            it.next();
        }
        return ItemView;
    }
}
