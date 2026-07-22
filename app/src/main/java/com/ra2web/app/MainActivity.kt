package com.ra2web.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

class MainActivity : AppCompatActivity() {

    private lateinit var gameWebView: WebView
    private lateinit var downloadLayout: View
    private lateinit var downloadProgress: ProgressBar
    private lateinit var downloadStatus: TextView

    private lateinit var jsBridge: JsBridge
    private lateinit var touchHandler: TouchGestureHandler
    private lateinit var resourceManager: ResourceManager
    private lateinit var vibrationHelper: VibrationHelper
    private lateinit var displayHelper: DisplayHelper

    // 通知权限请求
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // 权限结果处理，无论是否授予都继续
    }

    // 返回键处理（Android 14+ 预测性返回）
    private val backPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (gameWebView.canGoBack()) {
                gameWebView.goBack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
                isEnabled = true
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Android 12+: 显示启动画面
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // 注册返回键回调
        onBackPressedDispatcher.addCallback(this, backPressedCallback)

        // 保持屏幕常亮
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // 边对边显示（Android 15+ 优化）
        setupEdgeToEdge()

        setContentView(R.layout.activity_main)

        // 初始化视图
        gameWebView = findViewById(R.id.gameWebView)
        downloadLayout = findViewById(R.id.downloadLayout)
        downloadProgress = findViewById(R.id.downloadProgress)
        downloadStatus = findViewById(R.id.downloadStatus)

        // 初始化辅助类
        vibrationHelper = VibrationHelper(this)
        displayHelper = DisplayHelper(this)
        jsBridge = JsBridge(this, vibrationHelper)
        resourceManager = ResourceManager(this)
        touchHandler = TouchGestureHandler(this, gameWebView, jsBridge)

        // 配置 WebView
        setupWebView()

        // 请求通知权限（Android 13+）
        requestNotificationPermission()

        // 检查资源并加载游戏
        checkResourcesAndLoad()
    }

    /**
     * 设置边对边显示
     */
    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }

        // 沉浸式全屏
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * 请求通知权限（Android 13+）
     */
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 延迟一点再请求，避免启动时弹出
                gameWebView.postDelayed({
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }, 3000)
            }
        }
    }

    private fun setupWebView() {
        val settings: WebSettings = gameWebView.settings

        // 基本配置
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.setSupportZoom(false)
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true

        // 性能优化
        settings.cacheMode = WebSettings.LOAD_DEFAULT

        // 硬件加速
        gameWebView.setLayerType(View.LAYER_TYPE_HARDWARE, null)

        // 媒体配置
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

        // 允许文件访问
        settings.allowFileAccess = true
        settings.allowContentAccess = true

        // 离屏渲染（Android 14+ 优化）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            @Suppress("NewApi")
            settings.isAlgorithmicDarkeningAllowed = false
        }

        // 离屏渲染优化
        settings.offscreenPreRaster = true

        // WebChromeClient
        gameWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
            }
        }

        // WebViewClient + AssetLoader
        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .addPathHandler("/res/", WebViewAssetLoader.ResourcesPathHandler(this))
            .addPathHandler(
                "/gameres/",
                WebViewAssetLoader.InternalStoragePathHandler(
                    this,
                    filesDir.resolve("gameres")
                )
            )
            .build()

        gameWebView.webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: android.webkit.WebResourceRequest
            ): android.webkit.WebResourceResponse? {
                // 先尝试 AssetLoader 拦截
                val intercepted = assetLoader.shouldInterceptRequest(request.url)
                if (intercepted != null) {
                    return intercepted
                }

                // 拦截 CDN 资源请求，尝试从本地加载
                val url = request.url.toString()
                if (url.contains("stdres.wangerhuoda.cn") ||
                    url.contains("wyhjres2.bun.sh.cn")) {
                    val localPath = url.replace(Regex("^https?://[^/]+/"), "")
                    val localFile = filesDir.resolve("gameres/$localPath")
                    if (localFile.exists()) {
                        val ext = localFile.extension
                        val mimeType = getMimeType(ext)
                        try {
                            return android.webkit.WebResourceResponse(
                                mimeType,
                                "utf-8",
                                localFile.inputStream()
                            )
                        } catch (_: Exception) {
                        }
                    }
                }

                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                // 注入触屏优化脚本
                touchHandler.injectTouchOptimizer()
                jsBridge.onPageLoaded()
                // 重新应用沉浸式模式
                applyImmersiveMode()
            }
        }

        // 添加 JS 接口
        gameWebView.addJavascriptInterface(jsBridge, "AndroidBridge")

        // 设置触摸手势处理
        gameWebView.setOnTouchListener { _, event ->
            touchHandler.handleTouch(event)
            false
        }
    }

    private fun getMimeType(ext: String): String {
        return when (ext.lowercase()) {
            "js" -> "application/javascript"
            "css" -> "text/css"
            "html", "htm" -> "text/html"
            "json" -> "application/json"
            "png", "jpg", "jpeg", "gif", "webp", "bmp", "ico" -> "image/$ext"
            "svg" -> "image/svg+xml"
            "mp3", "wav", "ogg", "m4a" -> "audio/$ext"
            "mp4", "webm", "avi" -> "video/$ext"
            "wasm" -> "application/wasm"
            "woff", "woff2", "ttf", "otf" -> "font/$ext"
            "mix" -> "application/octet-stream"
            "ini" -> "text/plain"
            "map" -> "application/json"
            else -> "application/octet-stream"
        }
    }

    private fun checkResourcesAndLoad() {
        if (resourceManager.areResourcesReady()) {
            loadGame()
        } else {
            downloadLayout.visibility = View.VISIBLE
            gameWebView.visibility = View.GONE

            resourceManager.downloadResources(
                onProgress = { progress, status ->
                    runOnUiThread {
                        downloadProgress.progress = progress
                        downloadStatus.text = status
                    }
                },
                onComplete = {
                    runOnUiThread {
                        downloadLayout.visibility = View.GONE
                        loadGame()
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        downloadStatus.text = "下载失败: $error\n点击重试"
                        downloadLayout.setOnClickListener {
                            checkResourcesAndLoad()
                        }
                    }
                }
            )
        }
    }

    private fun loadGame() {
        gameWebView.visibility = View.VISIBLE
        gameWebView.loadUrl("https://appassets.androidplatform.net/assets/ra2web/index.html")
    }

    /**
     * 应用沉浸式全屏模式
     */
    private fun applyImmersiveMode() {
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onResume() {
        super.onResume()
        gameWebView.onResume()
        applyImmersiveMode()
    }

    override fun onPause() {
        super.onPause()
        gameWebView.onPause()
    }

    override fun onDestroy() {
        gameWebView.stopLoading()
        gameWebView.removeJavascriptInterface("AndroidBridge")
        gameWebView.destroy()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            applyImmersiveMode()
        }
    }
}
