package remix.myplayer.bean.mp3

/**
 * @ClassName
 * @Description
 * @Author Xiaoborui
 * @Date 2016/10/13 11:21
 */
class PlayListSong {

  var _Id: Int = 0
  var AudioId: Int = 0
  //    public int AlbumID;
  //    public String Album;
  //    public int ArtistID;
  //    public String Artist;
  //    public String Data;
  var PlayListID: Int = 0
  var PlayListName: String = ""

  constructor() {}

  constructor(audioId: Int, playListID: Int, playListName: String) {
    AudioId = audioId
    PlayListID = playListID
    PlayListName = playListName
  }
}
