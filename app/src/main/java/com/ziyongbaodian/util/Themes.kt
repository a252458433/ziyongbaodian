package com.ziyongbaodian.util

import android.content.Context
import androidx.annotation.AttrRes
import androidx.core.content.ContextCompat
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Prefs

/**
 * 主题工具 · 柔光漫画 2.0
 *
 * 1.8.7-ui 起提供六套主题：漫画粉 / 石墨灰 / 品牌蓝 / 活力橙 / 薄荷青 / 星野紫。
 * 顺序即主题索引；ALIASES 与 AndroidManifest 里的 activity-alias 一一对应，
 * 新增主题必须同时补：themes.xml 的 style、colors.xml 的主色、header 渐变 drawable、
 * strings.xml 的桌面名、mipmap-anydpi-v26 的 adaptive 图标、manifest 的 alias。
 *
 * 当前选中索引存 Prefs.themeIndex，BaseActivity 在 onCreate 套用、onResume 检测变更重建。
 */
object Themes {

    data class Spec(val name: String, val colorRes: Int, val styleRes: Int, val appNameRes: Int)

    /** 顺序即主题索引；ALIASES 与 manifest 里的 activity-alias 一一对应 */
    val ALL: List<Spec> = listOf(
        Spec("漫画粉", R.color.t_manga, R.style.Theme_ZiyongBaodian_Manga, R.string.app_name_pink),
        Spec("石墨灰", R.color.t_gray, R.style.Theme_ZiyongBaodian_Gray, R.string.app_name),
        Spec("品牌蓝", R.color.t_blue, R.style.Theme_ZiyongBaodian_Blue, R.string.app_name_blue),
        Spec("活力橙", R.color.t_orange, R.style.Theme_ZiyongBaodian_Torch, R.string.app_name_torch),
        Spec("薄荷青", R.color.t_mint, R.style.Theme_ZiyongBaodian_Mint, R.string.app_name_mint),
        Spec("星野紫", R.color.t_aurora, R.style.Theme_ZiyongBaodian_Aurora, R.string.app_name_aurora)
    )

    /** 各主题对应的桌面入口 alias 类名（顺序与 ALL 一致） */
    private val ALIASES = listOf(
        ".LauncherPink", ".LauncherGray", ".LauncherBlue",
        ".LauncherTorch", ".LauncherMint", ".LauncherAurora"
    )

    fun index(): Int = Prefs.themeIndex().coerceIn(0, ALL.size - 1)
    fun current(): Spec = ALL[index()]

    fun styleRes(index: Int = -1): Int = ALL[if (index < 0) index() else index].styleRes

    fun name(): String = current().name

    fun color(context: Context, index: Int = -1): Int =
        ContextCompat.getColor(context, ALL[if (index < 0) index() else index].colorRes)

    /**
     * 桌面入口随主题联动：只启用当前主题对应的 activity-alias，其余全部停用。
     * App 启动和切主题时各调用一次。
     */
    fun applyLauncherAlias(context: Context) {
        val pm = context.packageManager
        val pkg = context.packageName
        val cur = index()
        ALIASES.forEachIndexed { i, alias ->
            pm.setComponentEnabledSetting(
                android.content.ComponentName(pkg, "$pkg$alias"),
                if (i == cur) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                android.content.pm.PackageManager.DONT_KILL_APP
            )
        }
    }

    /**
     * 悬浮窗答案颜色 + 悬浮球颜色随主题联动。
     * 只在切主题时调用——App 启动不要调，否则会覆盖用户手动调过的自定义色。
     */
    fun syncFloatColors(context: Context) {
        val c = color(context)
        if (Prefs.answerColor != c) Prefs.answerColor = c
        if (Prefs.ballColor != c) Prefs.ballColor = c
    }

    /**
     * appcompat 的 colorPrimary。工程开启了 nonTransitiveRClass，
     * 本模块的 R 里没有这个 attr，必须引用 appcompat 的 R。
     */
    val ATTR_PRIMARY: Int = androidx.appcompat.R.attr.colorPrimary

    /** 当前主题的主色 */
    fun primary(context: Context): Int = attr(context, ATTR_PRIMARY)

    /** 读当前主题里的颜色属性（如 R.attr.colorPrimary），会随 setTheme 变化 */
    fun attr(context: Context, @AttrRes attr: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val c = ta.getColor(0, 0)
        ta.recycle()
        return c
    }
}
