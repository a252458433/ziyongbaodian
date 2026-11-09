package com.ziyongbaodian.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Prefs
import com.ziyongbaodian.databinding.ActivitySettingsBinding
import com.ziyongbaodian.databinding.ViewSettingRowBinding
import com.ziyongbaodian.util.Themes
import com.ziyongbaodian.util.appVersion
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast
import com.ziyongbaodian.widget.ColorPickerView

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding

    /** 折叠分组的展开状态，页面重建时保留 */
    private var floatExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.tvFooterVersion.text = "${getString(Themes.current().appNameRes)} ${appVersion()}"

        setupGroups()
        setupFloatRows()
        setupPracticeRows()
        setupBankRows()
        setupOtherRows()
        refreshSummaries()
    }

    // ==================== 折叠分组 ====================

    private fun setupGroups() {
        bindGroup(binding.groupFloatHeader, binding.groupFloatBody, binding.arrowFloat, floatExpanded) {
            floatExpanded = it
        }
    }

    private fun bindGroup(
        header: View,
        body: View,
        arrow: ImageView,
        initial: Boolean,
        onToggle: (Boolean) -> Unit
    ) {
        fun render(open: Boolean) {
            body.visibility = if (open) View.VISIBLE else View.GONE
            arrow.animate().rotation(if (open) 180f else 0f).setDuration(150).start()
        }
        var open = initial
        render(open)
        header.setOnClickListener {
            open = !open
            render(open)
            onToggle(open)
        }
    }

    private fun refreshSummaries() {
        val dark = if (Prefs.floatDark) "深色" else "浅色"
        binding.tvFloatSummary.text =
            "$dark · 文字 ${Prefs.textAlpha}% · 背景 ${Prefs.bgAlpha}% · 字号 ${Prefs.textSize}sp"
        binding.rowRebuild.rowValue.text = "${Bank.questions.size} 道题"
    }

    // ==================== 悬浮窗 ====================

    private fun setupFloatRows() {
        binding.swDark.rowTitle.text = "深色答案卡片"
        binding.swDark.rowDesc.text = "浮窗使用深色背景，夜里看题更省眼"
        binding.swDark.rowSwitch.isChecked = Prefs.floatDark
        binding.swDark.rowSwitch.setOnCheckedChangeListener { _, checked ->
            Prefs.floatDark = checked
            refreshSummaries()
        }

        // 文字透明度：只淡出题干 / 选项 / 答案，背景由下面那个滑块单独控制
        binding.sliderTextAlpha.rowTitle.text = "文字不透明度"
        binding.sliderTextAlpha.rowSeek.max = Prefs.MAX_TEXT_ALPHA - Prefs.MIN_TEXT_ALPHA
        binding.sliderTextAlpha.rowSeek.progress = (Prefs.textAlpha - Prefs.MIN_TEXT_ALPHA)
            .coerceIn(0, Prefs.MAX_TEXT_ALPHA - Prefs.MIN_TEXT_ALPHA)
        binding.sliderTextAlpha.rowValue.text = "${Prefs.textAlpha}%"
        binding.sliderTextAlpha.rowSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只响应用户拖动，避免界面重建恢复进度时反向覆盖已保存的值
                if (!fromUser) return
                val v = Prefs.MIN_TEXT_ALPHA + progress
                binding.sliderTextAlpha.rowValue.text = "$v%"
                Prefs.textAlpha = v
                refreshSummaries()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 背景透明度：0% 时背景完全消失，只剩文字浮在屏幕上（默认 60%）
        binding.sliderBgAlpha.rowTitle.text = "背景不透明度"
        binding.sliderBgAlpha.rowSeek.max = Prefs.MAX_BG_ALPHA - Prefs.MIN_BG_ALPHA
        binding.sliderBgAlpha.rowSeek.progress =
            (Prefs.bgAlpha - Prefs.MIN_BG_ALPHA).coerceIn(0, Prefs.MAX_BG_ALPHA - Prefs.MIN_BG_ALPHA)
        binding.sliderBgAlpha.rowValue.text = "${Prefs.bgAlpha}%"
        binding.sliderBgAlpha.rowSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只响应用户拖动，避免界面重建恢复进度时反向覆盖已保存的值
                if (!fromUser) return
                val v = Prefs.MIN_BG_ALPHA + progress
                binding.sliderBgAlpha.rowValue.text = "$v%"
                Prefs.bgAlpha = v
                refreshSummaries()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 题目字号
        binding.sliderTextSize.rowTitle.text = "题目字号"
        binding.sliderTextSize.rowSeek.max = Prefs.MAX_TEXT_SIZE - Prefs.MIN_TEXT_SIZE
        binding.sliderTextSize.rowSeek.progress =
            (Prefs.textSize - Prefs.MIN_TEXT_SIZE).coerceIn(0, Prefs.MAX_TEXT_SIZE - Prefs.MIN_TEXT_SIZE)
        binding.sliderTextSize.rowValue.text = "${Prefs.textSize}sp"
        binding.sliderTextSize.rowSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只响应用户拖动，避免界面重建恢复进度时反向覆盖已保存的值
                if (!fromUser) return
                val v = Prefs.MIN_TEXT_SIZE + progress
                binding.sliderTextSize.rowValue.text = "${v}sp"
                Prefs.textSize = v
                refreshSummaries()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 悬浮球大小
        binding.sliderBallSize.rowTitle.text = "悬浮球大小"
        binding.sliderBallSize.rowSeek.max = Prefs.MAX_BALL_SIZE - Prefs.MIN_BALL_SIZE
        binding.sliderBallSize.rowSeek.progress =
            (Prefs.ballSize - Prefs.MIN_BALL_SIZE).coerceIn(0, Prefs.MAX_BALL_SIZE - Prefs.MIN_BALL_SIZE)
        binding.sliderBallSize.rowValue.text = "${Prefs.ballSize}dp"
        binding.sliderBallSize.rowSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只响应用户拖动，避免界面重建恢复进度时反向覆盖已保存的值
                if (!fromUser) return
                val v = Prefs.MIN_BALL_SIZE + progress
                binding.sliderBallSize.rowValue.text = "${v}dp"
                Prefs.ballSize = v
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 悬浮球透明度：只影响收起后的小球，不影响答案卡片
        binding.sliderBallAlpha.rowTitle.text = "悬浮球不透明度"
        binding.sliderBallAlpha.rowSeek.max = Prefs.MAX_BALL_ALPHA - Prefs.MIN_BALL_ALPHA
        binding.sliderBallAlpha.rowSeek.progress =
            (Prefs.ballAlpha - Prefs.MIN_BALL_ALPHA).coerceIn(0, Prefs.MAX_BALL_ALPHA - Prefs.MIN_BALL_ALPHA)
        binding.sliderBallAlpha.rowValue.text = "${Prefs.ballAlpha}%"
        binding.sliderBallAlpha.rowSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                // 只响应用户拖动，避免界面重建恢复进度时反向覆盖已保存的值
                if (!fromUser) return
                val v = Prefs.MIN_BALL_ALPHA + progress
                binding.sliderBallAlpha.rowValue.text = "$v%"
                Prefs.ballAlpha = v
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // 悬浮球样式：小爱心 / 小花朵 / 五角星 / 圆形 / 自定义文字或符号
        setupRow(
            binding.rowBallStyle,
            "悬浮球样式",
            "小爱心、小花朵、五角星、圆形，或自定义符号",
            ballStyleLabel(),
            true
        ) {
            showBallStyleDialog()
        }

        // 悬浮球的颜色
        setupRow(
            binding.rowBallColor,
            "悬浮球颜色",
            "悬浮球的颜色，可自由调色",
            colorLabel(Prefs.BALL_COLORS, Prefs.ballColor),
            true
        ) {
            showColorPicker("悬浮球颜色", Prefs.BALL_COLORS, Prefs.ballColor) { v ->
                Prefs.ballColor = v
                binding.rowBallColor.rowValue.text = colorLabel(Prefs.BALL_COLORS, v)
            }
        }

        // 题目 / 选项颜色
        setupRow(
            binding.rowQuestionColor,
            "题目文字颜色",
            "浮窗里题干与选项的颜色，可自由调色",
            colorLabel(Prefs.QUESTION_COLORS, Prefs.questionColor),
            true
        ) {
            showColorPicker("题目文字颜色", Prefs.QUESTION_COLORS, Prefs.questionColor) { v ->
                Prefs.questionColor = v
                binding.rowQuestionColor.rowValue.text = colorLabel(Prefs.QUESTION_COLORS, v)
            }
        }

        // 答案颜色
        setupRow(
            binding.rowAnswerColor,
            "答案文字颜色",
            "浮窗顶部答案与正确选项的颜色，可自由调色",
            colorLabel(Prefs.ANSWER_COLORS, Prefs.answerColor),
            true
        ) {
            showColorPicker("答案文字颜色", Prefs.ANSWER_COLORS, Prefs.answerColor) { v ->
                Prefs.answerColor = v
                binding.rowAnswerColor.rowValue.text = colorLabel(Prefs.ANSWER_COLORS, v)
            }
        }

        // 检测范围
        setupRow(binding.rowRange, "题目检测范围", "默认全屏读取；可限定只读屏幕局部区域", rangeText(), true) {
            showRangeDialog()
        }
        setupRow(
            binding.rowFloatTip,
            "浮窗按钮隐藏",
            "长按浮窗左上角的框选图标，可隐藏或显示按钮",
            if (Prefs.hideButtons) "已隐藏" else "显示中",
            false
        ) {
            toast("在浮窗左上角框选图标上长按即可切换")
        }
    }

    /** 悬浮球样式行右侧的摘要 */
    private fun ballStyleLabel(): String = when (Prefs.ballStyle) {
        Prefs.BALL_STYLE_FLOWER -> "小花朵"
        Prefs.BALL_STYLE_STAR -> "五角星"
        Prefs.BALL_STYLE_CIRCLE -> "圆形"
        Prefs.BALL_STYLE_TEXT -> "自定义 ${Prefs.ballText}"
        else -> "小爱心"
    }

    private fun refreshBallStyleRow() {
        binding.rowBallStyle.rowValue.text = ballStyleLabel()
    }

    /** 五选一样式对话框，选「自定义」再弹文字输入 */
    private fun showBallStyleDialog() {
        val styles = listOf(
            "小爱心" to Prefs.BALL_STYLE_HEART,
            "小花朵" to Prefs.BALL_STYLE_FLOWER,
            "五角星" to Prefs.BALL_STYLE_STAR,
            "圆形" to Prefs.BALL_STYLE_CIRCLE,
            "自定义文字 / 符号" to Prefs.BALL_STYLE_TEXT
        )
        val current = styles.indexOfFirst { it.second == Prefs.ballStyle }.coerceAtLeast(0)
        MaterialAlertDialogBuilder(this)
            .setTitle("悬浮球样式")
            .setSingleChoiceItems(Array(styles.size) { styles[it].first }, current) { dialog, which ->
                dialog.dismiss()
                val style = styles[which].second
                if (style == Prefs.BALL_STYLE_TEXT) {
                    showBallTextInput()
                } else {
                    Prefs.ballStyle = style
                    refreshBallStyleRow()
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /** 自定义文字 / 符号输入，确定后才落库 */
    private fun showBallTextInput() {
        val d = resources.displayMetrics.density
        val frame = FrameLayout(this).apply {
            setPadding((16 * d).toInt(), (8 * d).toInt(), (16 * d).toInt(), 0)
        }
        val input = EditText(this).apply {
            setText(Prefs.ballText)
            hint = "如 ★ ☀ ☾ 或一个字"
            filters = arrayOf(android.text.InputFilter.LengthFilter(Prefs.MAX_BALL_TEXT))
            setSingleLine(true)
            imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
            setOnEditorActionListener { v, _, _ ->
                (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.hideSoftInputFromWindow(v.windowToken, 0)
                v.clearFocus()
                true
            }
        }
        frame.addView(
            input,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        )
        val dlg = MaterialAlertDialogBuilder(this)
            .setTitle("自定义文字 / 符号")
            .setView(frame)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                Prefs.ballText = input.text.toString()
                Prefs.ballStyle = Prefs.BALL_STYLE_TEXT
                refreshBallStyleRow()
                toast("悬浮球样式已更新")
            }
            .create()
        // 键盘弹出时压缩对话框，别把「确定」顶出屏幕
        dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dlg.show()
    }

    // ==================== 练习 ====================

    private fun setupPracticeRows() {
        // 题库管理：放在练习与题库卡片最上方
        setupRow(
            binding.rowTikuManage,
            "题库管理",
            "导入、启用停用与删除题库",
            "${Bank.tikus.size} 个题库",
            true
        ) {
            startActivity(Intent(this, TikuActivity::class.java))
        }

        setupRow(
            binding.rowWrongRemove,
            "错题自动移出",
            "错题连续答对几次后自动移出错题本",
            "${Prefs.wrongRemove} 次",
            true
        ) {
            showWrongRemoveDialog()
        }
    }

    // ==================== 题库 ====================

    private fun setupBankRows() {
        setupRow(
            binding.rowRebuild,
            "重新加载题库",
            "重建检索索引，导入或删除题库后可用",
            "${Bank.questions.size} 道题",
            true
        ) {
            Bank.reload {
                toast("题库已重新加载，共 ${Bank.questions.size} 道题")
                refreshSummaries()
            }
        }
        setupRow(binding.rowClear, "清空全部数据", "删除所有题库、错题、收藏与搜题记录", null, true) {
            MaterialAlertDialogBuilder(this)
                .setTitle("清空全部数据")
                .setMessage("将删除本机保存的全部题库与练习记录，且无法恢复。确定继续吗？")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空") { _, _ ->
                    Db.clearAll()
                    Bank.reload {
                        toast("已清空")
                        refreshSummaries()
                    }
                }
                .show()
        }
    }

    // ==================== 其他 ====================

    private fun setupOtherRows() {
        // 主题设置：双主题（漫画粉 / 石墨灰），选中即保存索引并重建本页；
        // 其他页面由 BaseActivity.onResume 检测变更后自动 recreate
        setupRow(
            binding.rowTheme, "主题",
            "App 全局配色", Themes.name(), true
        ) {
            val names = Themes.ALL.map { it.name }.toTypedArray()
            MaterialAlertDialogBuilder(this)
                .setTitle("主题")
                .setSingleChoiceItems(names, Themes.index()) { dialog, which ->
                    if (which != Themes.index()) {
                        Prefs.setThemeIndex(which)
                        Themes.applyLauncherAlias(this)  // 桌面名称/图标随主题联动
                        Themes.syncFloatColors(this)     // 悬浮窗答案色 + 悬浮球色随主题联动
                        toast("已切换主题：${names[which]}")
                        recreate()
                    }
                    dialog.dismiss()
                }
                .setNegativeButton("关闭", null)
                .show()
        }
        setupRow(binding.rowGuide, "使用指引", null, null, true) {
            startActivity(Intent(this, GuideActivity::class.java))
        }
        setupRow(binding.rowPerm, "权限说明", null, null, true) {
            MaterialAlertDialogBuilder(this)
                .setTitle("权限说明")
                .setMessage(
                    "【无障碍服务】读取屏幕文字用于本地题库匹配，不记录、不上传、不点击。\n\n" +
                        "【悬浮窗】显示答案卡片。\n\n【存储】读取你选择的题库文件、保存本地数据库。\n\n" +
                        "本应用未申请网络权限，所有数据都在这台设备上。"
                )
                .setPositiveButton("我知道了", null)
                .show()
        }
        setupRow(binding.rowAbout, "关于", "版本 ${appVersion()}", null, true) {
            startActivity(Intent(this, AboutActivity::class.java))
        }
        setupRow(
            binding.rowReset, "恢复默认设置",
            "悬浮窗、悬浮球、练习选项全部还原", null, true
        ) {
            MaterialAlertDialogBuilder(this)
                .setTitle("恢复默认设置")
                .setMessage(
                    "将把悬浮窗、悬浮球、练习选项等全部设置恢复到出厂默认。\n\n" +
                        "题库、练习记录、收藏等数据不受影响。"
                )
                .setNegativeButton("取消", null)
                .setPositiveButton("恢复默认") { _, _ ->
                    Prefs.resetToDefaults()
                    toast("已恢复默认设置")
                    recreate()
                }
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        // 从浮窗「框」返回时，范围可能已经变了，这里统一刷新一遍
        binding.rowRange.rowValue.text = rangeText()
        binding.rowQuestionColor.rowValue.text = colorLabel(Prefs.QUESTION_COLORS, Prefs.questionColor)
        binding.rowAnswerColor.rowValue.text = colorLabel(Prefs.ANSWER_COLORS, Prefs.answerColor)
        binding.rowBallColor.rowValue.text = colorLabel(Prefs.BALL_COLORS, Prefs.ballColor)
        binding.rowBallStyle.rowValue.text = ballStyleLabel()
        binding.rowFloatTip.rowValue.text = if (Prefs.hideButtons) "已隐藏" else "显示中"
        binding.rowWrongRemove.rowValue.text = "${Prefs.wrongRemove} 次"
        binding.rowTikuManage.rowValue.text = "${Bank.tikus.size} 个题库"
        binding.swDark.rowSwitch.isChecked = Prefs.floatDark
        binding.sliderTextAlpha.rowValue.text = "${Prefs.textAlpha}%"
        binding.sliderTextAlpha.rowSeek.progress = (Prefs.textAlpha - Prefs.MIN_TEXT_ALPHA)
            .coerceIn(0, Prefs.MAX_TEXT_ALPHA - Prefs.MIN_TEXT_ALPHA)
        binding.sliderBgAlpha.rowValue.text = "${Prefs.bgAlpha}%"
        binding.sliderBgAlpha.rowSeek.progress = (Prefs.bgAlpha - Prefs.MIN_BG_ALPHA)
            .coerceIn(0, Prefs.MAX_BG_ALPHA - Prefs.MIN_BG_ALPHA)
        binding.sliderTextSize.rowValue.text = "${Prefs.textSize}sp"
        binding.sliderTextSize.rowSeek.progress =
            (Prefs.textSize - Prefs.MIN_TEXT_SIZE).coerceIn(0, Prefs.MAX_TEXT_SIZE - Prefs.MIN_TEXT_SIZE)
        binding.sliderBallSize.rowValue.text = "${Prefs.ballSize}dp"
        binding.sliderBallSize.rowSeek.progress =
            (Prefs.ballSize - Prefs.MIN_BALL_SIZE).coerceIn(0, Prefs.MAX_BALL_SIZE - Prefs.MIN_BALL_SIZE)
        binding.sliderBallAlpha.rowValue.text = "${Prefs.ballAlpha}%"
        binding.sliderBallAlpha.rowSeek.progress =
            (Prefs.ballAlpha - Prefs.MIN_BALL_ALPHA).coerceIn(0, Prefs.MAX_BALL_ALPHA - Prefs.MIN_BALL_ALPHA)
        refreshSummaries()
    }

    private fun setupRow(
        row: ViewSettingRowBinding,
        title: String,
        desc: String?,
        value: String?,
        arrow: Boolean,
        onClick: () -> Unit
    ) {
        row.rowTitle.text = title
        if (desc.isNullOrBlank()) {
            row.rowDesc.visibility = View.GONE
        } else {
            row.rowDesc.visibility = View.VISIBLE
            row.rowDesc.text = desc
        }
        row.rowValue.text = value.orEmpty()
        row.rowArrow.visibility = if (arrow) View.VISIBLE else View.GONE
        row.root.setOnClickListener { onClick() }
    }

    private fun rangeText(): String {
        val r = Prefs.range() ?: return "全屏"
        return "已限定 ${r.width()}×${r.height()}"
    }

    /** 预设色显示名字，自选色显示 #RRGGBB */
    private fun colorLabel(colors: List<Pair<String, Int>>, value: Int): String =
        colors.firstOrNull { it.second == value }?.first
            ?: String.format("#%06X", 0xFFFFFF and value)

    /**
     * 自定义调色：色相条 + 饱和度/明度面板 + 十六进制输入，常用色做快捷入口。
     * 点「确定」才写入偏好，避免拖动过程中反复落库。
     */
    private fun showColorPicker(
        title: String,
        presets: List<Pair<String, Int>>,
        current: Int,
        onPick: (Int) -> Unit
    ) {
        val d = resources.displayMetrics.density
        fun dp(v: Number): Int = (v.toFloat() * d).toInt()

        val view = layoutInflater.inflate(R.layout.dialog_color_picker, null)
        val preview = view.findViewById<View>(R.id.preview)
        val etHex = view.findViewById<EditText>(R.id.et_hex)
        val picker = view.findViewById<ColorPickerView>(R.id.picker)
        val swatches = view.findViewById<LinearLayout>(R.id.swatches)

        var guard = false
        var picked = current

        fun hex(c: Int): String = String.format("#%06X", 0xFFFFFF and c)

        fun sync(c: Int, writeHex: Boolean) {
            picked = c
            preview.background = colorDot(c)
            if (writeHex) {
                guard = true
                etHex.setText(hex(c))
                etHex.setSelection(etHex.text.length)
                guard = false
            }
            markSwatch(swatches, c)
        }

        picker.setColor(current, false)
        sync(current, true)
        picker.onColorChanged = { c -> sync(c, true) }

        // 输入完按键盘「完成」就把键盘收起来，免得遮住下面的常用色和按钮
        etHex.setOnEditorActionListener { v, _, _ ->
            (getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager)
                ?.hideSoftInputFromWindow(v.windowToken, 0)
            v.clearFocus()
            true
        }

        etHex.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (guard) return
                val c = parseHex(s?.toString().orEmpty()) ?: return
                picked = c
                preview.background = colorDot(c)
                picker.setColor(c, false)
                markSwatch(swatches, c)
            }
        })

        presets.forEach { (_, value) ->
            val cell = FrameLayout(this).apply {
                tag = value
                isClickable = true
                setPadding(dp(4), dp(4), dp(4), dp(4))
            }
            cell.addView(
                View(this).apply { background = colorDot(value) },
                FrameLayout.LayoutParams(dp(26), dp(26), Gravity.CENTER)
            )
            cell.setOnClickListener {
                picker.setColor(value, false)
                sync(value, true)
            }
            val lp = LinearLayout.LayoutParams(dp(38), dp(38))
            lp.marginEnd = dp(2)
            swatches.addView(cell, lp)
        }
        markSwatch(swatches, current)

        val dlg = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(view)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                onPick(picked)
                toast("$title 已更新")
            }
            .create()
        // 弹出键盘时压缩对话框而不是把「确定」顶到键盘下面，配合 XML 里的 ScrollView 可滚动
        dlg.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        dlg.show()
    }

    /** 当前颜色正好等于某个常用色时，给那个色块套一圈主题色环 */
    private fun markSwatch(parent: LinearLayout, color: Int) {
        val primary = Themes.primary(this)
        for (i in 0 until parent.childCount) {
            val cell = parent.getChildAt(i)
            val value = cell.tag as? Int ?: continue
            cell.background = if (value == color) ringBg(primary) else null
        }
    }

    private fun parseHex(text: String): Int? {
        val t = text.trim().removePrefix("#")
        if (t.length != 6 || !t.all { it.isDigit() || it in "abcdefABCDEF" }) return null
        return try {
            (0xFF000000 or t.toLong(16)).toInt()
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun ringBg(color: Int): GradientDrawable {
        val d = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke((2 * d).toInt(), color)
        }
    }

    private fun colorDot(color: Int): GradientDrawable {
        val d = resources.displayMetrics.density
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setStroke((1f * d).toInt(), 0x33000000)
        }
    }

    private fun showRangeDialog() {
        val items = arrayOf("全屏检测（推荐）", "自定义范围（在浮窗上点框选图标）")
        MaterialAlertDialogBuilder(this)
            .setTitle("题目检测范围")
            .setItems(items) { _, which ->
                if (which == 0) {
                    Prefs.setRange(null)
                    binding.rowRange.rowValue.text = rangeText()
                    toast("已恢复全屏检测")
                } else {
                    Prefs.setRange(null)
                    binding.rowRange.rowValue.text = rangeText()
                    toast("在浮窗上点左上角框选图标，框内任意位置可拖动，右下角可缩放")
                }
            }
            .show()
    }

    private fun showWrongRemoveDialog() {
        val labels = (1..5).map { "$it 次" }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("连续答对几次移出错题本")
            .setSingleChoiceItems(labels, Prefs.wrongRemove - 1) { dialog, which ->
                Prefs.wrongRemove = which + 1
                binding.rowWrongRemove.rowValue.text = "${which + 1} 次"
                dialog.dismiss()
            }
            .show()
    }
}
