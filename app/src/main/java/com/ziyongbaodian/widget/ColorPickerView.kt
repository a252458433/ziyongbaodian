package com.ziyongbaodian.widget

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

/**
 * 简易 HSV 取色器：上方为「饱和度 × 明度」面板，下方为色相条。
 *
 * 只处理 RGB，不处理透明度：浮窗文字的透明度由单独的「文字不透明度」滑块控制，
 * 两者的叠加效果由浮窗自己按层合成，这样颜色选得准、文字也能单独调淡。
 */
class ColorPickerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val hsv = floatArrayOf(216f, 0.82f, 1f)

    private val svRect = RectF()
    private val hueRect = RectF()

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shade = Paint(Paint.ANTI_ALIAS_FLAG)
    private val huePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val thumbWhite = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    private val thumbRing = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dp(1.5f)
        color = 0x66000000
    }

    private val hueColors = intArrayOf(
        0xFFFF0000.toInt(), 0xFFFFFF00.toInt(), 0xFF00FF00.toInt(), 0xFF00FFFF.toInt(),
        0xFF0000FF.toInt(), 0xFFFF00FF.toInt(), 0xFFFF0000.toInt()
    )

    private val hueHeight = dp(22f)
    private val gap = dp(14f)

    /** 拖动选色时回调（带 alpha 的 ARGB，这里 alpha 固定为 255） */
    var onColorChanged: ((Int) -> Unit)? = null

    var color: Int
        get() = Color.HSVToColor(hsv)
        set(value) = setColor(value, false)

    /** [notify] 为 false 时只更新界面，不回调（用于外部回填颜色，避免死循环） */
    fun setColor(value: Int, notify: Boolean = false) {
        Color.colorToHSV(value, hsv)
        invalidate()
        if (notify) onColorChanged?.invoke(color)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val left = paddingLeft.toFloat()
        val right = (width - paddingRight).toFloat()
        val top = paddingTop.toFloat()
        val svBottom = (height - paddingBottom - hueHeight - gap).coerceAtLeast(top + dp(40f))
        svRect.set(left, top, right, svBottom)
        hueRect.set(left, svBottom + gap, right, svBottom + gap + hueHeight)
    }

    override fun onDraw(canvas: Canvas) {
        val radius = dp(10f)

        // 饱和度 / 明度面板：先铺当前色相，再横向叠白、纵向叠黑
        fill.shader = null
        fill.color = Color.HSVToColor(floatArrayOf(hsv[0], 1f, 1f))
        canvas.drawRoundRect(svRect, radius, radius, fill)

        shade.shader = LinearGradient(
            svRect.left, 0f, svRect.right, 0f,
            Color.WHITE, 0x00FFFFFF, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(svRect, radius, radius, shade)

        shade.shader = LinearGradient(
            0f, svRect.top, 0f, svRect.bottom,
            0x00000000, 0xFF000000.toInt(), Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(svRect, radius, radius, shade)
        shade.shader = null

        // 色相条
        huePaint.shader = LinearGradient(
            hueRect.left, 0f, hueRect.right, 0f,
            hueColors, null, Shader.TileMode.CLAMP
        )
        canvas.drawRoundRect(hueRect, hueHeight / 2f, hueHeight / 2f, huePaint)
        huePaint.shader = null

        // 两个拇指（贴边时往里收一点，避免圆形被裁成半圆）
        val r = dp(8f)
        val cx = (svRect.left + hsv[1] * svRect.width()).coerceIn(svRect.left + r, svRect.right - r)
        val cy = (svRect.top + (1f - hsv[2]) * svRect.height()).coerceIn(svRect.top + r, svRect.bottom - r)
        canvas.drawCircle(cx, cy, r, thumbWhite)
        canvas.drawCircle(cx, cy, r, thumbRing)

        val hx = (hueRect.left + hsv[0] / 360f * hueRect.width())
            .coerceIn(hueRect.left + dp(9f), hueRect.right - dp(9f))
        canvas.drawCircle(hx, hueRect.centerY(), dp(9f), thumbWhite)
        canvas.drawCircle(hx, hueRect.centerY(), dp(9f), thumbRing)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                pick(event.x, event.y)
            }

            MotionEvent.ACTION_MOVE -> pick(event.x, event.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                parent?.requestDisallowInterceptTouchEvent(false)
        }
        return true
    }

    private fun pick(x: Float, y: Float) {
        when {
            y <= svRect.bottom -> {
                hsv[1] = ((x - svRect.left) / svRect.width()).coerceIn(0f, 1f)
                hsv[2] = (1f - (y - svRect.top) / svRect.height()).coerceIn(0f, 1f)
            }

            y >= hueRect.top - gap / 2f -> {
                hsv[0] = ((x - hueRect.left) / hueRect.width()).coerceIn(0f, 1f) * 360f
            }

            else -> return
        }
        invalidate()
        onColorChanged?.invoke(color)
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density
}
