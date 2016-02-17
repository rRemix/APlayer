package remix.myplayer.infos;

import java.io.Serializable;

/**
 * Created by Remix on 2015/11/30.
 */
public class MP3Info implements Serializable,Cloneable {
    private long Id;
    private String Displayname;
    private String Album;
    private long AlbumId;
    private String Artist;
    private String AlbumArt;
    private long Duration;
    private String ReailTime;
    private String Url;
    private long Size;
    public MP3Info(){};
    public MP3Info(long id, String displayname, String album,long albumid, String artist, long duration,String reailTime, String url, long size,String albumart) {
        Id = id;
        Displayname = displayname;
        Album = album;
        AlbumId = albumid;
        Artist = artist;
        Duration = duration;
        ReailTime = reailTime;
        Url = url;
        Size = size;
        AlbumArt = albumart;
    }
    public MP3Info(MP3Info info)
    {
        this.Id = info.getId();
        this.Displayname = info.getDisplayname();
        this.Album = info.getAlbum();
        this.AlbumId = info.getAlbumId();
        this.Artist = info.getArtist();
        this.AlbumArt = info.getAlbumArt();
        this.Duration = info.getDuration();
        this.ReailTime = info.getReailTime();
        this.Url = info.getUrl();
        this.Size = info.getSize();
    }
    @Override
    public Object clone()
    {
        Object o=null;
        try
        {
            o = (MP3Info)super.clone();//Object 中的clone()识别出你要复制的是哪一个对象。
        }
        catch(CloneNotSupportedException e)
        {
            System.out.println(e.toString());
        }
        return o;
    }



    @Override
    public String toString()
    {
        return new String("Id = " + Id + " Name = " + Displayname + " Album = " + Album
                + " Artist = " + Artist + " Duration = " + Duration + " Realtime = " + ReailTime + " Url = " + Url + " Size = " + Size);
    }

    public long getAlbumId(){return AlbumId;}

    public void setAlbumId(long albumId){AlbumId = albumId;}

    public String getAlbumArt() {
        return AlbumArt;
    }

    public void setAlbumArt(String albumBitmap) {
        AlbumArt = albumBitmap;
    }

    public String getReailTime() {
        return ReailTime;
    }

    public void setReailTime(String reailTime) {
        ReailTime = reailTime;
    }

    public long getId() {
        return Id;
    }

    public void setId(long id) {
        Id = id;
    }

    public String getDisplayname() {
        return Displayname;
    }

    public void setDisplayname(String displayname) {
        Displayname = displayname;
    }

    public String getAlbum() {
        return Album;
    }

    public void setAlbum(String album) {
        Album = album;
    }

    public long getDuration() {
        return Duration;
    }

    public void setDuration(long duration) {
        Duration = duration;
    }

    public String getUrl() {
        return Url;
    }

    public void setUrl(String url) {
        Url = url;
    }

    public String getArtist() {
        return Artist;
    }

    public void setArtist(String artist) {
        Artist = artist;
    }

    public long getSize() {
        return Size;
    }

    public void setSize(long size) {
        Size = size;
    }


}
