package remix.myplayer.bean.qq

data class QSearchResponse(val data: SearchData) {
  data class SearchData(val song: SearchDataSong) {
    data class SearchDataSong(val list: List<SearchDataListItem>) {
      data class SearchDataListItem(val songmid: String,
                                    val songname: String)
    }
  }
}