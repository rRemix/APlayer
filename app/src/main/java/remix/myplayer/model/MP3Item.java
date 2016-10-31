package remix.myplayer.model;

import java.io.Serializable;

/**
 * Created by Remix on 2015/11/30.
 */

/**
 * 歌曲信息
 */
public class MP3Item implements Serializable,Cloneable {
    public int Id;
    public String Title;
    public String Displayname;
    public String Album;
    public int AlbumId;
    public String Artist;
    public String AlbumArt;
    public long Duration;
    public String ReailTime;
    public String Url;
    public long Size;
    public String Year;
    public MP3Item(){}

    public MP3Item(int id, String displayname, String title, String album, int albumid, String artist, long duration, String reailTime, String url, long size, String albumart) {
        this(id,displayname,title,album,albumid,artist,duration,reailTime,url,size,albumart,null);
    }

    public MP3Item(int id, String displayname, String title, String album, int albumid, String artist, long duration, String reailTime, String url, long size, String albumart,String year){
        Id = id;
        Title = title;
        Displayname = displayname;
        Album = album;
        AlbumId = albumid;
        Artist = artist;
        Duration = duration;
        ReailTime = reailTime;
        Url = url;
        Size = size;
        AlbumArt = albumart;
        Year = year;
    }

    public MP3Item(MP3Item info) {
        this.Id = info.getId();
        this.Title = info.getTitle();
        this.Displayname = info.getDisplayname();
        this.Album = info.getAlbum();
        this.AlbumId = info.getAlbumId();
        this.Artist = info.getArtist();
        this.AlbumArt = info.getAlbumArt();
        this.Duration = info.getDuration();
        this.ReailTime = info.getReailTime();
        this.Url = info.getUrl();
        this.Size = info.getSize();
        this.Year = info.getYear();
    }

    @Override
    public Object clone() {
        Object o=null;
        try {
            o = super.clone();//Object 中的clone()识别出你要复制的是哪一个对象。
        } catch(CloneNotSupportedException e) {
            System.out.println(e.toString());
        }
        return o;
    }

    @Override
    public String toString() {
        return "MP3Item{" +
                "Id=" + Id +
                ", Title='" + Title + '\'' +
                ", Displayname='" + Displayname + '\'' +
                ", Album='" + Album + '\'' +
                ", AlbumId=" + AlbumId +
                ", Artist='" + Artist + '\'' +
                ", AlbumArt='" + AlbumArt + '\'' +
                ", Duration=" + Duration +
                ", ReailTime='" + ReailTime + '\'' +
                ", Url='" + Url + '\'' +
                ", Size=" + Size +
                ", Year=" + Year +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        MP3Item temp = (MP3Item)o;
        return temp.getId() == this.getId();
    }


    public void setYear(String year){
        Year = year;
    }

    public String getYear(){
        return Year;
    }

    public void setTitle(String title){
        Title = title;
    }

    public String getTitle(){
        return Title;
    }

    public int getAlbumId(){return AlbumId;}

    public void setAlbumId(int albumId){AlbumId = albumId;}

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

    public int getId() {
        return Id;
    }

    public void setId(int id) {
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
