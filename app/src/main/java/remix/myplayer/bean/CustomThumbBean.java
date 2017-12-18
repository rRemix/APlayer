package remix.myplayer.bean;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Remix on 2017/12/18.
 */

public class CustomThumbBean implements Parcelable {
    private int mId;
    private int mType;
    private String mkey;

    public CustomThumbBean(int id, int type, String key) {
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

    public CustomThumbBean(Parcel in) {
        this.mId = in.readInt();
        this.mType = in.readInt();
        this.mkey = in.readString();
    }

    public static final Parcelable.Creator<CustomThumbBean> CREATOR = new Parcelable.Creator<CustomThumbBean>() {
        @Override
        public CustomThumbBean createFromParcel(Parcel source) {
            return new CustomThumbBean(source);
        }

        @Override
        public CustomThumbBean[] newArray(int size) {
            return new CustomThumbBean[size];
        }
    };
}
