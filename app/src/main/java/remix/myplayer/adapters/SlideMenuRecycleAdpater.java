package remix.myplayer.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import remix.myplayer.R;


/**
 * Created by Remix on 2015/12/18.
 */
public class SlideMenuRecycleAdpater extends RecyclerView.Adapter<SlideMenuRecycleAdpater.VHolder>{
    private int draws[] = new int[]{R.drawable.menu_icon_about,R.drawable.menu_icon_find_music,
            R.drawable.menu_icon_playmode, R.drawable.menu_icon_refresh_lib,R.drawable.menu_icon_exit};
    private String strings[] = new String[]{"关于软件","搜索本地","随便来一曲","刷新乐库","退出"};
    private LayoutInflater mInflater;

    public SlideMenuRecycleAdpater(LayoutInflater mInflater) {
        this.mInflater = mInflater;
    }

    @Override
    public VHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        VHolder holder = new VHolder(mInflater.inflate(R.layout.slide_menu_item,null));
        return holder;

    }

    @Override
    public void onBindViewHolder(VHolder holder, int position) {
        holder.mTextView.setText(strings[position]);
        holder.mImage.setImageResource(draws[position]);
    }

    @Override
    public int getItemCount() {
        return 5;
    }

    class VHolder extends RecyclerView.ViewHolder {
        TextView mTextView;
        ImageView mImage;
        public VHolder(View itemView) {
            super(itemView);
            mTextView = (TextView)itemView.findViewById(R.id.slide_menu_text);
            mImage = (ImageView)itemView.findViewById(R.id.slide_menu_image);
        }
    }

}


