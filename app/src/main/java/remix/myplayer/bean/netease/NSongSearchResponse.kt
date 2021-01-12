package remix.myplayer.bean.netease

data class NSongSearchResponse(val code: Int = 0,
                               val result: SearchActualData? = null) {

  data class SearchActualData(val songs: List<SearchInnerData?>? = null) {

    data class SearchInnerData(val id: Int,
                               val album: SearchAlbumData? = null,
                               val score: Int) {

      data class SearchAlbumData(val picUrl: String? = null)
    }
  }

}