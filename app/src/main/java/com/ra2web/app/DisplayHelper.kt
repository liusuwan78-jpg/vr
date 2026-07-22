package com.ra2web.app

import android.app.Activity
import android.content.res.Configuration
import android.os.Build
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * 显示适配助手 - 全屏、刘海屏、横屏等
 * 适配 Android 13/14/15
 */
class DisplayHelper(private val activity: Activity) {

    /**
     * 设置沉浸式全屏模式（兼容 Android 11+）
     */
    fun setupFullscreen() {
        val window = activity.window

        // 边对边显示
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val controller = WindowInsetsControllerCompat(window, window.decorView)

        // 隐藏系统栏
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.hide(WindowInsetsCompat.Type.displayCutout())

        // 手势导航条的行为 - 短暂显示后自动隐藏
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        // 刘海屏适配
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
    }

    /**
     * 获取安全边距（避开刘海和导航栏）
     */
    fun getSafeInsets(): SafeInsets {
        val window = activity.window
        var left = 0
        var top = 0
        var right = 0
        var bottom = 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val insets = window.decorView.rootWindowInsets?.getInsets(
                WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout()
            )
            if (insets != null) {
                left = insets.left
                top = insets.top
                right = insets.right
                bottom = insets.bottom
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 旧版本估算
            val resourceId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
            if (resourceId > 0) {
                top = activity.resources.getDimensionPixelSize(resourceId)
            }
        }

        return SafeInsets(left, top, right, bottom)
    }

    /**
     * 是否是深色模式
     */
    fun isDarkMode(): Boolean {
        val currentNightMode = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }

    /**
     * 设置屏幕亮度
     */
    fun setBrightness(brightness: Float) {
        val layoutParams = activity.window.attributes
        layoutParams.screenBrightness = brightness.coerceIn(0f, 1f)
        activity.window.attributes = layoutParams
    }

    data class SafeInsets(
        val left: Int,
        val top: Int,
        val right: Int,
        val bottom: Int
    )
}
