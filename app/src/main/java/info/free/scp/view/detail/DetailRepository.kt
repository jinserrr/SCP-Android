package info.free.scp.view.detail

import androidx.lifecycle.LiveData
import info
import info.free.scp.SCPConstants
import info.free.scp.bean.ScpLikeBox
import info.free.scp.bean.ScpLikeModel
import info.free.scp.bean.ScpModel
import info.free.scp.db.AppInfoDatabase
import info.free.scp.db.ScpDataHelper
import info.free.scp.db.ScpDatabase


class DetailRepository {
    var scp: LiveData<out ScpModel>? = null
    var scpLikeInfo: LiveData<ScpLikeModel>? = null
    private var scpDao = ScpDatabase.getInstance()?.scpDao()
    private var likeDao = AppInfoDatabase.getInstance().likeAndReadDao()

    fun setScp(link: String) {
        scp = scpDao?.getLiveScpByLink(link) ?: scpDao?.getLiveCollectionByLink(link)
    }

    fun setScpReadInfo() {
        scp?.value?.let {
            ScpDataHelper.getInstance().insertViewListItem(it.link, it.title, SCPConstants.HISTORY_TYPE)
            AppInfoDatabase.getInstance().readRecordDao().delete(it.link, SCPConstants.LATER_TYPE)
            scpLikeInfo = likeDao.getLiveInfoByLink(it.link)
        }
    }

    fun setScpLikeInfo() {
        scp?.value?.let {
            if (scpLikeInfo?.value == null) {
                val likeInfo = ScpLikeModel(it.link, it.title, false, hasRead = false, boxId = 0)
                info("save new like info:${likeInfo}")
                likeDao.save(likeInfo)
//                scpLikeInfo = likeDao.getLiveInfoByLink(it.link)
            }
        }
    }
}