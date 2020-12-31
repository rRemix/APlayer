package remix.myplayer.ui.fragment;

import static remix.myplayer.misc.ExtKt.isPortraitOrientation;
import static remix.myplayer.util.ColorUtil.getColor;
import static remix.myplayer.util.ColorUtil.isColorLight;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import java.util.List;
import remix.myplayer.R;
import remix.myplayer.helper.MusicEventCallback;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.adapter.BaseAdapter;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;
import remix.myplayer.ui.misc.MultipleChoice;
import remix.myplayer.ui.widget.fastcroll_recyclerview.FastScrollRecyclerView;
import remix.myplayer.util.Constants;
import remix.myplayer.util.DensityUtil;

/**
 * Created by Remix on 2016/12/23.
 */

public abstract class LibraryFragment<D, A extends BaseAdapter> extends BaseMusicFragment implements
    MusicEventCallback, LoaderManager.LoaderCallbacks<List<D>> {

  protected A mAdapter;
  protected MultipleChoice<D> mChoice;

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (mHasPermission) {
      getLoaderManager().initLoader(getLoaderId(), null, this);
    }
  }

  public MultipleChoice getChoice() {
    return mChoice;
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    final View rootView = inflater.inflate(getLayoutID(), container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    int type = this instanceof SongFragment ? Constants.SONG :
        this instanceof AlbumFragment ? Constants.ALBUM :
            this instanceof ArtistFragment ? Constants.ARTIST :
                this instanceof PlayListFragment ? Constants.PLAYLIST :
                    Constants.FOLDER;
    mChoice = new MultipleChoice<>(requireActivity(), type);
    initAdapter();
    initView();

    //recyclerView的滚动条
    final int accentColor = ThemeStore.getAccentColor();
    final RecyclerView recyclerView = rootView.findViewById(R.id.recyclerView);
    if (recyclerView instanceof FastScrollRecyclerView) {
      ((FastScrollRecyclerView) recyclerView).setBubbleColor(accentColor);
      ((FastScrollRecyclerView) recyclerView).setHandleColor(accentColor);
      ((FastScrollRecyclerView) recyclerView)
          .setBubbleTextColor(getColor(isColorLight(accentColor) ?
              R.color.light_text_color_primary : R.color.dark_text_color_primary));
    }

    mChoice.setAdapter(mAdapter);
    return rootView;
  }


  protected abstract int getLayoutID();

  protected abstract void initAdapter();

  protected abstract void initView();

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

  @Override
  public void onMediaStoreChanged() {
    if (mHasPermission) {
      getLoaderManager().restartLoader(getLoaderId(), null, this);
    } else {
      if (mAdapter != null) {
        mAdapter.setData(null);
      }
    }
  }

  @Override
  public void onPermissionChanged(boolean has) {
    if (has != mHasPermission) {
      mHasPermission = has;
      onMediaStoreChanged();
    }
  }


  @Override
  public Loader<List<D>> onCreateLoader(int id, Bundle args) {
    return getLoader();
  }

  @Override
  public void onLoadFinished(Loader<List<D>> loader, List<D> data) {
    mAdapter.setData(data);
  }

  @Override
  public void onLoaderReset(Loader<List<D>> loader) {
    if (mAdapter != null) {
      mAdapter.setData(null);
    }
  }

  private static final int PORTRAIT_ORIENTATION_COUNT = 2;
  private static final int LANDSCAPE_ORIENTATION_ITEM_WIDTH = DensityUtil.dip2px(180);
  private static final int PORTRAIT_ORIENTATION_MAX_ITEM_COUNT = 6;

  protected int getSpanCount() {
    final boolean portraitOrientation = isPortraitOrientation(requireContext());
    if (portraitOrientation) {
      return PORTRAIT_ORIENTATION_COUNT;
    } else {
      int count = getResources().getDisplayMetrics().widthPixels / LANDSCAPE_ORIENTATION_ITEM_WIDTH;
      return count > PORTRAIT_ORIENTATION_MAX_ITEM_COUNT ? PORTRAIT_ORIENTATION_MAX_ITEM_COUNT : count;
    }
  }

  protected abstract Loader<List<D>> getLoader();

  protected abstract int getLoaderId();

}
