package remix.myplayer.bean.netease

data class NArtistSearchResponse(val result: ResultBean? = null,
                                 val code: Int) {
  data class ResultBean(var artistCount: Int,
                        val artists: List<ArtistsBean>? = null) {

    data class ArtistsBean(val picUrl: String? = null)
  }
}