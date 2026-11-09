package com.ziyongbaodian.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.SharedPreferences
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Prefs
import java.util.concurrent.Executors

/**
 * 无障碍服务：读取屏幕文字 → 本地题库检索 → 浮窗显示答案。
 * 不模拟点击、不联网、不记录用户内容。
 */
class ScreenSearchService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor()
    private var float: FloatWindow? = null

    private var lastEventAt = 0L
    private var lastQuery = ""
    private var lastQid = -1L
    private var busy = false
    private var tick = 0

    private var chooseId = -1
    private var nextSkip = false
    private var alwaysSkip = false

    private val prefListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            Prefs.KEY_FLOAT_DARK, Prefs.KEY_TEXT_ALPHA, Prefs.KEY_BG_ALPHA, Prefs.KEY_HIDE_BUTTONS,
            Prefs.KEY_ANSWER_COLOR, Prefs.KEY_QUESTION_COLOR, Prefs.KEY_TEXT_SIZE ->
                float?.refreshTheme()
            Prefs.KEY_BALL_ALPHA, Prefs.KEY_BALL_SIZE, Prefs.KEY_BALL_COLOR,
            Prefs.KEY_BALL_STYLE, Prefs.KEY_BALL_TEXT ->
                float?.refreshBall()
            Prefs.KEY_RANGE_L, Prefs.KEY_RANGE_T, Prefs.KEY_RANGE_R, Prefs.KEY_RANGE_B ->
                if (!Prefs.hasRange()) float?.setStatus("全屏检测中")
            Prefs.KEY_RESET_TS ->
                float?.onSettingsReset()
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        configure()
        Prefs.raw().registerOnSharedPreferenceChangeListener(prefListener)
        float = FloatWindow(this).also { it.attach() }
        handler.post(detectLoop)
    }

    private fun configure() {
        try {
            val info = serviceInfo ?: AccessibilityServiceInfo()
            info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
            info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            info.notificationTimeout = 80L
            info.flags = info.flags or
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS or
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
            serviceInfo = info
        } catch (_: Exception) {
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val e = event ?: return
        lastEventAt = SystemClock.uptimeMillis()
        if (e.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            // 切换应用时清掉上一题的缓存，避免显示旧答案
            lastQuery = ""
            lastQid = -1L
        }
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        cleanup()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
    }

    private fun cleanup() {
        handler.removeCallbacksAndMessages(null)
        runCatching { Prefs.raw().unregisterOnSharedPreferenceChangeListener(prefListener) }
        float?.detach()
        float = null
        if (instance === this) instance = null
    }

    fun closeSelf() {
        cleanup()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            disableSelf()
        } else {
            stopSelf()
        }
    }

    // ---------------- 检测循环 ----------------

    private val detectLoop = object : Runnable {
        override fun run() {
            try {
                // 页面上有交互时按 280ms 快速检测；静止页面（如试卷、图片题）降到 ~840ms
                // 也要继续扫，否则静止页永远等不到事件，浮窗会一直空着。
                val idle = SystemClock.uptimeMillis() - lastEventAt
                tick++
                if (!busy && (idle < 1200 || tick % 3 == 0)) detectOnce()
            } catch (_: Throwable) {
            }
            handler.postDelayed(this, 280)
        }
    }

    private fun detectOnce() {
        val root = rootInActiveWindow ?: return
        val range = Prefs.range()
        chooseId = -1
        nextSkip = false
        alwaysSkip = false

        val sb = StringBuilder()
        collect(root, range, sb, 0, intArrayOf(0))
        val raw = sb.toString().trim()
        if (raw.length < 8) return
        val query = if (raw.length > 1200) raw.substring(0, 1200) else raw
        if (query == lastQuery) return
        lastQuery = query

        float?.setStatus("识别中…")
        busy = true
        executor.execute {
            val hit = try {
                Bank.best(query)
            } catch (t: Throwable) {
                null
            }
            handler.post {
                busy = false
                val f = float ?: return@post
                if (hit == null) {
                    f.showNoMatch(query)
                } else {
                    if (hit.question.id != lastQid) {
                        lastQid = hit.question.id
                        Db.addHistory(query, hit.question.id)
                    }
                    f.showQuestion(hit.question, query)
                }
            }
        }
    }

    private fun collect(
        node: AccessibilityNodeInfo?,
        range: Rect?,
        sb: StringBuilder,
        depth: Int,
        counter: IntArray
    ) {
        if (node == null || depth > 30 || counter[0] > 900) return
        counter[0]++
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && node.isVisibleToUser) {
            val r = Rect()
            node.getBoundsInScreen(r)
            val inRange = range == null || range.contains(r.centerX(), r.centerY())
            if (inRange && policy(text) && !filterText(text)) {
                if (sb.isNotEmpty()) sb.append('\n')
                sb.append(text)
            }
        }
        val count = node.childCount
        for (i in 0 until count) {
            collect(node.getChild(i), range, sb, depth + 1, counter)
        }
    }

    /** 过滤 "A"、"B" 这类型号标记与"出题：""来源："这类页脚 */
    private fun policy(text: String): Boolean {
        if (text.startsWith("出题：") || text.startsWith("推荐：") || text.startsWith("来源：")) return false
        if (alwaysSkip) return false
        if (nextSkip) {
            nextSkip = false
            return true
        }
        val t = text.trim()
        if (t.isEmpty()) return false
        val c = t.uppercase()[0]
        val idx = c - 'A'
        return if (idx in 0..25) {
            if (idx == chooseId + 1 || idx == chooseId) {
                chooseId = idx
                if (t.length == 1) nextSkip = true
                true
            } else {
                if (chooseId >= 1) {
                    alwaysSkip = true
                    false
                } else {
                    chooseId = -1
                    true
                }
            }
        } else {
            true
        }
    }

    private fun filterText(text: String): Boolean {
        val t = text.trim()
        return t.isEmpty() || "javascript:;" == t
    }

    companion object {
        @Volatile
        var instance: ScreenSearchService? = null
            private set

        fun isRunning(): Boolean = instance != null
    }
}
