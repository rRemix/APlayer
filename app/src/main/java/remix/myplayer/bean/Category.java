package remix.myplayer.bean;

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
    private String mTitle;
    private int mIndex;

    public Category(String title) {

        this.mTitle = title;
        mIndex = APlayerApplication.getContext().getString(R.string.tab_song).equals(mTitle) ? 0 :
                APlayerApplication.getContext().getString(R.string.tab_album).equals(mTitle) ? 1 :
                APlayerApplication.getContext().getString(R.string.tab_artist).equals(mTitle) ? 2 :
                APlayerApplication.getContext().getString(R.string.tab_playlist).equals(mTitle) ? 3 : 4;
    }

    public String getTitle(){
        return mTitle;
    }

    public int getIndex(){
        return mIndex;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && mTitle.equals(((Category)o).getTitle());
    }

    public static final List<String> ALL_LIBRARY_STRING = Arrays.asList(APlayerApplication.getContext().getResources().getString(R.string.tab_song),
            APlayerApplication.getContext().getResources().getString(R.string.tab_album),APlayerApplication.getContext().getResources().getString(R.string.tab_artist),
            APlayerApplication.getContext().getResources().getString(R.string.tab_playlist),APlayerApplication.getContext().getResources().getString(R.string.tab_folder));
    public static final List<Category> DEFAULT_LIBRARY = Arrays.asList(
            new Category(APlayerApplication.getContext().getString(R.string.tab_song)),
            new Category(APlayerApplication.getContext().getString(R.string.tab_album)),
            new Category(APlayerApplication.getContext().getString(R.string.tab_artist)),
            new Category(APlayerApplication.getContext().getString(R.string.tab_playlist)),
            new Category(APlayerApplication.getContext().getString(R.string.tab_folder)));
}
