package info.free.scp.view.detail

import android.app.AlertDialog
import android.content.*
import android.content.DialogInterface.BUTTON_POSITIVE
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.view.Gravity.CENTER
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout.VERTICAL
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.umeng.analytics.MobclickAgent
import info.free.scp.R
import info.free.scp.SCPConstants
import info.free.scp.SCPConstants.AppMode.OFFLINE
import info.free.scp.SCPConstants.SCP_SITE_URL
import info.free.scp.bean.ScpLikeBox
import info.free.scp.bean.ScpLikeModel
import info.free.scp.bean.ScpModel
import info.free.scp.db.AppInfoDatabase
import info.free.scp.db.ScpDataHelper
import info.free.scp.db.ScpDatabase
import info.free.scp.util.EventUtil
import info.free.scp.util.PreferenceUtil
import info.free.scp.util.ThemeUtil
import info.free.scp.util.ThemeUtil.DAY_THEME
import info.free.scp.util.ThemeUtil.NIGHT_THEME
import info.free.scp.util.Utils
import info.free.scp.view.base.BaseActivity
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.layout_dialog_report.view.*
import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onScrollChange
import org.jetbrains.anko.sdk27.coroutines.onSeekBarChangeListener
import taobe.tec.jcc.JChineseConvertor
import java.io.IOException


class DetailActivity : BaseActivity() {

    private var onlineMode = 0 // 0 离线 1 网页
    private var readType = 0 // 0 普通（按顺序） 1 随机 2 TODO 未读列表
    private var randomType = 0 // 0 所有，1仅scp，2 故事，3 joke
    private var itemType = 0 //
    private var url = ""
        set(value) {
            field = value
            fullUrl = if (value.contains("http")) value else "http://scp-wiki-cn.wikidot.com$value"
        }
    private var title = ""
    private var scp: ScpModel? = null
    private var detailHtml = ""
    private var textSizeList = arrayOf("12px", "14px", "16px", "18px", "20px")
    private var currentTextSizeIndex = textSizeList.indexOf(PreferenceUtil.getDetailTextSize())
        set(value) {
            field = value
            currentTextSize = textSizeList[value]
        }
    private var currentTextSize = PreferenceUtil.getDetailTextSize()
        set(value) {
            field = value
            PreferenceUtil.setDetailTextSize(value)
            nightTextStyle = "<style>body{background-color:#222;}p {font-size:" +
                    "$currentTextSize;}* {color:#aaa;}</style>"
            dayTextStyle = "<style>p {font-size:$currentTextSize}" +
                    ";}* {color:#000;}</style>"
        }
    private var nightTextStyle = "<style>body{background-color:#222;}p {font-size:" +
            "$currentTextSize;}* {color:#aaa;}</style>"
    private var dayTextStyle = "<style>body{background-color:#fff;}p {font-size:$currentTextSize;}* {color:#000;}</style>"
    private val siteStyle = "<link rel=\"stylesheet\" type=\"text/css\" href=\"style.css\" />"
    private var currentTextStyle = siteStyle + (if (ThemeUtil.currentTheme == NIGHT_THEME) nightTextStyle else dayTextStyle)
    private val jqScript = "<script type=\"text/javascript\" src=\"jquery-ui.min.js\"></script>\n"
    private val initScript = "<script type=\"text/javascript\" src=\"init.combined.js\"></script>"
    private val tabScript = "<script type=\"text/javascript\" src=\"tabview-min.js\"></script>"
    private val wikiScript = "<script type=\"text/javascript\" src=\"WIKIDOT.combined.js\"></script>"
    private val jsScript = "$jqScript$initScript$tabScript$wikiScript"
    private var screenHeight = 0
    private val historyList: MutableList<ScpModel> = emptyList<ScpModel>().toMutableList()
    private val randomList: MutableList<ScpModel> = emptyList<ScpModel>().toMutableList()
    private var randomIndex = 0
    private var historyIndex = 0
    private var fullUrl = ""
    private var forceOnline = false

    private var tvLoad: TextView? = null

    private val viewModel by lazy {
        ViewModelProvider(this)
                .get(DetailViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)
        tvLoad = TextView(this)
        tvLoad?.text = "评论加载中..."
        tvLoad?.gravity = CENTER
        screenHeight = Utils.getScreenHeight(this)

        EventUtil.onEvent(this, EventUtil.clickReadDetail)
        webView?.setBackgroundColor(0) // 设置背景色
        webView?.background?.alpha = 0 // 设置填充透明度 范围：0-255
        webView?.setBackgroundColor(ThemeUtil.containerBg)
        webView?.settings?.javaScriptEnabled = true

        url = intent.getStringExtra("link") ?: ""
        title = intent.getStringExtra("title") ?: ""
        // 0 普通（按顺序） 1 随机 2 TODO 未读列表
        readType = intent.getIntExtra("read_type", 0)
        // 0 所有，1仅scp，2 故事，3 joke
        randomType = intent.getIntExtra("random_type", 0)
        itemType = intent.getIntExtra("scp_type", 0)
        forceOnline = intent.getBooleanExtra("forceOnline", false)

        fullUrl = if (url.contains("http")) url else "$SCP_SITE_URL$url"

        // 有些不是以/开头的而是完整链接
        if (url.isEmpty()) {
            // 入口都确定了有url，没有的话直接finish
            finish()
        } else if (!forceOnline) {
            viewModel.setScp(url, title) // 设置scp
        } else {
            pbLoading.visibility = VISIBLE
            webView.loadUrl(fullUrl)
        }

        viewModel.getScp()?.observe(this, Observer {
            // 数据库取到
            if (it != null) {
                scp = it as ScpModel
                scp?.let { s ->
                    setData(s)
                }
            }
        }) ?: run {
            // 数据库没有，加载链接
            webView.loadUrl(fullUrl)
            nsv_web_wrapper?.scrollTo(0, 0)
        }

        viewModel.getOfflineScp()?.observe(this, Observer {
            // 数据库取到
            if (it != null) {
                scp = it as ScpModel
                scp?.let { s ->
                    setData(s)
                }
            }
        }) ?: run {
            // 数据库没有，加载链接
            pbLoading.visibility = VISIBLE
            webView.loadUrl(fullUrl)
            nsv_web_wrapper?.scrollTo(0, 0)
        }

        viewModel.getOfflineCollection()?.observe(this, Observer {
            // 数据库取到
            if (it != null) {
                scp = it as ScpModel
                scp?.let { s ->
                    setData(s)
                }
            }
        }) ?: run {
            // 数据库没有，加载链接
            pbLoading.visibility = VISIBLE
            webView.loadUrl(fullUrl)
            nsv_web_wrapper?.scrollTo(0, 0)
        }

        webView?.requestFocus()

        //覆盖WebView默认通过第三方或系统浏览器打开网页的行为
        webView?.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, requestUrl: String): Boolean {
                info(requestUrl)
                info { "onPageshouldOverrideUrlLoading" }
                if (requestUrl.startsWith(SCP_SITE_URL) and requestUrl.contains("/html/")) {
                    return false
                }
                if (requestUrl.startsWith(SCP_SITE_URL)) {
                    return false
                }
                if (requestUrl.contains("player.bilibili.com")) {
                    return false
                }

                if (onlineMode == 1) {
                    view.loadUrl(requestUrl)
                } else {
                    if (requestUrl.startsWith(SCP_SITE_URL)) {
                        url = requestUrl.subSequence(30, requestUrl.length).toString()
                    } else if (requestUrl.startsWith("file:")) {
                        url = requestUrl.subSequence(7, requestUrl.length).toString()
                    } else {
                        url = requestUrl
                    }
                    info(url)
                    val tmpScp = ScpDatabase.getInstance()?.scpDao()?.getScpByLink(url)
                    tmpScp?.let {
                        scp = tmpScp
                        setData(tmpScp)
                    } ?: run {
                        pbLoading.visibility = VISIBLE
                        view.loadUrl(fullUrl)
                    }
                }
                return false
            }


            override fun onPageCommitVisible(view: WebView?, url: String?) {
                pbLoading.visibility = GONE
            }

            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                pbLoading.visibility = VISIBLE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                pbLoading.visibility = GONE
            }
        }

        if (!PreferenceUtil.getShownDetailNotice()) {
            AlertDialog.Builder(this)
                    .setTitle("Notice")
                    .setMessage("1.右上角菜单可以切换网络阅读和离线阅读模式（如果本地数据没有加载完成则离线模式可能不可用）\n" +
                            "2.所有显示尚无内容的即表示在网上是404状态，即禁止访问，可能存在数据更新不及时的情况，所以也可以切换阅读模式看原网页\n" +
                            "3.文档数量较多，如果发现有疏漏，如文不对题等，可右上角菜单选择反馈问题\n" +
                            "4.图片依然需要网络才能显示出来，另有一些网页上依赖代码等复杂的文章，请切换跳转网页查看\n")
                    .setPositiveButton("OK") { dialog, _ ->
                        PreferenceUtil.setShownDetailNotice()
                        dialog.dismiss()
                    }
                    .create().show()
        }

        sb_detail?.onSeekBarChangeListener {
            this.onProgressChanged { seekBar, i, b ->
                if (b) {
                    nsv_web_wrapper?.scrollTo(0, (webView.height * (i / 100f)).toInt())
                }
            }
        }
        nsv_web_wrapper?.onScrollChange { v, scrollX, scrollY, oldScrollX, oldScrollY ->
            webView?.let {
                sb_detail?.progress = ((scrollY.toFloat() / webView.height) * 100).toInt()
            }
        }

        initToolbar()
        initSwitchBtn()
        refreshReadBtnStatus()
    }

    private fun setDetail(detail: String) {
        detailHtml = detail
        // 显示frame
        if (!detailHtml.contains("""<iframe src="//player.bilibili.com""")) {
            detailHtml = detailHtml.replace("""<iframe src="/""", """<iframe src="http://scp-wiki-cn.wikidot.com/""")
        }
        detailHtml = detailHtml.replace("html-block-iframe", "")

        if (detailHtml.isEmpty()) {
            pbLoading.visibility = VISIBLE
            webView.loadUrl(fullUrl)
        } else {
            pbLoading.visibility = GONE
            webView.loadDataWithBaseURL("file:///android_asset/", currentTextStyle
                    + detailHtml + jsScript,
                    "text/html", "utf-8", null)
        }
        nsv_web_wrapper?.scrollTo(0, 0)
        btn_comment?.show()
        ll_comment_container.removeAllViews()
        ll_comment_container.addView(tvLoad, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }

    override fun refreshTheme() {
        super.refreshTheme()
        cl_detail_container?.setBackgroundColor(ThemeUtil.containerBg)
        refreshStyle()
        refreshReadBtnStatus()
        refreshButtonStyle()
    }

    /**
     * 不改变网页内容，只刷新样式
     */
    private fun refreshStyle() {
        currentTextStyle = siteStyle + (if (ThemeUtil.currentTheme == NIGHT_THEME) nightTextStyle else dayTextStyle)
        webView.loadDataWithBaseURL("file:///android_asset/", currentTextStyle
                + detailHtml + jsScript,
                "text/html", "utf-8", null)
    }

    private fun setData(s: ScpModel, back: Boolean = false) {
        if (readType == 1) {
            randomList.add(s)
        }
        viewModel.setScpReadInfo() // scp拿到之后，设置已读数据和拿like数据
        viewModel.getScpLikeInfo()?.observe(this, Observer { scpInfo ->
            if (scpInfo == null) {
                viewModel.setScpLikeInfo() // like数据拿到以后，进行初始化
            }
            invalidateOptionsMenu()
        })
        // 刷新toolbar（收藏状态
        invalidateOptionsMenu()
        // 更新标题
        supportActionBar?.setDisplayShowTitleEnabled(false)
        if (!back) {
            historyList.add(s)
            historyIndex = historyList.size - 1
        }
        tv_detail_toolbar?.text = s.title
        tv_detail_toolbar?.isSelected = true
        url = s.link
        refreshReadBtnStatus()
        viewModel.loadDetail(url)
        if (PreferenceUtil.getAppMode() == OFFLINE) {
            viewModel.getOfflineDetail().observe(this, Observer { detail ->
                if (!detail.isNullOrEmpty()) {
                    setDetail(detail)
                }
            })
        } else {
            viewModel.getDetail().observe(this, Observer { detail ->
                if (!detail.isNullOrEmpty()) {
                    setDetail(detail)
                }
            })
        }
    }


    private fun initToolbar() {
        baseToolbar = detail_toolbar
        supportActionBar?.title = null
        detail_toolbar?.inflateMenu(R.menu.detail_menu) //设置右上角的填充菜单
        detail_toolbar?.setOnMenuItemClickListener {
            scp?.let { s ->
                when (it.itemId) {
                    R.id.switch_read_mode -> {
                        PreferenceUtil.addPoints(1)
                        if (onlineMode == 0) {
                            if (enabledNetwork()) {
                                pbLoading.visibility = VISIBLE
                                onlineMode = 1
                                it.setTitle(R.string.offline_mode)
                                webView?.loadUrl(fullUrl)
                            } else {
                                toast("请先开启网络")
                            }
                        } else {
                            onlineMode = 0
                            it.setTitle(R.string.online_mode)
                            webView?.loadDataWithBaseURL("file:///android_asset/",
                                    currentTextStyle + detailHtml + jsScript,
                                    "text/html", "utf-8", null)
                        }
                    }
                    R.id.report -> {
                        val reportView = LayoutInflater.from(this@DetailActivity)
                                .inflate(R.layout.layout_dialog_report, null)
                        val reportDialog = AlertDialog.Builder(this@DetailActivity)
                                .setTitle("反馈问题")
                                .setView(reportView)
                                .setPositiveButton("OK") { _, _ -> }
                                .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                                .create()
                        reportDialog.show()
                        reportDialog.getButton(BUTTON_POSITIVE).setOnClickListener {
                            val reportString = reportView.et_report.text.toString()
                            MobclickAgent.reportError(this@DetailActivity, "url: $url, detail: $reportString")
                            reportDialog.dismiss()
                        }
                    }
                    R.id.open_in_browser -> {
                        EventUtil.onEvent(this, EventUtil.clickOpenInBrowser, s.link)
                        PreferenceUtil.addPoints(1)
                        val openIntent = Intent()
                        openIntent.action = "android.intent.action.VIEW"
                        val openUrl = Uri.parse(fullUrl)
                        openIntent.data = openUrl
                        startActivity(openIntent)
                    }
                    R.id.copy_link -> {
                        EventUtil.onEvent(this, EventUtil.clickCopyLink, s.link)
                        val clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager?
                        val clipData = ClipData.newPlainText("scp_link", fullUrl)
                        clipboardManager?.primaryClip = clipData
                        toast("已复制到剪贴板")
                    }
                    R.id.like -> {
                        likeScp()
                    }
                    R.id.change_theme -> {
                        it.setTitle(if (ThemeUtil.currentTheme == DAY_THEME) R.string.day_mode else R.string.dark_mode)
                        changeTheme(if (ThemeUtil.currentTheme == DAY_THEME) NIGHT_THEME else DAY_THEME)
                    }
                    R.id.add_read_later -> {
                        ScpDataHelper.getInstance().insertViewListItem(s.link, s.title,
                                SCPConstants.LATER_TYPE)
                        toast("已加入待读列表")
                    }
                    R.id.share_picture -> {
                        // 截屏分享
                        EventUtil.onEvent(this, EventUtil.clickShareByPicture, s.link)
                        toast("生成图片中...")
                        gp_share_content?.visibility = VISIBLE
                        cl_detail_container?.viewTreeObserver?.addOnGlobalLayoutListener(object :
                                ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                cl_detail_container?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                                val bitmap = Bitmap.createBitmap(webView.width, cl_detail_container.height,
                                        Bitmap.Config.RGB_565)
                                //使用Canvas，调用自定义view控件的onDraw方法，绘制图片
                                val canvas = Canvas(bitmap)
                                cl_detail_container.draw(canvas)
                                doAsync {
                                    // Runs in background
                                    Utils.saveBitmapFile(bitmap, scp?.title?.replace(" ", "") ?: "")
                                    // This code is executed on the UI thread
                                    uiThread {
                                        toast("图片已保存")
                                        gp_share_content?.visibility = GONE
                                    }
                                }
                            }
                        })
                    }
                    R.id.big_text -> {
                        if (currentTextSizeIndex < 4) {
                            currentTextSizeIndex++
                            refreshStyle()
                        }
                        return@let
                    }
                    R.id.small_text -> {
                        if (currentTextSizeIndex > 0) {
                            currentTextSizeIndex--
                            refreshStyle()
                        }
                        return@let
                    }
                    R.id.translate_to_simple -> {
                        translate(simple)
                    }
                    R.id.translate_to_traditional -> {
                        translate(traditional)
                    }
                    else -> {
                    }
                }
            }
            true
        }
    }

    private fun changeTheme(mode: Int) {
        ThemeUtil.changeTheme(this, mode)
    }

    private val simple = 0
    private val traditional = 1

    /**
     * 繁简转换
     */
    private fun translate(translateType: Int) {
        try {
            val converter = JChineseConvertor.getInstance()
            detailHtml = if (translateType == simple) converter.t2s(detailHtml) else converter.s2t(detailHtml)
            webView.loadDataWithBaseURL("file:///android_asset/", currentTextStyle
                    + detailHtml + jsScript,
                    "text/html", "utf-8", null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 先get or create一个scpInfo信息
     * 未收藏
     * 已收藏
     */
    private fun likeScp() {
        val scpInfo = viewModel.getScpLikeInfo()?.value ?: return
        val likeDao = AppInfoDatabase.getInstance().likeAndReadDao()
        if (!scpInfo.like) {
            // 未收藏
            // 获取数据库中的收藏夹
            val boxList = arrayListOf<ScpLikeBox>()
            boxList.addAll(likeDao.getLikeBox())
            if (boxList.isEmpty()) {
                val defaultBox = ScpLikeBox(0, "默认收藏夹")
                boxList.add(defaultBox)
                likeDao.saveLikeBox(defaultBox)
            }
            val nameList = arrayListOf<String>()
            nameList.addAll(boxList.map { it.name })
            nameList.add("新建收藏夹")
            // 显示收藏夹列表和新建收藏夹选项
            selector("加入收藏夹", nameList) { _, i ->
                if (i == boxList.size) {
                    // 新建收藏夹
                    createNewBox()
                    return@selector
                } else {
                    // 选择一个收藏夹加入
                    PreferenceUtil.addPoints(2)
                    scpInfo.boxId = boxList[i].id
                    scpInfo.like = true
                    viewModel.likeScp(scpInfo)
                }
            }
        } else {
            scpInfo.like = false
            viewModel.likeScp(scpInfo)
        }
    }

    private fun createNewBox() {
        var input: EditText? = null
        alert {
            customView {
                linearLayout {
                    padding = dip(16)
                    orientation = VERTICAL
                    textView("输入收藏夹标题") {
                        textColor = ThemeUtil.darkText
                        textSize = 18f
                    }
                    input = editText {
                        height = WRAP_CONTENT
                        width = MATCH_PARENT
                        singleLine = true
                    }
                }

            }
            positiveButton("确定") {
                input?.clearFocus()
                val defaultBox = ScpLikeBox(0, input?.text?.toString() ?: "")
                info { defaultBox }
                AppInfoDatabase.getInstance().likeAndReadDao().saveLikeBox(defaultBox)
                likeScp()
            }
            negativeButton("取消") {}

        }.show()
    }

    private fun toNextArticle() {
        val index = scp?.index ?: 0
        val scpType = scp?.scpType ?: 0
        PreferenceUtil.addPoints(1)
        when (readType) {
            0 -> {
                scp = if (itemType == 0) {
                    ScpDatabase.getInstance()?.scpDao()?.getNextScp(index, scpType)
                } else {
                    ScpDatabase.getInstance()?.scpDao()?.getNextCollection(index, scpType)
                }
                scp?.let {
                    setData(it)
                } ?: toast("已经是最后一篇了")
            }
            1 -> {
                if (randomIndex < randomList.size - 1) {
                    scp = randomList[++randomIndex]
                    scp?.let {
                        setData(it)
                    }
                } else {
                    val randomRange = when (randomType) {
                        1 -> "1,2"
                        2 -> "3,4"
                        3 -> "5,6"
                        else -> ""
                    }
                    scp = ScpDataHelper.getInstance().getRandomScp(randomRange)
                    scp?.let {
                        randomList.add(it)
                        randomIndex++
                        setData(it)
                    }
                }
            }
            else -> {
            }
        }
    }

    private fun toPreviewArticle() {
        val index = scp?.index ?: 0
        val scpType = scp?.scpType ?: 0
        PreferenceUtil.addPoints(1)
        when (readType) {
            0 -> {
                when (index) {
                    0 -> toast("已经是第一篇了")
                    else -> {
                        scp = if (itemType == 0) {
                            ScpDatabase.getInstance()?.scpDao()?.getPreviewScp(index, scpType)
                        } else {
                            ScpDatabase.getInstance()?.scpDao()?.getPreviewCollection(index, scpType)
                        }
                        scp?.let {
                            setData(it)
                        }
                    }
                }
            }
            1 -> {
                if (randomList.isNotEmpty() && randomIndex - 1 >= 0) {
                    scp = randomList[--randomIndex]
                    scp?.let {
                        setData(it)
                    }
                } else {
                    toast("已经是第一篇了")
                }
            }
            else -> {
            }
        }

    }

    private var readBtnLp: ConstraintLayout.LayoutParams? = null

    private fun setHasRead() {
        scp?.let { s ->
            var scpInfo = AppInfoDatabase.getInstance().likeAndReadDao().getInfoByLink(s.link)
            if (scpInfo == null) {
                scpInfo = ScpLikeModel(s.link, s.title, like = false, hasRead = false, boxId = 0)
            }
            if (scpInfo.hasRead) {
                // 取消已读
                PreferenceUtil.reducePoints(5)
                scpInfo.hasRead = false
                AppInfoDatabase.getInstance().likeAndReadDao().save(scpInfo)
                refreshReadBtnStatus()
            } else {
                // 标记已读
                PreferenceUtil.addPoints(5)
                scpInfo.hasRead = true
                AppInfoDatabase.getInstance().likeAndReadDao().save(scpInfo)
                refreshReadBtnStatus()
            }
        }
    }

    private fun refreshReadBtnStatus() {
        val scpInfo = AppInfoDatabase.getInstance().likeAndReadDao().getInfoByLink(url)
        val hasRead = scpInfo != null && scpInfo.hasRead
        readBtnLp = tv_bottom_set_has_read?.layoutParams as ConstraintLayout.LayoutParams?
        if (hasRead) {
            tv_bottom_set_has_read?.setText(R.string.set_has_not_read)
            tv_bottom_set_has_read?.setTextColor(ThemeUtil.lightText)
            tv_bottom_set_has_read?.background = ThemeUtil.customShape(
                    ThemeUtil.disabledBg, ThemeUtil.disabledBg, 0, dip(15))
            readBtnLp?.endToEnd = -1
            readBtnLp?.startToStart = -1
            readBtnLp?.endToStart = R.id.gl_detail_center
            tv_bottom_set_has_read?.layoutParams = readBtnLp
            tv_bottom_like?.visibility = VISIBLE
        } else {
            tv_bottom_set_has_read?.setText(R.string.set_has_read)
            tv_bottom_set_has_read?.setTextColor(ThemeUtil.darkText)
            tv_bottom_set_has_read?.background = ThemeUtil.customShape(
                    ThemeUtil.itemBg, ThemeUtil.itemBg, 0, dip(15))
            readBtnLp?.endToEnd = 0
            readBtnLp?.startToStart = 0
            readBtnLp?.endToStart = -1
            tv_bottom_set_has_read?.layoutParams = readBtnLp
            tv_bottom_like?.visibility = GONE
        }
    }

    private fun initSwitchBtn() {
        refreshButtonStyle()
        tv_bottom_preview?.setOnClickListener {
            if (PreferenceUtil.getAppMode() == OFFLINE) {
                toPreviewArticle()
            } else {
                toast("仅限离线模式下使用")
            }
        }

        tv_bottom_next?.setOnClickListener {
            if (PreferenceUtil.getAppMode() == OFFLINE) {
                toNextArticle()
            } else {
                toast("仅限离线模式下使用")
            }
        }

        tv_bottom_set_has_read?.setOnClickListener {
            setHasRead()
        }

        tv_bottom_like?.setOnClickListener { likeScp() }

        viewModel.repo.commentList.observe(this, Observer {
            ll_comment_container.removeAllViews()
            it.forEach { c ->
                val newComment = CommentLayout(this)
                newComment.setData(c)
                ll_comment_container.addView(newComment, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
            }
        })
        btn_comment?.setOnClickListener {
            ll_comment_container.visibility = VISIBLE
            nsv_web_wrapper?.scrollTo(0, nsv_web_wrapper.height)
            btn_comment?.hide()
            viewModel.loadComment(url)
        }
    }

    private fun refreshButtonStyle() {
        tv_bottom_preview?.background = ThemeUtil.customShape(
                ThemeUtil.itemBg, ThemeUtil.itemBg, 0, dip(15))
        tv_bottom_next?.background = ThemeUtil.customShape(
                ThemeUtil.itemBg, ThemeUtil.itemBg, 0, dip(15))
        tv_bottom_like?.setTextColor(ThemeUtil.darkText)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KEYCODE_BACK && webView != null && historyIndex > 0) {
            scp = historyList[historyIndex - 1]
            scp?.let {
                setData(it, true)
            }
            historyList.removeAt(historyIndex - 1)
            historyIndex--
            return true
        } else {
            return super.onKeyDown(keyCode, event)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.detail_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val scpInfo = AppInfoDatabase.getInstance().likeAndReadDao().getInfoByLink(url)
        val menuItem = menu?.getItem(0)
        if (scpInfo == null || !scpInfo.like) {
            menuItem?.setIcon(R.drawable.ic_star_border_white_24dp)
            tv_bottom_like?.text = "收藏"
            tv_bottom_like?.background = ThemeUtil.customShape(
                    ThemeUtil.itemBg, ThemeUtil.itemBg, 0, dip(15))
        } else {
            menuItem?.setIcon(R.drawable.ic_star_white_24dp)
            tv_bottom_like?.text = "取消收藏"
            tv_bottom_like?.background = ThemeUtil.customShape(
                    ThemeUtil.disabledBg, ThemeUtil.disabledBg, 0, dip(15))
        }
        return super.onPrepareOptionsMenu(menu)
    }
}
