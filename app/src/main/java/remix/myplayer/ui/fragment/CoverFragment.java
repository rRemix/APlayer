package remix.myplayer.ui.fragment;

import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.controller.ControllerListener;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.image.ImageInfo;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.rebound.SimpleSpringListener;
import com.facebook.rebound.Spring;
import com.facebook.rebound.SpringSystem;
import remix.myplayer.R;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.helper.MusicServiceRemote;
import remix.myplayer.misc.interfaces.OnCoverFirstLoadFinishListener;
import remix.myplayer.misc.interfaces.OnInflateFinishListener;
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
  //    @BindView(R.id.cover_shadow)
//    ImageView mShadow;
  @BindView(R.id.cover_container)
  View mCoverContainer;

  private int mWidth;
  private Uri mUri = Uri.EMPTY;
//  private OnInflateFinishListener mInflateFinishListener;
//  private OnCoverFirstLoadFinishListener mFirstLoadFinishListener;

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

    mImage.getHierarchy().setFailureImage(ThemeStore.isLightTheme() ? R.drawable.album_empty_bg_day
        : R.drawable.album_empty_bg_night);
//    mImage.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//      @Override
//      public boolean onPreDraw() {
//        mImage.getViewTreeObserver().removeOnPreDrawListener(this);
//        if (mInflateFinishListener != null) {
//          mInflateFinishListener.onViewInflateFinish(mImage);
//        }
//        return true;
//      }
//    });

    return rootView;
  }

//  public void setInflateFinishListener(OnInflateFinishListener l) {
//    mInflateFinishListener = l;
//  }

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
//            mShadow.setVisibility(View.VISIBLE);
    }
  }

  public void setImageUriInternal() {
//        mShadow.setVisibility(View.INVISIBLE);
    mImage.setImageURI(mUri);
//    ImageRequestBuilder imageRequestBuilder = ImageRequestBuilder.newBuilderWithSource(mUri);
//    DraweeController controller = Fresco.newDraweeControllerBuilder()
//        .setImageRequest(imageRequestBuilder.build())
//        .setOldController(mImage.getController())
//        .setControllerListener(new ControllerListener<ImageInfo>() {
//          @Override
//          public void onSubmit(String id, Object callerContext) {
//
//          }
//
//          @Override
//          public void onFinalImageSet(String id, ImageInfo imageInfo, Animatable animatable) {
////            if (mFirstLoadFinishListener != null) {
////              mFirstLoadFinishListener.onCoverFirstLoadFinish();
////              mFirstLoadFinishListener = null;
////            }
//          }
//
//          @Override
//          public void onIntermediateImageSet(String id, ImageInfo imageInfo) {
//
//          }
//
//          @Override
//          public void onIntermediateImageFailed(String id, Throwable throwable) {
//
//          }
//
//          @Override
//          public void onFailure(String id, Throwable throwable) {
////            if (mFirstLoadFinishListener != null) {
////              mFirstLoadFinishListener.onCoverFirstLoadFinish();
////              mFirstLoadFinishListener = null;
////            }
//          }
//
//          @Override
//          public void onRelease(String id) {
//
//          }
//        })
//        .build();
//    mImage.setController(controller);
  }

  /**
   * activity退出时隐藏封面
   */
  public void hideImage() {
    if (mCoverContainer != null) {
      mCoverContainer.setVisibility(View.INVISIBLE);
    }
  }

  /**
   * activity入场动画播放完毕时显示封面
   */
  public void showImage() {
    if (mImage != null) {
      mImage.setVisibility(View.VISIBLE);
    }
    if (mCoverContainer != null) {
      mCoverContainer.setVisibility(View.VISIBLE);
    }
  }

//  public void setOnFirstLoadFinishListener(OnCoverFirstLoadFinishListener onFirstLoadFinishListener) {
//    mFirstLoadFinishListener = onFirstLoadFinishListener;
//  }
}
