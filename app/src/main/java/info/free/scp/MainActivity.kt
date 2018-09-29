package info.free.scp

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.util.Log
import info.free.scp.service.HttpManager
import info.free.scp.service.InitDataService
import info.free.scp.util.PreferenceUtil
import info.free.scp.view.base.BaseActivity
import info.free.scp.view.base.BaseFragment
import info.free.scp.view.category.CategoryActivity
import info.free.scp.view.feed.FeedFragment
import info.free.scp.view.home.HomeFragment
import kotlinx.android.synthetic.main.activity_main.*
import android.support.v4.content.LocalBroadcastManager
import android.content.IntentFilter


class MainActivity : BaseActivity(), HomeFragment.CategoryListener {
    private var currentFragment: BaseFragment? = null
    private val homeFragment = HomeFragment.newInstance()
    private val feedFragment = FeedFragment.newInstance()
    private val aboutFragment = AboutFragment.newInstance()
    private var updateChecked = false

    var progressDialog: ProgressDialog? = null

    private var mLocalBroadcastManager: LocalBroadcastManager? = null
    private var mBroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context?, intent: Intent?) {
            val progress = intent?.getIntExtra("progress", 0) ?: 0
            progressDialog?.progress = progress
        }
    }

    val INIT_PROGRESS = "initProgress"

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        val transaction = fragmentManager.beginTransaction()
        transaction?.hide(currentFragment)
        when (item.itemId) {
            R.id.navigation_home -> {
                if (homeFragment.isAdded) {
                    transaction.show(homeFragment)
                } else {
                    transaction.add(R.id.flMainContainer, homeFragment)
                }
                currentFragment = homeFragment
            }
//            R.id.navigation_feed -> {
//                if (feedFragment.isAdded) {
//                    transaction.show(feedFragment)
//                } else {
//                    transaction.add(R.id.flMainContainer, feedFragment)
//                }
//                currentFragment = feedFragment
//                return@OnNavigationItemSelectedListener true
//            }
            R.id.navigation_about -> {
                if (aboutFragment.isAdded) {
                    transaction.show(aboutFragment)
                } else {
                    transaction.add(R.id.flMainContainer, aboutFragment)
                }
                currentFragment = aboutFragment
            }
        }
        transaction?.commitAllowingStateLoss()
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this)
        val intentFilter = IntentFilter()
        intentFilter.addAction(INIT_PROGRESS)
        mLocalBroadcastManager?.registerReceiver(mBroadcastReceiver, intentFilter)
        // 启动数据加载service
        if (!PreferenceUtil.getInitDataFinish()) {
            val intent = Intent(this, InitDataService::class.java)
            startService(intent)
            progressDialog = ProgressDialog(this)
            progressDialog?.max = 100
            progressDialog?.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
            progressDialog?.setMessage("数据初始化中")
            progressDialog?.show()
        }
        setContentView(R.layout.activity_main)
        val transaction = fragmentManager.beginTransaction()
        transaction.add(R.id.flMainContainer, homeFragment)
        currentFragment = homeFragment
        transaction.commitAllowingStateLoss()
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
    }

    override fun onResume() {
        super.onResume()
        if (!updateChecked) {
            updateChecked = true
            checkUpdate()
        }
    }

    /**
     * 检查更新信息
     */
    private fun checkUpdate() {
        val currentVersionCode = BuildConfig.VERSION_CODE
        var newVersionCode = 0
        var updateDesc: String? = ""
        var updateLink: String? = ""
        HttpManager.instance.getAppConfig {
            for (config in it) {
                if (config.key == "version") {
                    newVersionCode = config.value.toInt()
                }
                if (config.key == "update_desc") {
                    updateDesc = config.value
                }
                if (config.key == "update_link") {
                    updateLink = config.value
                }
            }
            if (currentVersionCode < newVersionCode) {
                Log.i("version", "current = $currentVersionCode, new = $newVersionCode, 需要升级")
                AlertDialog.Builder(this)
                        .setTitle("发现新版本")
                        .setMessage(updateDesc)
                        .setPositiveButton("现在升级") { _, _ ->
                            val updateIntent = Intent()
                            updateIntent.action = "android.intent.action.VIEW"
                            val updateUrl = Uri.parse(updateLink)
                            updateIntent.data = updateUrl
                            startActivity(updateIntent)
                        }
                        .setNegativeButton("暂不升级") { dialog, _ -> dialog.dismiss() }
                        .create().show()
            }
        }
    }

    override fun onCategoryClick(type: Int) {
        val intent = Intent()
        intent.putExtra("saveType", type)
        intent.setClass(this, CategoryActivity::class.java)
        startActivity(intent)
    }
}
