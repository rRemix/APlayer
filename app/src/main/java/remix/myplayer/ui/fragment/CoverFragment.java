package remix.myplayer.ui.fragment;

import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.service.Command;
import remix.myplayer.theme.ThemeStore;
import remix.myplayer.ui.fragment.base.BaseMusicFragment;

/**
 * Created by Remix on 2015/12/2.
 */

/**
 * 专辑封面Fragment
 */
public class CoverFragment extends BaseMusicFragment {

  @BindView(R.id.cover_image)
  SimpleDraweeView mImage;
  @BindView(R.id.cover_container)
  View mCoverContainer;

  private int mWidth;
  private Uri mUri = Uri.EMPTY;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    mPageName = CoverFragment.class.getSimpleName();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    mWidth = getResources().getDisplayMetrics().widthPixels;
    View rootView = inflater.inflate(R.layout.fragment_cover, container, false);
    mUnBinder = ButterKnife.bind(this, rootView);

    mImage.getHierarchy().setFailureImage(ThemeStore.isLightTheme() ?
        R.drawable.album_empty_bg_day : R.drawable.album_empty_bg_night);

    return rootView;
  }


  /**
   * 操作为上一首歌曲时，显示往左侧消失的动画 下一首歌曲时，显示往右侧消失的动画
   *
   * @param info 需要更新的歌曲
   * @param withAnim 是否需要动画
   */
  public void updateCover(Song info, Uri uri, boolean withAnim) {
    if (!isAdded()) {
      return;
    }
    if (mImage == null || info == null) {
      return;
    }
    mUri = uri;
    if (withAnim) {
      int operation = MusicServiceRemote.getOperation();

      int offsetX = (mWidth + mImage.getWidth()) >> 1;
      final double startValue = 0;
      final double endValue = operation == Command.PREV ? offsetX : -offsetX;

      //封面移动动画
      final Spring outAnim = SpringSystem.create().createSpring();
      outAnim.addListener(new SimpleSpringListener() {
        @Override
        public void onSpringUpdate(Spring spring) {
          if (mCoverContainer == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) spring.getCurrentValue());
        }

        @Override
        public void onSpringAtRest(Spring spring) {
          //显示封面的动画
          if (mImage == null || spring == null) {
            return;
          }
          mCoverContainer.setTranslationX((float) startValue);
          setImageUriInternal();

          float endVal = 1;
          final Spring inAnim = SpringSystem.create().createSpring();
          inAnim.addListener(new SimpleSpringListener() {
            @Override
            public void onSpringUpdate(Spring spring) {
              if (mImage == null || spring == null) {
                return;
              }
              mCoverContainer.setScaleX((float) spring.getCurrentValue());
              mCoverContainer.setScaleY((float) spring.getCurrentValue());
            }

            @Override
            public void onSpringActivate(Spring spring) {

            }

          });
          inAnim.setCurrentValue(0.85);
          inAnim.setEndValue(endVal);
        }
      });
      outAnim.setOvershootClampingEnabled(true);
      outAnim.setCurrentValue(startValue);
      outAnim.setEndValue(endValue);
    } else {
      setImageUriInternal();
    }
  }

  private void setImageUriInternal() {
    mImage.setImageURI(mUri);
  }

}
