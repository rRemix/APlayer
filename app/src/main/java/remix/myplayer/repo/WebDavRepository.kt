package remix.myplayer.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import remix.myplayer.data.db.room.dao.WebDavDao
import remix.myplayer.data.db.room.entity.WebDav
import javax.inject.Inject

interface WebDavRepository {
  fun allWebDav(): Flow<List<WebDav>>

  suspend fun insertOrReplace(webDav: WebDav): Long

  suspend fun delete(webDav: WebDav): Int
}

class WebDavRepoImpl @Inject constructor(
  @param:ApplicationContext
  private val context: Context,
  private val webDavDao: WebDavDao
) : WebDavRepository {

  override fun allWebDav(): Flow<List<WebDav>> = webDavDao.selectAll()

  override suspend fun insertOrReplace(webDav: WebDav) = webDavDao.insertOrReplace(webDav)

  override suspend fun delete(webDav: WebDav) = webDavDao.delete(webDav)

}
