package com.ra2web.app

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/**
 * 震动反馈助手
 * 适配 Android 12+ 新震动 API
 */
class VibrationHelper(private val context: Context) {

    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    /**
     * 短震 - 选中单位等轻量反馈
     */
    fun vibrateShort() {
        vibrate(10, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    /**
     * 中震 - 建造完成等中等反馈
     */
    fun vibrateMedium() {
        vibrate(30, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    /**
     * 长震 - 单位阵亡等重要反馈
     */
    fun vibrateLong() {
        vibrate(100, VibrationEffect.DEFAULT_AMPLITUDE)
    }

    /**
     * 受击震动 - 连续短震
     */
    fun vibrateAttack() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+: 使用预制效果
            val effect = VibrationEffect.createWaveform(
                longArrayOf(0, 15, 25, 15),
                intArrayOf(0, VibrationEffect.DEFAULT_AMPLITUDE, 0, VibrationEffect.DEFAULT_AMPLITUDE),
                -1
            )
            vibrator.vibrate(effect)
        } else {
            val pattern = longArrayOf(0, 20, 30, 20)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    /**
     * 点击反馈 - 模拟物理按键触感
     */
    fun vibrateClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            )
        } else {
            vibrate(8, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    /**
     * 重型点击反馈
     */
    fun vibrateHeavyClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
            )
        } else {
            vibrate(20, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    /**
     * 双击反馈
     */
    fun vibrateDoubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK)
            )
        } else {
            val pattern = longArrayOf(0, 10, 50, 10)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, -1)
            }
        }
    }

    private fun vibrate(milliseconds: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(milliseconds, amplitude)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    /**
     * 取消震动
     */
    fun cancel() {
        vibrator.cancel()
    }

    /**
     * 检查是否有震动器
     */
    fun hasVibrator(): Boolean {
        return vibrator.hasVibrator()
    }
}
