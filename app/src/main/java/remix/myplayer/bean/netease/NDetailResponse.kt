package remix.myplayer.bean.netease

data class NDetailResponse(val code: Int = 0,
                           val songs: List<SongDetailData>? = null) {

    data class SongDetailData(val id: Int,
                              val album: SearchAlbumData? = null) {

            data class SearchAlbumData(val picUrl: String? = null)
    }
}
