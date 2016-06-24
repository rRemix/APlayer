package remix.myplayer.adapters;

import android.content.Context;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;

import remix.myplayer.R;
import remix.myplayer.listeners.OnItemClickListener;
import remix.myplayer.listeners.PopupListener;
import remix.myplayer.ui.activities.MainActivity;
import remix.myplayer.utils.Constants;
import remix.myplayer.utils.Global;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.ViewHolder> {
    private Context mContext;
    private OnItemClickListener mOnItemClickLitener;

    public FolderAdapter(Context context) {
        this.mContext = context;
    }
    public void setOnItemClickLitener(OnItemClickListener l)
    {
        this.mOnItemClickLitener = l;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(mContext).inflate(R.layout.folder_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        if(Global.mFolderMap == null || Global.mFolderMap.size() < 0)
            return ;
        //根据当前索引 获得对应的歌曲列表
        Iterator it = Global.mFolderMap.keySet().iterator();
        String temp = null;
        for(int i = 0 ; i <= position ; i++)
            temp = it.next().toString();
        //设置文件夹名字 路径名 歌曲数量
        if(temp != null){
            holder.mName.setText(temp.substring(temp.lastIndexOf("/")+ 1,temp.length()));
            holder.mPath.setText(temp);
            holder.mCount.setText(Global.mFolderMap.get(temp).size()+ "首");
        }

        final String full_path = temp;
        if(holder.mButton != null) {
            holder.mButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final PopupMenu popupMenu = new PopupMenu(mContext, v);
                    MainActivity.mInstance.getMenuInflater().inflate(R.menu.alb_art_menu, popupMenu.getMenu());
                    popupMenu.setOnMenuItemClickListener(new PopupListener(mContext,
                            position,
                            Constants.FOLDER_HOLDER,
                            full_path));
                    popupMenu.setGravity(Gravity.END);
                    popupMenu.show();
                }
            });
        }

        if(mOnItemClickLitener != null && holder.mRootView != null) {
            holder.mRootView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = holder.getAdapterPosition();
                    mOnItemClickLitener.onItemClick(v,pos);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return Global.mFolderMap == null ? 0 : Global.mFolderMap.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        public View mRootView;
        public TextView mName;
        public TextView mPath;
        public TextView mCount;
        public ImageButton mButton;
        public ViewHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
            mName = (TextView)itemView.findViewById(R.id.folder_name);
            mCount = (TextView)itemView.findViewById(R.id.folder_num);
            mPath = (TextView)itemView.findViewById(R.id.folder_path);
            mButton = (ImageButton)itemView.findViewById(R.id.folder_button);
        }
    }
}
