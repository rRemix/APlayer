package remix.myplayer.helper

import io.reactivex.Observable
import io.reactivex.functions.Predicate
import remix.myplayer.APlayerApplication
import remix.myplayer.util.ToastUtil
import java.io.File

object M3UParser {
    private val TAG = "M3UParser"

    fun parse(path: String) : Unit{
        Observable.just(File(path)).filter(Predicate {
                    return@Predicate it.isFile && it.canRead()
                })
                .map {
                    val count = 0
                    count
                }
                .subscribe({
                    ToastUtil.show(APlayerApplication.getContext(), "导入($it)首歌曲")
                }, {
                    ToastUtil.show(APlayerApplication.getContext(), "导入失败: $it")
                })

    }
}