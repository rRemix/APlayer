package remix.myplayer.bean;

import android.os.Parcel;
import android.os.Parcelable;

public class AnimationUrl implements Parcelable {
    private int mAlbumId;
    private String mUrl;

    public int getAlbumId() {
        return mAlbumId;
    }

    public void setAlbumId(int albumId) {
        this.mAlbumId = albumId;
    }

    public String getUrl() {
        return mUrl;
    }

    public void setUrl(String url) {
        this.mUrl = url;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mAlbumId);
        dest.writeString(this.mUrl);
    }

    public AnimationUrl() {
    }

    protected AnimationUrl(Parcel in) {
        this.mAlbumId = in.readInt();
        this.mUrl = in.readString();
    }

    public static final Creator<AnimationUrl> CREATOR = new Creator<AnimationUrl>() {
        @Override
        public AnimationUrl createFromParcel(Parcel source) {
            return new AnimationUrl(source);
        }

        @Override
        public AnimationUrl[] newArray(int size) {
            return new AnimationUrl[size];
        }
    };
}
