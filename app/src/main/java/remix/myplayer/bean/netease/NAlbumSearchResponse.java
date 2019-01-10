package remix.myplayer.bean.netease;

import java.io.Serializable;
import java.util.List;

/**
 * Created by Remix on 2017/11/30.
 */

public class NAlbumSearchResponse implements Serializable {

  private static final long serialVersionUID = 3087250502584503377L;
  public ResultBean result;
  public int code;

  public static class ResultBean implements Serializable {

    private static final long serialVersionUID = -966951494042024200L;
    public int albumCount;
    public List<AlbumsBean> albums;

    public static class AlbumsBean {

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
      public int status;
      public int copyrightId;
      public String commentThreadId;
      public String picId_str;

    }
  }
}
