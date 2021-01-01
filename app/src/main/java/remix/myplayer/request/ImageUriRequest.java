package remix.myplayer.request;

import static remix.myplayer.request.LibraryUriRequest.ERROR_BLACKLIST;
import static remix.myplayer.request.LibraryUriRequest.ERROR_NO_RESULT;
import static remix.myplayer.request.UriRequest.TYPE_NETEASE_ALBUM;
import static remix.myplayer.request.UriRequest.TYPE_NETEASE_ARTIST;
import static remix.myplayer.request.UriRequest.TYPE_NETEASE_SONG;
import static remix.myplayer.util.Util.isWifi;

import android.content.ContentUris;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import com.facebook.common.executors.CallerThreadExecutor;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.google.gson.Gson;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import okhttp3.ResponseBody;
import remix.myplayer.App;
import remix.myplayer.R;
import remix.myplayer.bean.lastfm.LastFmAlbum;
import remix.myplayer.bean.lastfm.LastFmArtist;
import remix.myplayer.bean.mp3.Song;
import remix.myplayer.bean.netease.NAlbumSearchResponse;
import remix.myplayer.bean.netease.NArtistSearchResponse;
import remix.myplayer.bean.netease.NSongSearchResponse;
import remix.myplayer.misc.cache.DiskCache;
import remix.myplayer.request.network.HttpClient;
import remix.myplayer.util.DensityUtil;
import remix.myplayer.util.ImageUriUtil;
import remix.myplayer.util.MediaStoreUtil;
import remix.myplayer.util.SPUtil;

/**
 * Created by Remix on 2017/11/30.
 */

public abstract class ImageUriRequest<T> {

  private static final ConcurrentHashMap<Integer, String> MEMORY_CACHE = new ConcurrentHashMap<>();

  private static final List<String> BLACKLIST = Arrays
      .asList("https://lastfm-img2.akamaized.net/i/u/300x300/7c58a2e3b889af6f923669cc7744c3de.png",
          "https://lastfm-img2.akamaized.net/i/u/300x300/e1d60ddbcaaa6acdcbba960786f11360.png",
          "http://p1.music.126.net/l8KRlRa-YLNW0GOBeN6fIA==/17914342951434926.jpg",
          "http://p1.music.126.net/RCIIvR7ull5iQWN-awJ-Aw==/109951165555852156.jpg");

  private static final String PREFIX_FILE = "file://";
  private static final String PREFIX_EMBEDDED = "embedded://";

  public static final int BIG_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 170);
  public static final int SMALL_IMAGE_SIZE = DensityUtil.dip2px(App.getContext(), 45);
  public static final int URL_PLAYLIST = 1000;
  public static final int URL_ALBUM = 10;
  public static final int URL_ARTIST = 100;

  //自动下载封面
  public static String AUTO_DOWNLOAD_ALBUM = SPUtil
      .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.AUTO_DOWNLOAD_ALBUM_COVER,
          App.getContext().getString(R.string.always));
  //忽略内嵌
  public static boolean IGNORE_MEDIA_STORE = SPUtil
      .getValue(App.getContext(), SPUtil.SETTING_KEY.NAME,
          SPUtil.SETTING_KEY.IGNORE_MEDIA_STORE, false);

  //封面下载源
  public static final int DOWNLOAD_NETEASE = 1;
  public static final int DOWNLOAD_LASTFM = 0;
  public static int DOWNLOAD_SOURCE = SPUtil.getValue(App.getContext(), SPUtil.SETTING_KEY.NAME,
      SPUtil.SETTING_KEY.ALBUM_COVER_DOWNLOAD_SOURCE, DOWNLOAD_LASTFM);

  protected RequestConfig mConfig = DEFAULT_CONFIG;

  private static final RequestConfig DEFAULT_CONFIG = new RequestConfig.Builder()
      .forceDownload(true).build();

  public ImageUriRequest(RequestConfig config) {
    mConfig = config;
  }

  public ImageUriRequest() {
  }

  public static void clearUriCache() {
    MEMORY_CACHE.clear();
  }

  public abstract void onError(Throwable throwable);

  public abstract void onSuccess(@Nullable T result);

  protected void onStart() {

  }

  public abstract Disposable load();

  protected Observable<String> getCoverObservable(UriRequest request) {
    return Observable
        .concat(
            getMemoryCacheObservable(request),
            getCustomThumbObservable(request),
            getContentThumbObservable(request),
            getNetworkThumbObservable(request))
        .doOnNext(result -> {
          if (TextUtils.isEmpty(result)) {
            throw new Exception(ERROR_NO_RESULT);
          }
          if (ImageUriRequest.BLACKLIST.contains(result)) {
            throw new Exception(ERROR_BLACKLIST);
          }
        })
        .doOnError(throwable -> {
          if (throwable == null) {
            // 没有错误类型 忽略
          } else if (throwable instanceof UnknownHostException) {
            // 没有网络 忽略
          } else if (throwable instanceof NoSuchElementException) {
            // 没有结果不再查找
            MEMORY_CACHE.put(request.hashCode(), "");
          } else if (ERROR_NO_RESULT.equals(throwable.getMessage()) ||
              ERROR_BLACKLIST.equals(throwable.getMessage())) {
            // 黑名单或者没有结果 不再查找
            MEMORY_CACHE.put(request.hashCode(), "");
          } else {
            // 默认不处理
          }
        })
        .doOnNext(result -> {
          MEMORY_CACHE.put(request.hashCode(), result);
        })
        .doOnSubscribe(disposable -> onStart())
        .firstOrError()
        .toObservable();
  }

  private Observable<String> getMemoryCacheObservable(UriRequest request) {
    return Observable.create(emitter -> {
      final String cache = MEMORY_CACHE.get(request.hashCode());
      if (cache != null) {
        emitter.onNext(cache);
      }
      emitter.onComplete();
    });
  }


  Observable<String> getCustomThumbObservable(UriRequest request) {
    return Observable.create(emitter -> {
      //是否设置过自定义封面
      File customImage = ImageUriUtil.getCustomThumbIfExist(request.getId(), request.getSearchType());
      if (customImage != null && customImage.exists()) {
        emitter.onNext(PREFIX_FILE + customImage.getAbsolutePath());
      }
      emitter.onComplete();
    });
  }

  /**
   * 查询本地数据库
   */
  private Observable<String> getContentThumbObservable(UriRequest request) {
    return Observable.create(observer -> {
      String imageUrl = "";
      if (request.getSearchType() == URL_ALBUM) {//专辑封面
        //忽略多媒体缓存
        if (IGNORE_MEDIA_STORE) {
          final String selection = TextUtils.isEmpty(request.getTitle()) ?
              MediaStore.Audio.Media.ALBUM_ID + "=" + request.getId() :
              MediaStore.Audio.Media.ALBUM_ID + "=" + request.getId() + " and " +
                  MediaStore.Audio.Media.TITLE + "=?";
          final String[] selectionValues = TextUtils.isEmpty(request.getTitle()) ?
              null :
              new String[]{request.getTitle()};

          List<Song> songs = MediaStoreUtil.getSongs(selection, selectionValues);
          if (songs.size() > 0) {
//                        imageUrl = resolveEmbeddedPicture(songs.get(0));
            imageUrl = PREFIX_EMBEDDED + songs.get(0).getUrl();

          }
        } else {
          Uri uri = ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart/"), request.getId());
          if (ImageUriUtil.isAlbumThumbExistInMediaCache(uri)) {
            imageUrl = uri.toString();
          }
        }

      } else {//艺术家封面
        imageUrl = ImageUriUtil.getArtistArt(request.getId());
      }
      if (!TextUtils.isEmpty(imageUrl)) {
        observer.onNext(imageUrl);
      }
      observer.onComplete();
    });
  }

  /**
   * 内嵌封面
   */
  @Deprecated
  private String resolveEmbeddedPicture(Song song) throws IOException {
    if (song == null) {
      return "";
    }
    String imageUrl = null;
    String imageName = ((song.getArtist() + " - " + song.getTitle()) + ".jpg")
        .replaceAll("#", "")
        .replaceAll("\\?", "")
        .replaceAll("/", "");
    MediaMetadataRetriever retriever = null;
    try {
      File cacheDir = DiskCache.getDiskCacheDir(App.getContext(), "embedded/");
      File original = new File(cacheDir, imageName);
      if (original.exists()) {
        imageUrl = PREFIX_FILE + original.getAbsolutePath();
      } else {
        retriever = new MediaMetadataRetriever();
        retriever.setDataSource(song.getUrl());
        byte[] picture = retriever.getEmbeddedPicture();
        if (picture != null) {
          Bitmap bitmap = BitmapFactory.decodeByteArray(picture, 0, picture.length);
          //保存bitmap
          if (bitmap != null && (cacheDir.exists() || cacheDir.mkdirs())) {
            FileOutputStream fileOutputStream = new FileOutputStream(original);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);
            fileOutputStream.flush();
            fileOutputStream.close();
            imageUrl = PREFIX_FILE + original.getAbsolutePath();
          }
        } else {
          File cover = fallback(song);
          if (cover != null && cover.exists()) {
            imageUrl = PREFIX_FILE + cover.getAbsolutePath();
          }
        }
      }
    } finally {
      if (retriever != null) {
        retriever.release();
      }
    }
    return imageUrl;
  }

  private Observable<String> getNetworkThumbObservable(UriRequest request) {
    return DOWNLOAD_SOURCE == DOWNLOAD_LASTFM ? getLastFMNetworkThumbObservable(request)
        : getNeteaseNetworkThumbObservable(request);
  }

  //lastFM
  private Observable<String> getLastFMNetworkThumbObservable(UriRequest request) {
    return Observable.concat(Observable.create(new ObservableOnSubscribe<String>() {
      @Override
      public void subscribe(ObservableEmitter<String> emitter) throws Exception {
        String imageUrl = SPUtil
            .getValue(App.getContext(), SPUtil.COVER_KEY.NAME, request.getLastFMKey(), "");
        if (!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl)) && !BLACKLIST
            .contains(imageUrl)) {
          emitter.onNext(imageUrl);
        }
        emitter.onComplete();
      }
    }), Observable.just(isAutoDownloadCover() && !TextUtils.isEmpty(request.getLastFMKey()))
        .filter(aBoolean -> aBoolean)
        .flatMap(new Function<Boolean, ObservableSource<String>>() {
          private Observable<ResponseBody> getObservable(UriRequest request) {
            return request.getSearchType() == ImageUriRequest.URL_ALBUM ?
                HttpClient.getInstance()
                    .getAlbumInfo(request.getAlbumName(), request.getArtistName(), null) :
                HttpClient.getInstance().getArtistInfo(request.getArtistName(), null);
          }

          @Override
          public ObservableSource<String> apply(Boolean aBoolean) {
            return getObservable(request)
                .map(responseBody -> parseLastFMNetworkImageUrl(request, responseBody));
          }
        }).firstElement().toObservable());
  }

  private String parseLastFMNetworkImageUrl(UriRequest request, ResponseBody body)
      throws IOException {
    String imageUrl = null;
    String bodyString = body.string();
    if (request.getSearchType() == ImageUriRequest.URL_ALBUM) {
      LastFmAlbum lastFmAlbum = new Gson().fromJson(bodyString, LastFmAlbum.class);
      if (lastFmAlbum.getAlbum() != null) {
        imageUrl = ImageUriUtil.getLargestAlbumImageUrl(lastFmAlbum.getAlbum().getImage());
      }
    } else if (request.getSearchType() == ImageUriRequest.URL_ARTIST) {
      LastFmArtist lastFmArtist = new Gson().fromJson(bodyString, LastFmArtist.class);
      if (lastFmArtist.getArtist() != null) {
        imageUrl = ImageUriUtil.getLargestArtistImageUrl(lastFmArtist.getArtist().getImage());
      }
    }
    if (BLACKLIST.contains(imageUrl)) {
      imageUrl = null;
    }
    if (!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))) {
      SPUtil.putValue(App.getContext(), SPUtil.COVER_KEY.NAME, request.getLastFMKey(), imageUrl);
    }
    return imageUrl;
  }

  //网易
  private Observable<String> getNeteaseNetworkThumbObservable(UriRequest request) {
    return Observable.concat(Observable.create(new ObservableOnSubscribe<String>() {
      @Override
      public void subscribe(ObservableEmitter<String> emitter) throws Exception {
        String imageUrl = SPUtil
            .getValue(App.getContext(), SPUtil.COVER_KEY.NAME, request.getNeteaseCacheKey(), "");
        if (!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))) {
          emitter.onNext(imageUrl);
        }
        emitter.onComplete();
      }
    }), Observable.just(isAutoDownloadCover() && !TextUtils.isEmpty(request.getNeteaseSearchKey()))
        .filter(aBoolean -> aBoolean)
        .flatMap(aBoolean -> HttpClient.getInstance()
            .getNeteaseSearch(request.getNeteaseSearchKey(), 0, 1, request.getNeteaseType())
            .map(responseBody -> parseNeteaseNetworkImageUrl(request, responseBody))
            .firstElement().toObservable()));
  }

  @Nullable
  private String parseNeteaseNetworkImageUrl(UriRequest request, ResponseBody body)
      throws IOException {
    String imageUrl = "";
    if (request.getNeteaseType() == TYPE_NETEASE_SONG) {
      //搜索的是歌曲
      NSongSearchResponse response = new Gson().fromJson(body.string(), NSongSearchResponse.class);
      if (response.result.songs.get(0).score >= 60) {
        imageUrl = response.result.songs.get(0).album.picUrl;
      }
    } else if (request.getNeteaseType() == TYPE_NETEASE_ALBUM) {
      //搜索的是专辑
      NAlbumSearchResponse response = new Gson()
          .fromJson(body.string(), NAlbumSearchResponse.class);
      imageUrl = response.result.albums.get(0).picUrl;
    } else if (request.getNeteaseType() == TYPE_NETEASE_ARTIST) {
      //搜索的是艺术家
      NArtistSearchResponse response = new Gson()
          .fromJson(body.string(), NArtistSearchResponse.class);
      imageUrl = response.getResult().getArtists().get(0).getPicUrl();
    }
    if (!TextUtils.isEmpty(imageUrl) && UriUtil.isNetworkUri(Uri.parse(imageUrl))) {
      SPUtil.putValue(App.getContext(), SPUtil.COVER_KEY.NAME, request.getNeteaseCacheKey(),
          imageUrl);
    }
    return imageUrl;
  }

  protected Observable<Bitmap> getThumbBitmapObservable(final Uri uri){
    if(uri == null){
      return Observable.error(new Throwable("uri is null"));
    }

    return Observable.create(emitter -> {
      ImageRequest imageRequest =
          ImageRequestBuilder.newBuilderWithSource(uri)
              .setResizeOptions(new ResizeOptions(mConfig.getWidth(), mConfig.getHeight()))
              .build();
      DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline()
          .fetchDecodedImage(imageRequest, App.getContext());
      dataSource.subscribe(new BaseBitmapDataSubscriber() {
        @Override
        protected void onNewResultImpl(Bitmap bitmap) {
//                            Bitmap result = copy(bitmap);
          if (bitmap == null) {
            bitmap = BitmapFactory
                .decodeResource(App.getContext().getResources(), R.drawable.album_empty_bg_day);
          }
          emitter.onNext(bitmap);
          emitter.onComplete();
        }

        @Override
        protected void onFailureImpl(
            DataSource<CloseableReference<CloseableImage>> dataSource) {
          emitter.onError(dataSource.getFailureCause());
        }
      }, CallerThreadExecutor.getInstance());
    });
  }

  protected Observable<Bitmap> getThumbBitmapObservable(UriRequest request) {
    return getCoverObservable(request)
        .flatMap((Function<String, ObservableSource<Bitmap>>) url -> Observable.create(e -> {
          Uri imageUri = !TextUtils.isEmpty(url) ? Uri.parse(url) : Uri.EMPTY;
          ImageRequest imageRequest =
              ImageRequestBuilder.newBuilderWithSource(imageUri)
                  .setResizeOptions(new ResizeOptions(mConfig.getWidth(), mConfig.getHeight()))
                  .build();
          DataSource<CloseableReference<CloseableImage>> dataSource = Fresco.getImagePipeline()
              .fetchDecodedImage(imageRequest, App.getContext());
          dataSource.subscribe(new BaseBitmapDataSubscriber() {
            @Override
            protected void onNewResultImpl(Bitmap bitmap) {
//                            Bitmap result = copy(bitmap);
              if (bitmap == null) {
                bitmap = BitmapFactory
                    .decodeResource(App.getContext().getResources(), R.drawable.album_empty_bg_day);
              }
              e.onNext(bitmap);
              e.onComplete();
            }

            @Override
            protected void onFailureImpl(
                DataSource<CloseableReference<CloseableImage>> dataSource) {
              e.onError(dataSource.getFailureCause());
            }
          }, CallerThreadExecutor.getInstance());
        }));
  }

  /**
   * 是否下载封面
   */
  protected boolean isAutoDownloadCover() {
    Context context = App.getContext();
    return context.getString(R.string.always).equals(AUTO_DOWNLOAD_ALBUM) || (
        context.getString(R.string.wifi_only).equals(AUTO_DOWNLOAD_ALBUM) && isWifi(context));
  }


  private static final String[] FALLBACKS = {"cover.jpg", "album.jpg", "folder.jpg", "cover.png",
      "album.png", "folder.png"};

  private File fallback(Song song) {
    File parent = new File(song.getUrl()).getParentFile();

    File sameJPG = new File(parent, song.getArtist() + " - " + song.getTitle() + ".jpg");
    if (sameJPG.exists()) {
      return sameJPG;
    }

    File samePNG = new File(parent, song.getArtist() + " - " + song.getTitle() + ".png");
    if (samePNG.exists()) {
      return samePNG;
    }

    for (String fallback : FALLBACKS) {
      File cover = new File(parent, fallback);
      if (cover.exists()) {
        return cover;
      }
    }
    return null;
  }
}
