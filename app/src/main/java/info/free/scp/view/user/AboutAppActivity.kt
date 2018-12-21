package info.free.scp.view.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import info.free.scp.BuildConfig
import info.free.scp.R
import info.free.scp.util.EventUtil
import info.free.scp.util.PreferenceUtil
import info.free.scp.util.Toaster
import info.free.scp.view.base.BaseActivity
import kotlinx.android.synthetic.main.activity_about_app.*

class AboutAppActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventUtil.onEvent(this, EventUtil.clickAboutApp)
        setContentView(R.layout.activity_about_app)

        tv_version?.text = "version: ${BuildConfig.VERSION_NAME}"
        tv_data_update_time?.text = "本地数据上次更新时间：${PreferenceUtil.getLastUpdateDbTime()}"
//        tv_server_data_latest_time?.text = "云端最新数据更新时间：${PreferenceUtil.getServerLastUpdateTime()}"

        tv_qq_group?.setOnLongClickListener {
            val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
            val clipData = ClipData.newPlainText("qqGroup", "805194504")
            clipboardManager?.primaryClip = clipData
            Toaster.show("已复制到剪贴板")
            true
        }
    }
}
