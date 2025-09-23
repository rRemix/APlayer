package remix.myplayer.compose.repo

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import remix.myplayer.db.room.dao.WebDavDao
import remix.myplayer.db.room.model.WebDav
import javax.inject.Inject

interface WebDavRepository {
  fun getAll(): Flow<List<WebDav>>

  suspend fun insertOrReplace(webDav: WebDav): Long

  suspend fun delete(webDav: WebDav): Int
}

class WebDavRepoImpl @Inject constructor(
  @ApplicationContext
  private val context: Context,
  private val webDavDao: WebDavDao
) : WebDavRepository {

  override fun getAll(): Flow<List<WebDav>> = webDavDao.queryAll()

  override suspend fun insertOrReplace(webDav: WebDav) = webDavDao.insertOrReplace(webDav)

  override suspend fun delete(webDav: WebDav) = webDavDao.delete(webDav)

}