package remix.myplayer.bean.netease;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Remix on 2017/11/19.
 */

public class NSongSearchResponse implements Serializable {

  private static final long serialVersionUID = -649254748472577314L;
  public int code;
  public SearchActualData result;

  public static class SearchActualData implements Serializable {

    private static final long serialVersionUID = -8875856782646205962L;
    public int songCount;
    public List<SearchInnerData> songs;
  }

  public static class SearchInnerData implements Serializable {

    private static final long serialVersionUID = 835870151533020922L;
    public String name;
    public int id;
    public int position;
    public List<String> alias;
    public int status;
    public int fee;
    public String disc;
    public int copyrightId;
    public int no;
    public List<SearchArtistData> artists;
    public SearchAlbumData album;
    public String starred;
    public int popularity;
    public int score;
    public int starredNum;
    public long duration;
    public int playedNum;
    public int dayPlays;
    public int hearTime;
    public String ringtone;
  }

  public static class SearchArtistData implements Serializable {

    private static final long serialVersionUID = -871165113774382844L;
    public String name;
    public int id;
    public long picId;
    public int img1v1Id;
    public String briefDesc;
    public String picUrl;
    public String img1v1Url;
    public int albumSize;
    public List<String> alias;
    public String trans;
    public int musicSize;
  }

  public static class SearchAlbumData implements Serializable {

    private static final long serialVersionUID = -3427976565299765848L;
    public String name;
    public int id;
    public String type;
    public int size;
    public long picId;
    public String blurPicUrl;
    public int companyId;
    public long pic;
    public String picUrl;
    public long publishTime;
    public String description;
    public String tags;
    public String company;
    public String briefDesc;
    public SearchArtistData artist;
    public List<String> songs;
    public List<String> alias;
    public int status;
    public int copyrightId;
    public String commentThreadId;
    public List<SearchArtistData> artists;
  }
}
