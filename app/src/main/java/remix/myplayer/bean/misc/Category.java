package remix.myplayer.bean.misc;

import android.content.Context;
import android.text.TextUtils;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.ui.fragment.AlbumFragment;
import remix.myplayer.ui.fragment.ArtistFragment;
import remix.myplayer.ui.fragment.FolderFragment;
import remix.myplayer.ui.fragment.PlayListFragment;
import remix.myplayer.ui.fragment.SongFragment;

/**
 * Created by Remix on 2018/1/10.
 */

public class Category implements Serializable {

  private static final long serialVersionUID = -6799150022891213071L;
  private String mClassName;
  private int mOrder;
  private int mTag;

  public Category(int tag) {
    mTag = tag;
    mClassName = getClassName();
    mOrder = mTag;
  }

  public boolean isPlayList() {
    if (TextUtils.isEmpty(mClassName)) {
      mClassName = mTag == TAG_SONG ? SongFragment.class.getName() :
          mTag == TAG_ALBUM ? AlbumFragment.class.getName() :
              mTag == TAG_ARTIST ? ArtistFragment.class.getName() :
                  mTag == TAG_PLAYLIST ? PlayListFragment.class.getName()
                      : FolderFragment.class.getName();
    }
    return mClassName.equals(PlayListFragment.class.getName());
  }

  public boolean isSongList() {
    return getClassName().equals(SongFragment.class.getName());
  }

  public String getClassName() {
    return mTag == TAG_SONG ? SongFragment.class.getName() :
        mTag == TAG_ALBUM ? AlbumFragment.class.getName() :
            mTag == TAG_ARTIST ? ArtistFragment.class.getName() :
                mTag == TAG_PLAYLIST ? PlayListFragment.class.getName()
                    : FolderFragment.class.getName();
  }

  public int getTag() {
    return mTag;
  }

  public void setTag(int tag) {
    this.mTag = tag;
  }


  public int getOrder() {
    return mOrder;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || !(o == null || getClass() != o.getClass());
  }

  @Override
  public String toString() {
    return "Category{" +
        "mClassName='" + mClassName + '\'' +
        ", mOrder=" + mOrder +
        ", mTag=" + mTag +
        '}';
  }

  public static final int TAG_SONG = 0;
  public static final int TAG_ALBUM = 1;
  public static final int TAG_ARTIST = 2;
  public static final int TAG_PLAYLIST = 3;
  public static final int TAG_FOLDER = 4;


  public static List<String> getAllLibraryString(Context context){
    return Arrays
        .asList(context.getResources().getString(R.string.tab_song),
            context.getResources().getString(R.string.tab_album),
            context.getResources().getString(R.string.tab_artist),
            context.getResources().getString(R.string.tab_playlist),
            context.getResources().getString(R.string.tab_folder));
  }
  public static List<Category> getDefaultLibrary(Context context){
    return Arrays.asList(
        new Category(TAG_SONG),
        new Category(TAG_ALBUM),
        new Category(TAG_ARTIST),
        new Category(TAG_PLAYLIST),
        new Category(TAG_FOLDER));
  }

  public CharSequence getTitle() {
    return App.getContext().getString(mTag == TAG_SONG ? R.string.tab_song :
        mTag == TAG_ALBUM ? R.string.tab_album :
        mTag == TAG_ARTIST ? R.string.tab_artist :
        mTag == TAG_PLAYLIST ? R.string.tab_playlist : R.string.tab_folder);
  }
}
