package com.ziyongbaodian.data

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Rect

/** 本地设置，全部保存在本机 SharedPreferences，不上传任何数据 */
object Prefs {
    private const val NAME = "ziyongbaodian_prefs"
    private lateinit var sp: SharedPreferences

    // 检测范围
    const val KEY_RANGE_L = "range_l"
    const val KEY_RANGE_T = "range_t"
    const val KEY_RANGE_R = "range_r"
    const val KEY_RANGE_B = "range_b"

    const val KEY_TEXT_ALPHA = "text_alpha"
    const val KEY_BG_ALPHA = "bg_alpha"
    const val KEY_FLOAT_DARK = "float_dark"
    const val KEY_TOP_N = "top_n"
    const val KEY_WRONG_REMOVE = "wrong_remove"
    const val KEY_FIRST_RUN = "first_run"
    const val KEY_FLOAT_X = "float_x"
    const val KEY_FLOAT_Y = "float_y"
    const val KEY_FLOAT_W = "float_w"
    const val KEY_FLOAT_H = "float_h"
    const val KEY_HIDE_BUTTONS = "hide_buttons"
    const val KEY_AUTO_HIDE = "auto_hide"
    const val KEY_ANSWER_COLOR = "answer_color"
    const val KEY_QUESTION_COLOR = "question_color"
    const val KEY_TEXT_SIZE = "text_size"
    const val KEY_BALL_ALPHA = "ball_alpha"
    const val KEY_BALL_SIZE = "ball_size"
    const val KEY_BALL_COLOR = "ball_color"
    const val KEY_BALL_STYLE = "ball_style"
    const val KEY_BALL_TEXT = "ball_text"
    const val KEY_BALL_X = "ball_x"
    const val KEY_BALL_Y = "ball_y"

    /** 悬浮球样式取值 */
    const val BALL_STYLE_HEART = "heart"
    const val BALL_STYLE_FLOWER = "flower"
    const val BALL_STYLE_STAR = "star"
    const val BALL_STYLE_CIRCLE = "circle"
    const val BALL_STYLE_TEXT = "text"

    /** 自定义文字 / 符号的最大长度，避免一个字撑爆小球 */
    const val MAX_BALL_TEXT = 4
    const val KEY_BALL_STYLE_V2 = "ball_style_v2"
    const val KEY_BG_ALPHA_V2 = "bg_alpha_v2"
    const val KEY_BALL_COLOR_V2 = "ball_color_v2"
    const val KEY_SHUFFLE_OPTIONS = "shuffle_options"

    /** 恢复默认设置的时间戳，服务收到后把浮窗按默认位置重建 */
    const val KEY_RESET_TS = "reset_ts"

    /** 默认答案：漫画粉（定制版主题色） */
    const val DEFAULT_ANSWER_COLOR = 0xFFF9538F.toInt()

    /** 默认题目白 */
    const val DEFAULT_QUESTION_COLOR = 0xFFFFFFFF.toInt()

    /** 浮窗文字不透明度范围与默认值（%），最低 5% 仍能看清轮廓，方便极度挡屏时自救 */
    const val MIN_TEXT_ALPHA = 5
    const val MAX_TEXT_ALPHA = 100
    const val DEFAULT_TEXT_ALPHA = 100

    /** 浮窗背景不透明度范围与默认值（%），默认 60 让底下的内容能透出来 */
    const val MIN_BG_ALPHA = 0
    const val MAX_BG_ALPHA = 100
    const val DEFAULT_BG_ALPHA = 60

    /** 浮窗题目字号范围与默认值（sp） */
    const val MIN_TEXT_SIZE = 9
    const val MAX_TEXT_SIZE = 24
    const val DEFAULT_TEXT_SIZE = 12

    /** 悬浮球（最小化后的小球）直径范围与默认值（dp） */
    const val MIN_BALL_SIZE = 26
    const val MAX_BALL_SIZE = 80
    const val DEFAULT_BALL_SIZE = 42

    /** 悬浮球不透明度范围与默认值（%） */
    const val MIN_BALL_ALPHA = 8
    const val MAX_BALL_ALPHA = 100
    const val DEFAULT_BALL_ALPHA = 50

    /** 悬浮球默认颜色（漫画粉） */
    const val DEFAULT_BALL_COLOR = 0xFFF9538F.toInt()

    /** 漫画粉改版前的旧默认色（品牌蓝），仅用于一次性迁移判断 */
    private const val OLD_BLUE_DEFAULT = 0xFF2F6BFF.toInt()

    /** 旧版默认色（桃粉），仅用于一次性迁移判断 */
    private const val OLD_DEFAULT_BALL_COLOR = 0xFFFF6B9D.toInt()

    /** 可选悬浮球颜色：名称 to 色值 */
    val BALL_COLORS: List<Pair<String, Int>> = listOf(
        "漫画粉" to 0xFFF9538F.toInt(),
        "桃粉" to 0xFFFF6B9D.toInt(),
        "玫红" to 0xFFE64980.toInt(),
        "蓝色" to 0xFF2F6BFF.toInt(),
        "正红" to 0xFFE5484D.toInt(),
        "橙色" to 0xFFF79009.toInt(),
        "明黄" to 0xFFFFC53D.toInt(),
        "草绿" to 0xFF3EBD7F.toInt(),
        "天蓝" to 0xFF6EC1FF.toInt(),
        "白色" to 0xFFFFFFFF.toInt(),
        "石墨灰" to 0xFF4A505C.toInt()
    )

    /** 可选答案颜色：名称 to 色值 */
    val ANSWER_COLORS: List<Pair<String, Int>> = listOf(
        "漫画粉" to 0xFFF9538F.toInt(),
        "品牌蓝" to 0xFF2F6BFF.toInt(),
        "天蓝" to 0xFF00A8E8.toInt(),
        "绿色" to 0xFF3EBD7F.toInt(),
        "红色" to 0xFFE5484D.toInt(),
        "橙色" to 0xFFF79009.toInt(),
        "紫色" to 0xFF7A5AF8.toInt(),
        "白色" to 0xFFFFFFFF.toInt(),
        "黑色" to 0xFF000000.toInt(),
        "石墨灰" to 0xFF4A505C.toInt()
    )

    /** 可选题目 / 选项颜色 */
    val QUESTION_COLORS: List<Pair<String, Int>> = listOf(
        "白色" to 0xFFFFFFFF.toInt(),
        "浅灰" to 0xFFD5DBE3.toInt(),
        "绿色" to 0xFF3EBD7F.toInt(),
        "天蓝" to 0xFF6EC1FF.toInt(),
        "黄色" to 0xFFFFD666.toInt(),
        "橙色" to 0xFFFFB067.toInt(),
        "紫色" to 0xFFC4A5FF.toInt(),
        "黑色" to 0xFF000000.toInt()
    )

    fun init(context: Context) {
        sp = context.applicationContext.getSharedPreferences(NAME, Context.MODE_PRIVATE)
        // 悬浮球规格调整（26-80dp / 8-100%）：老版本存的旧默认值一次性换成新默认值
        if (!sp.getBoolean(KEY_BALL_STYLE_V2, false)) {
            sp.edit()
                .putInt(KEY_BALL_SIZE, DEFAULT_BALL_SIZE)
                .putInt(KEY_BALL_ALPHA, DEFAULT_BALL_ALPHA)
                .putBoolean(KEY_BALL_STYLE_V2, true)
                .apply()
        }
        // 背景不透明度默认值由 100 改为 60：只在「没设置过」或「还停在老默认值 100」时迁到 60，
        // 用户自己调过的其他数值保持原样，不抢用户的设置
        if (!sp.getBoolean(KEY_BG_ALPHA_V2, false)) {
            val e = sp.edit().putBoolean(KEY_BG_ALPHA_V2, true)
            if (!sp.contains(KEY_BG_ALPHA) || sp.getInt(KEY_BG_ALPHA, 100) == 100) {
                e.putInt(KEY_BG_ALPHA, DEFAULT_BG_ALPHA)
            }
            e.apply()
        }
        // 悬浮球默认色由桃粉改为蓝色：只在「没设置过」或「还停在老默认桃粉」时迁成蓝色，
        // 用户自己挑过的颜色（玫红、草绿、自定义色值…）一律不动
        if (!sp.getBoolean(KEY_BALL_COLOR_V2, false)) {
            val e = sp.edit().putBoolean(KEY_BALL_COLOR_V2, true)
            if (!sp.contains(KEY_BALL_COLOR) || sp.getInt(KEY_BALL_COLOR, 0) == OLD_DEFAULT_BALL_COLOR) {
                e.putInt(KEY_BALL_COLOR, DEFAULT_BALL_COLOR)
            }
            e.apply()
        }
        // 漫画粉改版：悬浮球 / 答案字色还停在旧默认蓝的，一次性迁成漫画粉；
        // 用户自己挑过的颜色不动
        if (!sp.getBoolean(KEY_BALL_COLOR_V2 + "_p", false)) {
            val e = sp.edit().putBoolean(KEY_BALL_COLOR_V2 + "_p", true)
            if (sp.getInt(KEY_BALL_COLOR, 0) == OLD_BLUE_DEFAULT) {
                e.putInt(KEY_BALL_COLOR, DEFAULT_BALL_COLOR)
            }
            if (sp.getInt(KEY_ANSWER_COLOR, 0) == OLD_BLUE_DEFAULT) {
                e.putInt(KEY_ANSWER_COLOR, DEFAULT_ANSWER_COLOR)
            }
            e.apply()
        }
    }

    fun raw(): SharedPreferences = sp

    // 主题索引：0=漫画粉 1=石墨灰 2=品牌蓝 3=活力橙（Themes.ALL 顺序）；未设置时默认品牌蓝（1.8.4 起）
    const val KEY_THEME_INDEX = "theme_index"

    fun themeIndex(): Int = sp.getInt(KEY_THEME_INDEX, 2)

    fun setThemeIndex(i: Int) {
        sp.edit().putInt(KEY_THEME_INDEX, i).apply()
    }

    /** 卡片里文字（题干、选项、答案）的不透明度 5-100，只影响文字，背景另有滑块 */
    var textAlpha: Int
        get() = sp.getInt(KEY_TEXT_ALPHA, DEFAULT_TEXT_ALPHA)
            .coerceIn(MIN_TEXT_ALPHA, MAX_TEXT_ALPHA)
        set(value) = sp.edit()
            .putInt(KEY_TEXT_ALPHA, value.coerceIn(MIN_TEXT_ALPHA, MAX_TEXT_ALPHA))
            .apply()

    /** 卡片背景不透明度 0-100（默认 60），0 表示背景完全透明，只留文字浮在屏幕上 */
    var bgAlpha: Int
        get() = sp.getInt(KEY_BG_ALPHA, DEFAULT_BG_ALPHA)
            .coerceIn(MIN_BG_ALPHA, MAX_BG_ALPHA)
        set(value) = sp.edit()
            .putInt(KEY_BG_ALPHA, value.coerceIn(MIN_BG_ALPHA, MAX_BG_ALPHA))
            .apply()

    var floatDark: Boolean
        get() = sp.getBoolean(KEY_FLOAT_DARK, true)
        set(value) = sp.edit().putBoolean(KEY_FLOAT_DARK, value).apply()

    /** 悬浮窗结果条数：定制版已去掉设置入口，固定 1 条（保留 setter 兼容旧调用） */
    var topN: Int
        get() = 1
        set(@Suppress("UNUSED_PARAMETER") value) {}

    var wrongRemove: Int
        get() = sp.getInt(KEY_WRONG_REMOVE, 2)
        set(value) = sp.edit().putInt(KEY_WRONG_REMOVE, value.coerceIn(1, 5)).apply()

    var firstRun: Boolean
        get() = sp.getBoolean(KEY_FIRST_RUN, true)
        set(value) = sp.edit().putBoolean(KEY_FIRST_RUN, value).apply()

    var hideButtons: Boolean
        get() = sp.getBoolean(KEY_HIDE_BUTTONS, false)
        set(value) = sp.edit().putBoolean(KEY_HIDE_BUTTONS, value).apply()

    var autoHide: Boolean
        get() = sp.getBoolean(KEY_AUTO_HIDE, false)
        set(value) = sp.edit().putBoolean(KEY_AUTO_HIDE, value).apply()

    /** 浮窗里答案与正确选项的文字颜色 */
    var answerColor: Int
        get() = sp.getInt(KEY_ANSWER_COLOR, DEFAULT_ANSWER_COLOR)
        set(value) = sp.edit().putInt(KEY_ANSWER_COLOR, value).apply()

    /** 浮窗里题干与选项的普通文字颜色 */
    var questionColor: Int
        get() = sp.getInt(KEY_QUESTION_COLOR, DEFAULT_QUESTION_COLOR)
        set(value) = sp.edit().putInt(KEY_QUESTION_COLOR, value).apply()

    /** 浮窗题目（题干 + 选项）字号，单位 sp */
    var textSize: Int
        get() = sp.getInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
        set(value) = sp.edit().putInt(KEY_TEXT_SIZE, value.coerceIn(MIN_TEXT_SIZE, MAX_TEXT_SIZE)).apply()

    /** 悬浮球不透明度 8-100（只影响小球本身，不影响答案卡片） */
    var ballAlpha: Int
        get() = sp.getInt(KEY_BALL_ALPHA, DEFAULT_BALL_ALPHA).coerceIn(MIN_BALL_ALPHA, MAX_BALL_ALPHA)
        set(value) = sp.edit().putInt(KEY_BALL_ALPHA, value.coerceIn(MIN_BALL_ALPHA, MAX_BALL_ALPHA)).apply()

    /** 悬浮球小爱心的颜色 */
    var ballColor: Int
        get() = sp.getInt(KEY_BALL_COLOR, DEFAULT_BALL_COLOR)
        set(value) = sp.edit().putInt(KEY_BALL_COLOR, value).apply()

    /** 悬浮球样式：heart / flower / star / circle / text（自定义文字） */
    var ballStyle: String
        get() = sp.getString(KEY_BALL_STYLE, BALL_STYLE_HEART) ?: BALL_STYLE_HEART
        set(value) = sp.edit().putString(KEY_BALL_STYLE, value).apply()

    /** 自定义样式时悬浮球显示的文字 / 特殊符号 */
    var ballText: String
        get() = sp.getString(KEY_BALL_TEXT, "★")?.take(MAX_BALL_TEXT) ?: "★"
        set(value) = sp.edit()
            .putString(KEY_BALL_TEXT, value.trim().take(MAX_BALL_TEXT).ifBlank { "★" })
            .apply()

    /** 悬浮球直径，单位 dp */
    var ballSize: Int
        get() = sp.getInt(KEY_BALL_SIZE, DEFAULT_BALL_SIZE)
        set(value) = sp.edit().putInt(KEY_BALL_SIZE, value.coerceIn(MIN_BALL_SIZE, MAX_BALL_SIZE)).apply()

    /**
     * 悬浮球自己的位置，独立于卡片（float_x/float_y 是卡片收起前的位置，不能被小球覆盖）。
     * -1 表示还没存过（比如从没拖过小球），此时小球落到卡片收起前的位置。
     */
    var ballX: Int
        get() = sp.getInt(KEY_BALL_X, -1)
        set(value) = sp.edit().putInt(KEY_BALL_X, value).apply()

    var ballY: Int
        get() = sp.getInt(KEY_BALL_Y, -1)
        set(value) = sp.edit().putInt(KEY_BALL_Y, value).apply()

    /** 练习时打乱选项顺序（答案按内容重新对应，字母重新排） */
    var shuffleOptions: Boolean
        get() = sp.getBoolean(KEY_SHUFFLE_OPTIONS, true)
        set(value) = sp.edit().putBoolean(KEY_SHUFFLE_OPTIONS, value).apply()

    var floatX: Int
        get() = sp.getInt(KEY_FLOAT_X, 60)
        set(value) = sp.edit().putInt(KEY_FLOAT_X, value).apply()

    var floatY: Int
        get() = sp.getInt(KEY_FLOAT_Y, 260)
        set(value) = sp.edit().putInt(KEY_FLOAT_Y, value).apply()

    var floatW: Int
        get() = sp.getInt(KEY_FLOAT_W, 0)
        set(value) = sp.edit().putInt(KEY_FLOAT_W, value).apply()

    var floatH: Int
        get() = sp.getInt(KEY_FLOAT_H, 0)
        set(value) = sp.edit().putInt(KEY_FLOAT_H, value).apply()

    /** 检测范围，全屏时返回 null */
    fun range(): Rect? {
        // 拖框时可能被拖到屏幕外一点（如 -6），这里夹到 0 而不是判定为「没设置」，
        // 否则用户明明框过范围却会退化成全屏
        val l = sp.getInt(KEY_RANGE_L, -1).coerceAtLeast(0)
        val t = sp.getInt(KEY_RANGE_T, -1).coerceAtLeast(0)
        val r = sp.getInt(KEY_RANGE_R, -1)
        val b = sp.getInt(KEY_RANGE_B, -1)
        if (l < 0 || t < 0 || r <= l || b <= t) return null
        return Rect(l, t, r, b)
    }

    fun setRange(rect: Rect?) {
        val e = sp.edit()
        if (rect == null) {
            e.remove(KEY_RANGE_L).remove(KEY_RANGE_T).remove(KEY_RANGE_R).remove(KEY_RANGE_B)
        } else {
            e.putInt(KEY_RANGE_L, rect.left).putInt(KEY_RANGE_T, rect.top)
                .putInt(KEY_RANGE_R, rect.right).putInt(KEY_RANGE_B, rect.bottom)
        }
        e.apply()
    }

    fun hasRange(): Boolean = range() != null

    /**
     * 所有设置恢复出厂默认：主题、桌面图标、悬浮窗外观与位置、悬浮球、练习选项、检测范围。
     * 只动偏好设置，题库 / 练习记录 / 收藏都在数据库里，不受影响。
     * first_run 与三个一次性迁移开关（ball_style_v2 / bg_alpha_v2 / ball_color_v2）保留，
     * 避免下次启动迁移逻辑重跑。
     */
    fun resetToDefaults() {
        sp.edit()
            // 检测范围：删掉即回到「全屏检测」
            .remove(KEY_RANGE_L).remove(KEY_RANGE_T).remove(KEY_RANGE_R).remove(KEY_RANGE_B)
            // 浮窗外观
            .putInt(KEY_TEXT_ALPHA, DEFAULT_TEXT_ALPHA)
            .putInt(KEY_BG_ALPHA, DEFAULT_BG_ALPHA)
            .putBoolean(KEY_FLOAT_DARK, true)
            .putInt(KEY_ANSWER_COLOR, DEFAULT_ANSWER_COLOR)
            .putInt(KEY_QUESTION_COLOR, DEFAULT_QUESTION_COLOR)
            .putInt(KEY_TEXT_SIZE, DEFAULT_TEXT_SIZE)
            .putBoolean(KEY_HIDE_BUTTONS, false)
            .putBoolean(KEY_AUTO_HIDE, false)
            // 浮窗位置与大小：删掉后按 addCard 里的默认值重新出现
            .remove(KEY_FLOAT_X).remove(KEY_FLOAT_Y).remove(KEY_FLOAT_W).remove(KEY_FLOAT_H)
            // 悬浮球
            .putInt(KEY_BALL_ALPHA, DEFAULT_BALL_ALPHA)
            .putInt(KEY_BALL_SIZE, DEFAULT_BALL_SIZE)
            .putInt(KEY_BALL_COLOR, DEFAULT_BALL_COLOR)
            .putString(KEY_BALL_STYLE, BALL_STYLE_HEART)
            .putString(KEY_BALL_TEXT, "★")
            .remove(KEY_BALL_X).remove(KEY_BALL_Y)
            // 练习
            .putInt(KEY_TOP_N, 1)
            .putInt(KEY_WRONG_REMOVE, 2)
            .putBoolean(KEY_SHUFFLE_OPTIONS, true)
            // 主题与桌面图标：定制版已移除可选主题 / 图标套装模块，固定一套

            // 通知服务：浮窗按默认位置重建
            .putLong(KEY_RESET_TS, System.currentTimeMillis())
            .apply()
    }
}
