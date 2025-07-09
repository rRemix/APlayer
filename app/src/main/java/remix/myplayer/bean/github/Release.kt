package remix.myplayer.bean.github

import java.io.Serializable
import java.util.*

data class Release(var url: String?,
                   var assets_url: String?,
                   var upload_url: String?,
                   var html_url: String?,
                   var id: Int = 0,
                   var node_id: String?,
                   var tag_name: String?,
                   var target_commitish: String?,
                   var name: String?,
                   var isDraft: Boolean = false,
                   var isPrerelease: Boolean = false,
                   var created_at: String?,
                   var published_at: String?,
                   var tarball_url: String,
                   var zipball_url: String?,
                   var body: String?,
                   var assets: ArrayList<AssetsBean>?) : Serializable{

  data class AssetsBean(var url: String?,
                        var id: Int = 0,
                        var node_id: String?,
                        var name: String?,
                        var label: String?,
                        var content_type: String?,
                        var state: String?,
                        var size: Long = 0,
                        var download_count: Int = 0,
                        var created_at: String?,
                        var updated_at: String?,
                        var browser_download_url: String?) : Serializable

}

fun Release.isForce(): Boolean {
  val split = name?.split("-")
  return split != null && split.size > 3
}
