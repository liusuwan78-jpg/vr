package com.ra2web.app

import android.webkit.WebView
import org.json.JSONObject

/**
 * JS 与原生互调桥接
 */
class JsBridge(
    private val activity: MainActivity,
    private val vibrationHelper: VibrationHelper
) {

    private var webView: WebView? = null

    fun attachWebView(view: WebView) {
        webView = view
    }

    fun onPageLoaded() {
        webView = activity.findViewById(R.id.gameWebView)
        // 通知 JS 端原生桥接已就绪
        evaluateJavascript("""
            if (window.__androidBridgeReady) {
                window.__androidBridgeReady();
            }
        """.trimIndent())
    }

    /**
     * 执行 JS 代码
     */
    fun evaluateJavascript(script: String, callback: ((String) -> Unit)? = null) {
        webView?.post {
            webView?.evaluateJavascript(script) { result ->
                callback?.invoke(result ?: "null")
            }
        }
    }

    // ========== JS 可调用的原生方法 ==========

    /**
     * 震动反馈
     * @param pattern 震动模式: "short" | "medium" | "long" | "attack"
     */
    @android.webkit.JavascriptInterface
    fun vibrate(pattern: String) {
        when (pattern) {
            "short" -> vibrationHelper.vibrateShort()
            "medium" -> vibrationHelper.vibrateMedium()
            "long" -> vibrationHelper.vibrateLong()
            "attack" -> vibrationHelper.vibrateAttack()
        }
    }

    /**
     * 显示 toast 消息
     */
    @android.webkit.JavascriptInterface
    fun showToast(message: String) {
        activity.runOnUiThread {
            android.widget.Toast.makeText(activity, message, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取设备信息
     */
    @android.webkit.JavascriptInterface
    fun getDeviceInfo(): String {
        val info = JSONObject().apply {
            put("platform", "android")
            put("isMobile", true)
            put("sdk", android.os.Build.VERSION.SDK_INT)
            put("model", android.os.Build.MODEL)
            put("manufacturer", android.os.Build.MANUFACTURER)
        }
        return info.toString()
    }

    /**
     * 获取本地资源路径
     */
    @android.webkit.JavascriptInterface
    fun getLocalResPath(): String {
        return activity.filesDir.resolve("gameres").absolutePath
    }

    /**
     * 检查资源是否就绪
     */
    @android.webkit.JavascriptInterface
    fun areResourcesReady(): Boolean {
        val resDir = activity.filesDir.resolve("gameres")
        return resDir.exists() && resDir.listFiles()?.isNotEmpty() == true
    }

    // ========== 触屏操作相关 ==========

    /**
     * 平移视野
     * @param dx X方向偏移（像素）
     * @param dy Y方向偏移（像素）
     */
    fun panViewport(dx: Float, dy: Float) {
        evaluateJavascript("""
            (function() {
                // 尝试调用游戏内的视野平移 API
                if (window.__game && window.__game.viewport) {
                    window.__game.viewport.pan($dx, $dy);
                    return true;
                }
                // 回退方案：模拟鼠标移动到边缘
                return false;
            })()
        """.trimIndent())
    }

    /**
     * 缩放视野
     * @param delta 缩放变化量（正数放大，负数缩小）
     */
    fun zoomViewport(delta: Float) {
        evaluateJavascript("""
            (function() {
                if (window.__game && window.__game.viewport) {
                    window.__game.viewport.zoom($delta);
                    return true;
                }
                // 回退方案：模拟滚轮事件
                var event = new WheelEvent('wheel', {
                    deltaY: ${-delta * 100},
                    bubbles: true,
                    cancelable: true
                });
                document.dispatchEvent(event);
                return false;
            })()
        """.trimIndent())
    }

    /**
     * 模拟右键点击
     */
    fun simulateRightClick(x: Float, y: Float) {
        evaluateJavascript("""
            (function() {
                var event = new MouseEvent('contextmenu', {
                    clientX: $x,
                    clientY: $y,
                    button: 2,
                    bubbles: true,
                    cancelable: true
                });
                document.elementFromPoint($x, $y)?.dispatchEvent(event) || document.dispatchEvent(event);
            })()
        """.trimIndent())
    }

    /**
     * 模拟左键双击
     */
    fun simulateDoubleClick(x: Float, y: Float) {
        evaluateJavascript("""
            (function() {
                var target = document.elementFromPoint($x, $y) || document;
                var event1 = new MouseEvent('click', {
                    clientX: $x, clientY: $y, button: 0, bubbles: true, cancelable: true, detail: 1
                });
                var event2 = new MouseEvent('dblclick', {
                    clientX: $x, clientY: $y, button: 0, bubbles: true, cancelable: true, detail: 2
                });
                target.dispatchEvent(event1);
                target.dispatchEvent(event2);
            })()
        """.trimIndent())
    }
}
