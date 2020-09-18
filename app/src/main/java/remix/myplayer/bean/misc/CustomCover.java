package remix.myplayer.bean.misc;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Remix on 2017/12/18.
 */

public class CustomCover implements Parcelable {

  private long mId;
  private int mType;
  private String mkey;

  public CustomCover(long id, int type, String key) {
    this.mId = id;
    this.mType = type;
    this.mkey = key;
  }

  public long getId() {
    return mId;
  }

  public void setId(long id) {
    this.mId = id;
  }

  public int getType() {
    return mType;
  }

  public void setType(int type) {
    this.mType = type;
  }

  public String getkey() {
    return mkey;
  }

  public void setkey(String key) {
    this.mkey = key;
  }

  @Override
  public String toString() {
    return "CustomCover{" +
        "mId=" + mId +
        ", mType=" + mType +
        ", mkey='" + mkey + '\'' +
        '}';
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeLong(this.mId);
    dest.writeInt(this.mType);
    dest.writeString(this.mkey);
  }

  public CustomCover(Parcel in) {
    this.mId = in.readInt();
    this.mType = in.readInt();
    this.mkey = in.readString();
  }

  public static final Parcelable.Creator<CustomCover> CREATOR = new Parcelable.Creator<CustomCover>() {
    @Override
    public CustomCover createFromParcel(Parcel source) {
      return new CustomCover(source);
    }

    @Override
    public CustomCover[] newArray(int size) {
      return new CustomCover[size];
    }
  };
}
