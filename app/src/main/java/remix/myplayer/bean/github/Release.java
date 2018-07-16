package remix.myplayer.bean.github;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

public class Release implements Parcelable {

    private String url;
    private String assets_url;
    private String upload_url;
    private String html_url;
    private int id;
    private String node_id;
    private String tag_name;
    private String target_commitish;
    private String name;
    private boolean draft;
//    private AuthorBean author;
    private boolean prerelease;
    private String created_at;
    private String published_at;
    private String tarball_url;
    private String zipball_url;
    private String body;
    private ArrayList<AssetsBean> assets;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAssets_url() {
        return assets_url;
    }

    public void setAssets_url(String assets_url) {
        this.assets_url = assets_url;
    }

    public String getUpload_url() {
        return upload_url;
    }

    public void setUpload_url(String upload_url) {
        this.upload_url = upload_url;
    }

    public String getHtml_url() {
        return html_url;
    }

    public void setHtml_url(String html_url) {
        this.html_url = html_url;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNode_id() {
        return node_id;
    }

    public void setNode_id(String node_id) {
        this.node_id = node_id;
    }

    public String getTag_name() {
        return tag_name;
    }

    public void setTag_name(String tag_name) {
        this.tag_name = tag_name;
    }

    public String getTarget_commitish() {
        return target_commitish;
    }

    public void setTarget_commitish(String target_commitish) {
        this.target_commitish = target_commitish;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    public boolean isPrerelease() {
        return prerelease;
    }

    public void setPrerelease(boolean prerelease) {
        this.prerelease = prerelease;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public String getPublished_at() {
        return published_at;
    }

    public void setPublished_at(String published_at) {
        this.published_at = published_at;
    }

    public String getTarball_url() {
        return tarball_url;
    }

    public void setTarball_url(String tarball_url) {
        this.tarball_url = tarball_url;
    }

    public String getZipball_url() {
        return zipball_url;
    }

    public void setZipball_url(String zipball_url) {
        this.zipball_url = zipball_url;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public ArrayList<AssetsBean> getAssets() {
        return assets;
    }

    public void setAssets(ArrayList<AssetsBean> assets) {
        this.assets = assets;
    }

    public static class AssetsBean implements Parcelable {
        private String url;
        private int id;
        private String node_id;
        private String name;
        private String label;
//        private UploaderBean uploader;
        private String content_type;
        private String state;
        private long size;
        private int download_count;
        private String created_at;
        private String updated_at;
        private String browser_download_url;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }

        public String getNode_id() {
            return node_id;
        }

        public void setNode_id(String node_id) {
            this.node_id = node_id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public String getContent_type() {
            return content_type;
        }

        public void setContent_type(String content_type) {
            this.content_type = content_type;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public long getSize() {
            return size;
        }

        public void setSize(int size) {
            this.size = size;
        }

        public int getDownload_count() {
            return download_count;
        }

        public void setDownload_count(int download_count) {
            this.download_count = download_count;
        }

        public String getCreated_at() {
            return created_at;
        }

        public void setCreated_at(String created_at) {
            this.created_at = created_at;
        }

        public String getUpdated_at() {
            return updated_at;
        }

        public void setUpdated_at(String updated_at) {
            this.updated_at = updated_at;
        }

        public String getBrowser_download_url() {
            return browser_download_url;
        }

        public void setBrowser_download_url(String browser_download_url) {
            this.browser_download_url = browser_download_url;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(this.url);
            dest.writeInt(this.id);
            dest.writeString(this.node_id);
            dest.writeString(this.name);
            dest.writeString(this.label);
            dest.writeString(this.content_type);
            dest.writeString(this.state);
            dest.writeLong(this.size);
            dest.writeInt(this.download_count);
            dest.writeString(this.created_at);
            dest.writeString(this.updated_at);
            dest.writeString(this.browser_download_url);
        }

        public AssetsBean() {
        }

        protected AssetsBean(Parcel in) {
            this.url = in.readString();
            this.id = in.readInt();
            this.node_id = in.readString();
            this.name = in.readString();
            this.label = in.readString();
            this.content_type = in.readString();
            this.state = in.readString();
            this.size = in.readLong();
            this.download_count = in.readInt();
            this.created_at = in.readString();
            this.updated_at = in.readString();
            this.browser_download_url = in.readString();
        }

        public static final Creator<AssetsBean> CREATOR = new Creator<AssetsBean>() {
            @Override
            public AssetsBean createFromParcel(Parcel source) {
                return new AssetsBean(source);
            }

            @Override
            public AssetsBean[] newArray(int size) {
                return new AssetsBean[size];
            }
        };
    }

    public Release() {
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.url);
        dest.writeString(this.assets_url);
        dest.writeString(this.upload_url);
        dest.writeString(this.html_url);
        dest.writeInt(this.id);
        dest.writeString(this.node_id);
        dest.writeString(this.tag_name);
        dest.writeString(this.target_commitish);
        dest.writeString(this.name);
        dest.writeByte(this.draft ? (byte) 1 : (byte) 0);
        dest.writeByte(this.prerelease ? (byte) 1 : (byte) 0);
        dest.writeString(this.created_at);
        dest.writeString(this.published_at);
        dest.writeString(this.tarball_url);
        dest.writeString(this.zipball_url);
        dest.writeString(this.body);
        dest.writeTypedList(this.assets);
    }

    protected Release(Parcel in) {
        this.url = in.readString();
        this.assets_url = in.readString();
        this.upload_url = in.readString();
        this.html_url = in.readString();
        this.id = in.readInt();
        this.node_id = in.readString();
        this.tag_name = in.readString();
        this.target_commitish = in.readString();
        this.name = in.readString();
        this.draft = in.readByte() != 0;
        this.prerelease = in.readByte() != 0;
        this.created_at = in.readString();
        this.published_at = in.readString();
        this.tarball_url = in.readString();
        this.zipball_url = in.readString();
        this.body = in.readString();
        this.assets = in.createTypedArrayList(AssetsBean.CREATOR);
    }

    public static final Creator<Release> CREATOR = new Creator<Release>() {
        @Override
        public Release createFromParcel(Parcel source) {
            return new Release(source);
        }

        @Override
        public Release[] newArray(int size) {
            return new Release[size];
        }
    };
}
