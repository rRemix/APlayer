package remix.myplayer.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Iterator;

import butterknife.BindView;
import remix.myplayer.R;
import remix.myplayer.adapter.holder.BaseViewHolder;
import remix.myplayer.listener.OnItemClickListener;
import remix.myplayer.listener.PopupListener;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.activity.MainActivity;
import remix.myplayer.util.Constants;
import remix.myplayer.util.Global;

/**
 * Created by taeja on 16-6-23.
 */
public class FolderAdapter extends RecyclerView.Adapter<FolderAdapter.FolderHolder> {
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
    public FolderHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FolderHolder(LayoutInflater.from(mContext).inflate(R.layout.folder_recycle_item,null,false));
    }

    @Override
    public void onBindViewHolder(final FolderHolder holder, final int position) {
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
            holder.mName.setTextColor(mContext.getResources().getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.color_black_1c1b19 : R.color.color_white));

            holder.mPath.setText(temp);
            holder.mPath.setTextColor(mContext.getResources().getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.color_gray_6d6c69 : R.color.color_gray_6c6a6c));

            holder.mCount.setText(Global.mFolderMap.get(temp).size()+ "首");
            holder.mCount.setTextColor(mContext.getResources().getColor(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.color.color_black_1c1b19 : R.color.color_white));
        }
        //根据主题模式 设置图片
        if(holder.mImg != null) {
            holder.mImg.setImageResource(ThemeStore.THEME_MODE == ThemeStore.DAY ? R.drawable.scan_icn_folder_day : R.drawable.scan_icn_folder);
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

    public static class FolderHolder extends BaseViewHolder {
        public View mRootView;
        @BindView(R.id.folder_image)
        public ImageView mImg;
        @BindView(R.id.folder_name)
        public TextView mName;
        @BindView(R.id.folder_path)
        public TextView mPath;
        @BindView(R.id.folder_num)
        public TextView mCount;
        @BindView(R.id.folder_button)
        public ImageButton mButton;
        public FolderHolder(View itemView) {
            super(itemView);
            mRootView = itemView;
        }
    }
}
