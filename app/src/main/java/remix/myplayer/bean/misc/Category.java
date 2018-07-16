package remix.myplayer.bean.misc;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import remix.myplayer.App;
import remix.myplayer.R;

/**
 * Created by Remix on 2018/1/10.
 */

public class Category implements Serializable{
    private static final long serialVersionUID = 8896405422136136674L;
    private String mTitle;
    private int mOrder;
    private int mTag;

    public Category(String title) {
        this.mTitle = title;
        mTag = App.getContext().getString(R.string.tab_song).equals(mTitle) ? TAG_SONG :
                App.getContext().getString(R.string.tab_album).equals(mTitle) ? TAG_ALBUM :
                App.getContext().getString(R.string.tab_artist).equals(mTitle) ? TAG_ARTIST :
                App.getContext().getString(R.string.tab_playlist).equals(mTitle) ? TAG_PLAYLIST : TAG_FOLDER;
        mOrder = mTag;
    }

    public int getTag() {
        return mTag;
    }

    public void setTag(int tag) {
        this.mTag = tag;
    }

    public String getTitle(){
        return mTitle;
    }

    public int getOrder(){
        return mOrder;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && mTitle.equals(((Category)o).getTitle());
    }

    public static final int TAG_SONG = 0;
    public static final int TAG_ALBUM = 1;
    public static final int TAG_ARTIST = 2;
    public static final int TAG_PLAYLIST = 3;
    public static final int TAG_FOLDER = 4;

    public static final List<String> ALL_LIBRARY_STRING = Arrays.asList(App.getContext().getResources().getString(R.string.tab_song),
            App.getContext().getResources().getString(R.string.tab_album),App.getContext().getResources().getString(R.string.tab_artist),
            App.getContext().getResources().getString(R.string.tab_playlist),App.getContext().getResources().getString(R.string.tab_folder));
    public static final List<Category> DEFAULT_LIBRARY = Arrays.asList(
            new Category(App.getContext().getString(R.string.tab_song)),
            new Category(App.getContext().getString(R.string.tab_album)),
            new Category(App.getContext().getString(R.string.tab_artist)),
            new Category(App.getContext().getString(R.string.tab_playlist)),
            new Category(App.getContext().getString(R.string.tab_folder)));
}
