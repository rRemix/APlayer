package remix.myplayer.bean.kugou

/**
 * Created by Remix on 2017/11/21.
 */

data class KSearchResponse(var info: String? = null,
                           val status: Int = 0,
                           val proposal: String? = null,
                           val keyword: String? = null,
                           val candidates: List<SearchActualData>) {
    data class SearchActualData(
            var soundname: String? = null,
            var krctype: Int = 0,
            var nickname: String? = null,
            var originame: String? = null,
            var accesskey: String? = null,
            var origiuid: String? = null,
            var score: Int = 0,
            var hitlayer: Int = 0,
            var duration: Int = 0,
            var sounduid: String? = null,
            var song: String? = null,
            var uid: String? = null,
            var transuid: String? = null,
            var transname: String? = null,
            var adjust: Int = 0,
            var id: Int = 0,
            var singer: String? = null,
            var language: String? = null
    )
}
