package remix.myplayer.request;

import androidx.annotation.IntRange;

/**
 * Created by Remix on 2017/12/1.
 */

public class RequestConfig {

  private int mWidth;
  private int mHeight;
  private boolean mResize;
  private boolean mForceDownload = true;

  public int getWidth() {
    return mWidth;
  }

  public void setWidth(int width) {
    this.mWidth = width;
  }

  public int getHeight() {
    return mHeight;
  }

  public void setHeight(int height) {
    this.mHeight = height;
  }

  public boolean isResize() {
    return mResize;
  }

  public void setResize(boolean resize) {
    this.mResize = resize;
  }

  public boolean isForceDownload() {
    return mForceDownload;
  }

  public void setForceDownload(boolean forceDownload) {
    this.mForceDownload = forceDownload;
  }

  private RequestConfig(Builder builder) {
    mWidth = builder.mWidth;
    mHeight = builder.mHeight;
    mResize = builder.mResize;
    mForceDownload = builder.mForceDownload;
  }

  public static class Builder {

    private int mWidth;
    private int mHeight;
    private boolean mResize;
    private boolean mForceDownload;

    public Builder() {
      mResize = false;
    }

    public Builder(@IntRange(from = 1, to = Integer.MAX_VALUE) int width,
        @IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
      mWidth = width;
      mHeight = height;
      mResize = true;
    }

    public RequestConfig build() {
      return new RequestConfig(this);
    }

    public Builder forceDownload(boolean forceDownload) {
      mForceDownload = forceDownload;
      return this;
    }

    public Builder resize(boolean resize) {
      mResize = resize;
      return this;
    }

    public Builder width(@IntRange(from = 1, to = Integer.MAX_VALUE) int width) {
      mWidth = width;
      return this;
    }

    public Builder height(@IntRange(from = 1, to = Integer.MAX_VALUE) int height) {
      mHeight = height;
      return this;
    }
  }
}
