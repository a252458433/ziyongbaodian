package com.ziyongbaodian.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Point
import android.graphics.Rect
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.view.ContextThemeWrapper
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Prefs
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.databinding.ViewFloatBallBinding
import com.ziyongbaodian.databinding.ViewFloatCardBinding
import com.ziyongbaodian.databinding.ViewFloatRangeBinding
import com.ziyongbaodian.search.ScreenOptions
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/** 悬浮答案卡片 / 收起后的小球 / 检测范围框 */
class FloatWindow(private val service: AccessibilityService) {

    private val wm = service.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val ctx = ContextThemeWrapper(service, R.style.Theme_ZiyongBaodian)

    private var card: ViewFloatCardBinding? = null
    private var ball: ViewFloatBallBinding? = null
    private var range: ViewFloatRangeBinding? = null

    private var cardParams: WindowManager.LayoutParams? = null
    private var ballParams: WindowManager.LayoutParams? = null
    private var rangeParams: WindowManager.LayoutParams? = null

    private var buttonsHidden = false
    private var cardHiddenForRange = false
    private var currentQuestion: Question? = null
    private var currentQuery: String = ""

    /**
     * 真实屏幕尺寸（把状态栏 / 导航条算进去）。
     * resources.displayMetrics 给的是「应用可用区域」，比真机屏幕矮一条导航条
     * （本机 2154 vs 2280），拿它当拖动上限，窗口就会永远停在导航条上方一点、
     * 到不了屏幕最底下。这里取 displayMetrics 与真实尺寸的较大值，两种口径都覆盖。
     */
    private val realSize: Point by lazy {
        val dm = service.resources.displayMetrics
        val size = Point(dm.widthPixels, dm.heightPixels)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val b = wm.currentWindowMetrics.bounds
                size.x = max(size.x, b.width())
                size.y = max(size.y, b.height())
            } else {
                val real = Point()
                @Suppress("DEPRECATION")
                wm.defaultDisplay.getRealSize(real)
                size.x = max(size.x, real.x)
                size.y = max(size.y, real.y)
            }
        }
        size
    }

    private val screenW: Int get() = realSize.x
    private val screenH: Int get() = realSize.y
    private val density: Float get() = service.resources.displayMetrics.density

    // ---------------- 窗口 ----------------

    fun attach() {
        if (card != null || ball != null) return
        buttonsHidden = Prefs.hideButtons
        addCard()
    }

    fun detach() {
        card?.let { runCatching { wm.removeView(it.root) } }
        ball?.let { runCatching { wm.removeView(it.root) } }
        range?.let { runCatching { wm.removeView(it.root) } }
        card = null
        ball = null
        range = null
        cardParams = null
        ballParams = null
        rangeParams = null
    }

    private fun baseFlags(): Int =
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    private fun newParams(w: Int, h: Int, x: Int, y: Int): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1)
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val p = WindowManager.LayoutParams(w, h, type, baseFlags(), PixelFormat.TRANSLUCENT)
        p.gravity = Gravity.TOP or Gravity.START
        p.x = x
        p.y = y
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            p.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        return p
    }

    private fun addCard() {
        if (card != null) return
        val b = ViewFloatCardBinding.inflate(LayoutInflater.from(ctx))
        // 保存的是上一次的像素尺寸，换设备或改了系统显示大小/分辨率后可能超出屏幕，
        // 这里统一夹到当前屏幕范围内，保证任何机型上卡片都能完整显示
        val maxW = (screenW - (8 * density).toInt()).coerceAtLeast(minW())
        val maxH = (screenH - (8 * density).toInt()).coerceAtLeast(minH())
        val w = if (Prefs.floatW > 0) Prefs.floatW.coerceIn(minW(), maxW)
        else (screenW * 0.64f).toInt().coerceIn(minW(), maxW)
        val h = if (Prefs.floatH > 0) Prefs.floatH.coerceIn(minH(), maxH)
        else (screenH * 0.28f).toInt().coerceIn(minH(), maxH)
        val x = Prefs.floatX.coerceIn(0, max(0, screenW - w))
        val y = Prefs.floatY.coerceIn(0, max(0, screenH - h))
        val p = newParams(w, h, x, y)
        cardParams = p
        card = b
        applyTheme()
        bindCardActions(b, p)
        // 恢复原有题目，没有题才显示空闲提示
        val q = currentQuestion
        if (q != null) renderQuestion(q) else showIdle()
        runCatching { wm.addView(b.root, p) }
    }

    private fun saveGeometry() {
        val p = cardParams ?: return
        Prefs.floatX = p.x
        Prefs.floatY = p.y
        Prefs.floatW = p.width
        Prefs.floatH = p.height
    }

    private fun removeCard() {
        card?.let { runCatching { wm.removeView(it.root) } }
        card = null
    }

    private fun addBall() {
        if (ball != null) return
        val b = ViewFloatBallBinding.inflate(LayoutInflater.from(ctx))
        val size = (Prefs.ballSize * density).toInt()
        // 小球位置与卡片位置分开存：没拖过小球（ball_x/ball_y 还没写入）时落到卡片收起前的位置
        val defX = Prefs.floatX.coerceIn(0, max(0, screenW - size))
        val defY = Prefs.floatY.coerceIn(0, max(0, screenH - size))
        val x = if (Prefs.ballX >= 0) Prefs.ballX.coerceIn(0, max(0, screenW - size)) else defX
        val y = if (Prefs.ballY >= 0) Prefs.ballY.coerceIn(0, max(0, screenH - size)) else defY
        val p = newParams(size, size, x, y)
        ballParams = p
        ball = b
        b.root.setOnTouchListener(
            DragTouch(p, b.root, savePos = true, onTap = {
                removeBall()
                addCard()
            }, onLong = null, saveBall = true)
        )
        applyBallStyle()
        runCatching { wm.addView(b.root, p) }
    }

    /** 悬浮球（小爱心 / 小花朵 / 自定义文字）的透明度、大小与颜色：设置改动后即时生效 */
    private fun applyBallStyle() {
        val b = ball ?: return
        val p = ballParams ?: return
        val size = (Prefs.ballSize * density).toInt()
        when (Prefs.ballStyle) {
            Prefs.BALL_STYLE_TEXT -> {
                // 自定义文字 / 符号：白色阴影当描边光晕，任何壁纸上都看得清
                b.ballHalo.visibility = View.GONE
                b.ballIcon.visibility = View.GONE
                b.ballText.visibility = View.VISIBLE
                b.ballText.text = Prefs.ballText
                b.ballText.setTextSize(
                    TypedValue.COMPLEX_UNIT_PX,
                    (size * 0.52f).coerceAtLeast(14 * density)
                )
                b.ballText.setTextColor(Prefs.ballColor)
                b.ballText.setShadowLayer(size * 0.08f, 0f, 0f, 0xE6FFFFFF.toInt())
            }
            else -> {
                b.ballText.visibility = View.GONE
                b.ballHalo.visibility = View.VISIBLE
                b.ballIcon.visibility = View.VISIBLE
                val res = when (Prefs.ballStyle) {
                    Prefs.BALL_STYLE_FLOWER -> R.drawable.ic_flower
                    Prefs.BALL_STYLE_STAR -> R.drawable.ic_ball_star
                    Prefs.BALL_STYLE_CIRCLE -> R.drawable.ic_ball_circle
                    else -> R.drawable.ic_ball_heart
                }
                b.ballHalo.setImageResource(res)
                b.ballIcon.setImageResource(res)
                // 图形占球体的近九成，光晕比本体大一圈（白色描边，浅色壁纸上也能看清）
                val icon = (size * 0.88f).toInt().coerceAtLeast((16 * density).toInt())
                val halo = (icon * 1.18f).toInt()
                b.ballHalo.layoutParams =
                    android.widget.FrameLayout.LayoutParams(halo, halo, Gravity.CENTER)
                b.ballIcon.layoutParams =
                    android.widget.FrameLayout.LayoutParams(icon, icon, Gravity.CENTER)
                b.ballIcon.imageTintList = android.content.res.ColorStateList.valueOf(Prefs.ballColor)
                b.ballHalo.imageTintList = android.content.res.ColorStateList.valueOf(0xE6FFFFFF.toInt())
            }
        }
        if (p.width != size || p.height != size) {
            p.width = size
            p.height = size
            p.x = p.x.coerceIn(0, max(0, screenW - size))
            p.y = p.y.coerceIn(0, max(0, screenH - size))
            runCatching { wm.updateViewLayout(b.root, p) }
        }
        // 0% 也保留一点点可点性，避免完全看不见
        b.root.alpha = (Prefs.ballAlpha / 100f).coerceIn(0.08f, 1f)
    }

    fun refreshBall() = applyBallStyle()

    /** 恢复默认设置后：浮窗按默认位置与外观重建（当前题目保留，重新搜题时还在） */
    fun onSettingsReset() {
        if (ball != null) {
            removeBall()
            addBall()
        } else if (card != null) {
            removeCard()
            addCard()
        }
    }

    private fun removeBall() {
        ball?.let { runCatching { wm.removeView(it.root) } }
        ball = null
    }

    // ---------------- 内容 ----------------

    fun showIdle() {
        val b = card ?: return
        b.tvAnswerLabel.text = "答案"
        b.tvAnswer.setTextColor(idleColor())
        b.tvAnswer.text = "—"
        b.tvContent.text = when {
            !Bank.ready -> "题库初始化中，请稍等…"
            Bank.questions.isEmpty() ->
                "还没有题库。\n\n回到 App 首页导入题库（支持 txt / json / csv），导入后这里会自动开始识别。"
            else ->
                "题库已就绪（${Bank.questions.size} 道题）\n\n打开任意题目页面，让题目出现在屏幕中间，浮窗会自动识别并给出答案。"
        }
    }

    /** 保留接口：卡片已不显示状态文字 */
    fun setStatus(text: String) {
    }

    fun showNoMatch(query: String) {
        val b = card ?: return
        currentQuestion = null
        currentQuery = query
        b.tvAnswerLabel.text = "答案"
        b.tvAnswer.setTextColor(idleColor())
        b.tvAnswer.text = "未匹配到"
        val preview = if (query.length > 70) query.take(70) + "…" else query
        b.tvContent.text =
            "已识别：\n$preview\n\n本地题库里没有找到这道题。\n\n可以试试：\n· 拖动浮窗，让题干完整出现在屏幕上\n· 点左上角框选图标，把检测范围限定在题目区域\n· 导入包含这道题的题库"
    }

    fun showQuestion(q: Question, query: String) {
        currentQuestion = q
        currentQuery = query
        renderQuestion(q)
    }

    private fun renderQuestion(q: Question) {
        val b = card ?: return
        val dark = Prefs.floatDark
        val answerColor = Prefs.answerColor
        val normalColor = if (dark) Prefs.questionColor else 0xFF111827.toInt()

        // ---- 选项与答案：以屏幕实际显示为准 ----
        // 有些 App 会打乱选项顺序，题库里的「B」到屏幕上可能已经是「C」。
        // 这里从屏幕文字里解析出真实选项，用答案内容反查屏幕上的字母。
        val answerTexts = q.answerIndexes.mapNotNull { q.options.getOrNull(it) }
        val answerNorms = answerTexts.map { ScreenOptions.norm(it) }
        val bankNorms = q.options.map { ScreenOptions.norm(it) }
        val screen = ScreenOptions.parse(currentQuery).filter { it.text.isNotBlank() }
        val screenHits = screen.count { bankNorms.contains(ScreenOptions.norm(it.text)) }
        val screenAnswers =
            if (screen.size >= 2 && screenHits >= 2) {
                screen.filter { answerNorms.contains(ScreenOptions.norm(it.text)) }
            } else {
                emptyList()
            }
        val useScreen = answerNorms.isNotEmpty() &&
            screenAnswers.size == answerNorms.size &&
            screenHits >= 2

        val options: List<Pair<Char, String>> =
            if (useScreen) screen.map { it.label to it.text }
            else q.options.mapIndexed { i, t -> q.optionLabel(i)[0] to t }
        val answerLabels: String =
            if (useScreen) screenAnswers.joinToString("") { it.label.toString() }
            else q.answerLabel()
        val answerContent: String =
            if (useScreen) screenAnswers.joinToString("；") { it.text }
            else q.answerContent()

        // ---- 顶部答案区 ----
        b.tvAnswerLabel.text = "答案"
        b.tvAnswerLabel.setTextColor(if (dark) 0x8FFFFFFF.toInt() else 0xFF8A94A6.toInt())
        b.tvAnswer.setTextColor(answerColor)
        b.tvAnswer.text =
            if (answerContent.isBlank()) answerLabels else "$answerLabels　$answerContent"

        // ---- 下方题目与选项 ----
        val sb = StringBuilder()
        sb.append(q.q.trim()).append("\n")
        val spans = ArrayList<Pair<Int, Int>>()
        val answerSet = if (useScreen) screenAnswers.map { it.label }.toSet()
        else q.answerIndexes.map { q.optionLabel(it)[0] }.toSet()
        for ((label, text) in options) {
            sb.append("\n")
            val start = sb.length
            sb.append(label).append("  ").append(text)
            if (answerSet.contains(label)) spans.add(start to sb.length)
        }
        if (q.answerIndexes.isEmpty()) sb.append("\n\n答案：").append(q.ans)
        if (useScreen && answerLabels != q.answerLabel()) {
            sb.append("\n\n· 该应用打乱了选项顺序，已按屏幕上的顺序标出正确答案")
        }

        val sp = SpannableString(sb.toString())
        for ((start, end) in spans) {
            sp.setSpan(ForegroundColorSpan(answerColor), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            sp.setSpan(StyleSpan(Typeface.BOLD), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        b.tvContent.setTextColor(normalColor)
        b.tvContent.textSize = Prefs.textSize.toFloat()
        b.tvContent.text = sp
        b.scroll.scrollTo(0, 0)
    }

    private fun idleColor(): Int =
        if (Prefs.floatDark) 0x66FFFFFF else 0xFFB4BCC8.toInt()

    // ---------------- 主题 ----------------

    private fun applyTheme() {
        val b = card ?: return
        val dark = Prefs.floatDark
        b.root.setBackgroundResource(if (dark) R.drawable.bg_float_card else R.drawable.bg_float_card_light)
        // 背景单独一层透明度：调到 0 只剩文字浮在屏幕上，文字本身依旧清晰
        b.root.background?.mutate()?.alpha = (Prefs.bgAlpha * 255 / 100).coerceIn(0, 255)
        // 只给文字单独一层透明度：背景由上面那行控制，两者互不牵连，
        // 这样既能「只要文字不要背景」，也能把文字调淡避免挡屏（最低 5%）
        val textAlpha = (Prefs.textAlpha / 100f)
            .coerceIn(Prefs.MIN_TEXT_ALPHA / 100f, 1f)
        b.tvContent.alpha = textAlpha
        b.tvAnswer.alpha = textAlpha
        b.tvAnswerLabel.alpha = textAlpha
        val contentColor = if (dark) Prefs.questionColor else 0xFF111827.toInt()
        val btnText = if (dark) 0xB3FFFFFF.toInt() else 0xFF8A94A6.toInt()

        b.tvContent.setTextColor(contentColor)
        // 题目字号跟随设置（题干与选项统一）
        b.tvContent.textSize = Prefs.textSize.toFloat()
        b.tvAnswerLabel.setTextColor(if (dark) 0x8FFFFFFF.toInt() else 0xFF8A94A6.toInt())
        val tint = android.content.res.ColorStateList.valueOf(btnText)
        b.btnRange.imageTintList = tint
        b.btnMin.imageTintList = tint
        b.btnClose.imageTintList = tint
        b.resizeIcon.imageTintList = tint

        // 未匹配到题目时答案区是灰色的，重新渲染时再套用用户颜色
        if (currentQuestion == null) b.tvAnswer.setTextColor(idleColor())

        // 隐藏按钮时保留「框」（长按可恢复），只做淡出
        val vis = if (buttonsHidden) View.GONE else View.VISIBLE
        b.btnRange.visibility = View.VISIBLE
        b.btnRange.alpha = if (buttonsHidden) 0.3f else 1f
        b.btnMin.visibility = vis
        b.btnClose.visibility = vis
        b.resize.visibility = vis
    }

    /** 设置里改了颜色 / 主题后即时生效 */
    fun refreshTheme() {
        applyTheme()
        currentQuestion?.let { renderQuestion(it) }
    }

    // ---------------- 交互 ----------------

    private fun bindCardActions(b: ViewFloatCardBinding, p: WindowManager.LayoutParams) {
        b.btnClose.setOnClickListener { (service as? ScreenSearchService)?.closeSelf() }
        b.btnMin.setOnClickListener { minimize() }
        b.btnRange.setOnClickListener { openRange() }
        // 长按「框」：隐藏 / 显示其余按钮
        b.btnRange.setOnLongClickListener {
            toggleButtons()
            true
        }
        // 答案区占满顶栏，必须自己接管拖动
        b.answerBox.setOnTouchListener(
            DragTouch(p, b.root, savePos = true, onTap = null, onLong = null)
        )

        // 标题栏剩余空隙也可拖动
        b.header.setOnTouchListener(
            DragTouch(p, b.root, savePos = true, onTap = null, onLong = null)
        )
        b.resize.setOnTouchListener(ResizeTouch(p, b.root, minW(), minH()))
    }

    /** 长按「框」切换按钮显隐，长按后仍能点「框」恢复 */
    private fun toggleButtons() {
        buttonsHidden = !buttonsHidden
        Prefs.hideButtons = buttonsHidden
        applyTheme()
        Toast.makeText(
            service,
            if (buttonsHidden) "按钮已隐藏，长按左上角框选图标可恢复" else "按钮已显示",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun minimize() {
        val p = cardParams
        if (p != null) {
            Prefs.floatW = p.width
            Prefs.floatH = p.height
            Prefs.floatX = p.x
            Prefs.floatY = p.y
        }
        removeCard()
        addBall()
    }

    /** 最小尺寸：只保留顶栏 + 底栏可用，缩放不再做明显限制 */
    private fun minW(): Int = (72 * density).toInt()
    private fun minH(): Int = (68 * density).toInt()

    /** 拖动 + 点击 / 长按 */
    private inner class DragTouch(
        private val p: WindowManager.LayoutParams,
        private val root: View,
        private val savePos: Boolean,
        private val onTap: (() -> Unit)?,
        private val onLong: (() -> Unit)?,
        private val onMove: (() -> Unit)? = null,
        // 小球专用：松手时把位置写进 ball_x/ball_y，不碰卡片的 float_x/float_y，
        // 这样「收起 → 随便拖小球 → 展开」时卡片总能回到收起前的位置
        private val saveBall: Boolean = false
    ) : View.OnTouchListener {
        private val slop = ViewConfiguration.get(service).scaledTouchSlop
        private var downX = 0f
        private var downY = 0f
        private var originX = 0
        private var originY = 0
        private var moved = false
        private var longFired = false
        private val longRunnable = Runnable {
            if (!moved) {
                longFired = true
                onLong?.invoke()
            }
        }

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX
                    downY = e.rawY
                    originX = p.x
                    originY = p.y
                    moved = false
                    longFired = false
                    v.postDelayed(longRunnable, 520)
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (!moved && (abs(dx) > slop || abs(dy) > slop)) {
                        moved = true
                        v.removeCallbacks(longRunnable)
                    }
                    if (moved) {
                        p.x = (originX + dx).toInt().coerceIn(-6, max(0, screenW - p.width + 6))
                        p.y = (originY + dy).toInt().coerceIn(0, max(0, screenH - p.height))
                        runCatching { wm.updateViewLayout(root, p) }
                        onMove?.invoke()
                    }
                }
                MotionEvent.ACTION_UP -> {
                    v.removeCallbacks(longRunnable)
                    if (moved && savePos) {
                        if (saveBall) {
                            Prefs.ballX = p.x
                            Prefs.ballY = p.y
                        } else {
                            Prefs.floatX = p.x
                            Prefs.floatY = p.y
                        }
                    }
                    if (!moved && !longFired) onTap?.invoke()
                }
                MotionEvent.ACTION_CANCEL -> v.removeCallbacks(longRunnable)
            }
            return true
        }
    }

    private inner class ResizeTouch(
        private val p: WindowManager.LayoutParams,
        private val root: View,
        private val minW: Int,
        private val minH: Int,
        private val saveSize: Boolean = true,
        private val onMove: (() -> Unit)? = null
    ) : View.OnTouchListener {
        // 按下时手指距窗口右下角的距离，缩放过程中保持不变
        private var rightGap = 0f
        private var bottomGap = 0f

        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    rightGap = p.width - (e.rawX - p.x)
                    bottomGap = p.height - (e.rawY - p.y)
                }
                MotionEvent.ACTION_MOVE -> {
                    // 用「手指位置 + 固定余量」算尺寸，窗口右下角始终跟手，
                    // 避免缩小过程中手指跑到窗口外导致手势被系统取消
                    p.width = ((e.rawX - p.x) + rightGap).toInt()
                        .coerceIn(minW, max(minW, screenW - p.x))
                    p.height = ((e.rawY - p.y) + bottomGap).toInt()
                        .coerceIn(minH, max(minH, screenH - p.y))
                    runCatching { wm.updateViewLayout(root, p) }
                    onMove?.invoke()
                }
                MotionEvent.ACTION_UP -> {
                    if (saveSize) {
                        Prefs.floatW = p.width
                        Prefs.floatH = p.height
                    }
                    onMove?.invoke()
                }
            }
            return true
        }
    }

    // ---------------- 范围 ----------------

    private fun openRange() {
        if (range != null) return
        // 先收起答案卡片，否则会挡住选框
        if (card != null) {
            saveGeometry()
            card?.let { runCatching { wm.removeView(it.root) } }
            card = null
            cardParams = null
            cardHiddenForRange = true
        }
        val b = ViewFloatRangeBinding.inflate(LayoutInflater.from(ctx))
        // 选区只保留边框、中间全透明，边框颜色随应用主题
        // （代码生成而非 bg_range.xml，方便按主题主色即时着色）
        val border = com.ziyongbaodian.util.Themes.color(service)
        val gd = android.graphics.drawable.GradientDrawable().apply {
            shape = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 6 * density
            setColor(android.graphics.Color.TRANSPARENT)
            setStroke((2 * density).toInt(), border)
        }
        b.root.background = gd
        // 工具条「完成」胶囊同步主题色（默认模板色是旧品牌蓝）
        b.btnDone.background.mutate().setTint(border)
        val preset = Prefs.range()
        val w = (preset?.width() ?: (screenW - (80 * density).toInt()))
            .coerceIn((120 * density).toInt().coerceAtMost(screenW), screenW)
        val h = (preset?.height() ?: (screenH / 3))
            .coerceIn((100 * density).toInt().coerceAtMost(screenH), screenH)
        val x = (preset?.left ?: (40 * density).toInt()).coerceIn(0, max(0, screenW - w))
        val y = (preset?.top ?: (screenH / 4)).coerceIn(0, max(0, screenH - h))
        val p = newParams(w, h, x, y)
        rangeParams = p
        range = b

        val updateSize = {
            b.tvRangeSize.text = "${p.width} × ${p.height}"
        }
        updateSize()

        // 整块框内任意位置都能拖动（完成/取消按钮、右下角缩放柄除外）
        b.root.setOnTouchListener(
            DragTouch(p, b.root, savePos = false, onTap = null, onLong = null, onMove = updateSize)
        )
        b.rangeHandle.setOnTouchListener(
            ResizeTouch(
                p, b.root,
                (200 * density).toInt(), (160 * density).toInt(),
                saveSize = false,
                onMove = updateSize
            )
        )
        b.btnDone.setOnClickListener {
            Prefs.setRange(Rect(p.x, p.y, p.x + p.width, p.y + p.height))
            closeRange()
            setStatus("已限定检测范围 ${p.width}×${p.height}")
            Toast.makeText(service, "已保存检测范围 ${p.width}×${p.height}", Toast.LENGTH_SHORT).show()
        }
        b.btnCancel.setOnClickListener { closeRange() }
        runCatching { wm.addView(b.root, p) }
    }

    private fun closeRange() {
        range?.let { runCatching { wm.removeView(it.root) } }
        range = null
        rangeParams = null
        if (cardHiddenForRange) {
            cardHiddenForRange = false
            if (ball == null) addCard()
        }
    }

    fun isRangeOpen(): Boolean = range != null
}
