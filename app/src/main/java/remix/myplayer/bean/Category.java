package remix.myplayer.bean;

import android.text.TextUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import remix.myplayer.APlayerApplication;
import remix.myplayer.R;

/**
 * Created by Remix on 2018/1/10.
 */

public class Category implements Serializable{
    private static final long serialVersionUID = 8896405422136136674L;
    private int mIndex;
    private int mResID;
    private String mTitle;

    public Category(int resId) {
        mResID = resId;
        mIndex = mResID == R.string.tab_song ? 0 :
                mResID == R.string.tab_album ? 1 :
                mResID == R.string.tab_artist ? 2 :
                mResID == R.string.tab_playlist ? 3 : 4;
    }

    public String getTitle(){
        return !TextUtils.isEmpty(mTitle) ? mTitle : APlayerApplication.getContext().getString(mResID);
    }

    public int getIndex(){
        return mIndex;
    }

    public int getResId(){
        return mResID;
    }

    public void setResId(int id){
        mResID = id;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && mResID == ((Category)o).getResId();
    }

    public static final List<Integer> ALL_LIBRARY_RES = Arrays.asList(R.string.tab_song,
            R.string.tab_album,R.string.tab_artist,
            R.string.tab_playlist,R.string.tab_folder);
    public static final List<String> ALL_LIBRARY_STRING = Arrays.asList(APlayerApplication.getContext().getResources().getString(R.string.tab_song),
            APlayerApplication.getContext().getResources().getString(R.string.tab_album),APlayerApplication.getContext().getResources().getString(R.string.tab_artist),
            APlayerApplication.getContext().getResources().getString(R.string.tab_playlist),APlayerApplication.getContext().getResources().getString(R.string.tab_folder));
    public static final List<Category> DEFAULT_LIBRARY = Arrays.asList(
            new Category(R.string.tab_song),
            new Category(R.string.tab_album),
            new Category(R.string.tab_artist),
            new Category(R.string.tab_playlist),
            new Category(R.string.tab_folder));
}
