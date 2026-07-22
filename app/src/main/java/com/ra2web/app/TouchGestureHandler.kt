package com.ra2web.app

import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.webkit.WebView
import kotlin.math.abs

/**
 * 触屏手势识别处理器
 * 实现双指平移/缩放、长按右键、双击选中、滑动框选等功能
 * 适配 Android 13+
 */
class TouchGestureHandler(
    private val activity: MainActivity,
    private val webView: WebView,
    private val jsBridge: JsBridge
) {

    private val gestureDetector: GestureDetector
    private val scaleDetector: ScaleGestureDetector

    // 手势状态
    private var isTwoFingerPan = false
    private var isPinchZoom = false
    private var lastTwoFingerX = 0f
    private var lastTwoFingerY = 0f
    private var isLongPress = false

    // 滑动框选状态
    private var isDragSelecting = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragThreshold = 20f

    init {
        // 基础手势检测
        gestureDetector = GestureDetector(activity, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                return false
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                jsBridge.simulateDoubleClick(e.x, e.y)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (!isTwoFingerPan && !isPinchZoom) {
                    isLongPress = true
                    jsBridge.simulateRightClick(e.x, e.y)
                    jsBridge.evaluateJavascript("AndroidBridge.vibrate('short')")
                }
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (e1 != null && e2.pointerCount == 1 && !isTwoFingerPan && !isPinchZoom) {
                    val totalDx = abs(e2.x - e1.x)
                    val totalDy = abs(e2.y - e1.y)

                    if (!isDragSelecting && (totalDx > dragThreshold || totalDy > dragThreshold)) {
                        isDragSelecting = true
                        dragStartX = e1.x
                        dragStartY = e1.y
                        simulateMouseDown(dragStartX, dragStartY)
                    }

                    if (isDragSelecting) {
                        simulateMouseMove(e2.x, e2.y)
                        return true
                    }
                }
                return false
            }
        })

        // 缩放手势检测
        scaleDetector = ScaleGestureDetector(activity, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                isPinchZoom = true
                return true
            }

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.scaleFactor
                val delta = (scaleFactor - 1f) * 2f
                jsBridge.zoomViewport(delta)
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                isPinchZoom = false
            }
        })
    }

    fun handleTouch(event: MotionEvent): Boolean {
        val pointerCount = event.pointerCount

        if (pointerCount >= 2) {
            when (event.actionMasked) {
                MotionEvent.ACTION_POINTER_DOWN -> {
                    isTwoFingerPan = false
                    isDragSelecting = false
                    lastTwoFingerX = getAverageX(event)
                    lastTwoFingerY = getAverageY(event)
                }

                MotionEvent.ACTION_MOVE -> {
                    if (!isPinchZoom && !isTwoFingerPan) {
                        val avgX = getAverageX(event)
                        val avgY = getAverageY(event)
                        val moveDist = Math.hypot(
                            (avgX - lastTwoFingerX).toDouble(),
                            (avgY - lastTwoFingerY).toDouble()
                        ).toFloat()

                        if (moveDist > 10f) {
                            isTwoFingerPan = true
                        }
                    }

                    if (isTwoFingerPan) {
                        val avgX = getAverageX(event)
                        val avgY = getAverageY(event)
                        val dx = avgX - lastTwoFingerX
                        val dy = avgY - lastTwoFingerY

                        jsBridge.panViewport(dx, dy)

                        lastTwoFingerX = avgX
                        lastTwoFingerY = avgY
                        return true
                    }
                }

                MotionEvent.ACTION_POINTER_UP -> {
                    if (pointerCount <= 2) {
                        isTwoFingerPan = false
                        isPinchZoom = false
                    }
                }
            }
        }

        if (pointerCount >= 2) {
            scaleDetector.onTouchEvent(event)
            if (isPinchZoom) {
                return true
            }
        }

        if (pointerCount == 1 && !isTwoFingerPan && !isPinchZoom) {
            gestureDetector.onTouchEvent(event)
        }

        if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            if (isDragSelecting) {
                simulateMouseUp(event.x, event.y)
                isDragSelecting = false
            }
            isLongPress = false
            isTwoFingerPan = false
            isPinchZoom = false
        }

        return false
    }

    private fun getAverageX(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) {
            sum += event.getX(i)
        }
        return sum / event.pointerCount
    }

    private fun getAverageY(event: MotionEvent): Float {
        var sum = 0f
        for (i in 0 until event.pointerCount) {
            sum += event.getY(i)
        }
        return sum / event.pointerCount
    }

    private fun simulateMouseDown(x: Float, y: Float) {
        jsBridge.evaluateJavascript("""
            (function() {
                var event = new MouseEvent('mousedown', {
                    clientX: $x, clientY: $y,
                    button: 0, buttons: 1,
                    bubbles: true, cancelable: true
                });
                var target = document.elementFromPoint($x, $y) || document;
                target.dispatchEvent(event);
            })()
        """.trimIndent())
    }

    private fun simulateMouseMove(x: Float, y: Float) {
        jsBridge.evaluateJavascript("""
            (function() {
                var event = new MouseEvent('mousemove', {
                    clientX: $x, clientY: $y,
                    button: 0, buttons: 1,
                    bubbles: true, cancelable: true
                });
                var target = document.elementFromPoint($x, $y) || document;
                target.dispatchEvent(event);
            })()
        """.trimIndent())
    }

    private fun simulateMouseUp(x: Float, y: Float) {
        jsBridge.evaluateJavascript("""
            (function() {
                var event = new MouseEvent('mouseup', {
                    clientX: $x, clientY: $y,
                    button: 0, buttons: 0,
                    bubbles: true, cancelable: true
                });
                var target = document.elementFromPoint($x, $y) || document;
                target.dispatchEvent(event);
            })()
        """.trimIndent())
    }

    fun injectTouchOptimizer() {
        jsBridge.evaluateJavascript("""
            (function() {
                window.__isAndroidApp = true;
                window.__androidBridge = window.AndroidBridge;

                var meta = document.querySelector('meta[name="viewport"]');
                if (meta) {
                    meta.content = 'width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover';
                }

                // 禁用橡皮筋效果
                document.body.style.overscrollBehavior = 'none';
                document.documentElement.style.overscrollBehavior = 'none';

                console.log('[RA2Web Android] Touch optimizer injected');
            })()
        """.trimIndent())
    }
}
