package remix.myplayer.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Remix on 2017/12/18.
 */

public class CustomThumb implements Parcelable {
    private int mId;
    private int mType;
    private String mkey;

    public CustomThumb(int id, int type, String key) {
        this.mId = id;
        this.mType = type;
        this.mkey = key;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
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
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.mId);
        dest.writeInt(this.mType);
        dest.writeString(this.mkey);
    }

    public CustomThumb(Parcel in) {
        this.mId = in.readInt();
        this.mType = in.readInt();
        this.mkey = in.readString();
    }

    public static final Parcelable.Creator<CustomThumb> CREATOR = new Parcelable.Creator<CustomThumb>() {
        @Override
        public CustomThumb createFromParcel(Parcel source) {
            return new CustomThumb(source);
        }

        @Override
        public CustomThumb[] newArray(int size) {
            return new CustomThumb[size];
        }
    };
}
