package com.ziyongbaodian.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ziyongbaodian.service.ScreenSearchService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Context.dp(value: Float): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

fun Context.toast(msg: CharSequence) {
    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}

/** 无障碍服务是否已开启 */
fun isAccessibilityEnabled(context: Context): Boolean {
    if (ScreenSearchService.instance != null) return true
    val expected = ComponentName(context, ScreenSearchService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any {
        it.equals(expected.flattenToString(), ignoreCase = true) ||
            it.equals(expected.flattenToShortString(), ignoreCase = true)
    }
}

fun openAccessibilitySettings(context: Context) {
    try {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: Exception) {
        try {
            context.startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
        }
    }
}

fun canDrawOverlay(context: Context): Boolean = Settings.canDrawOverlays(context)

fun openOverlaySettings(context: Context) {
    try {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + context.packageName)
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (_: Exception) {
    }
}

fun AppCompatActivity.setupStatusBar(brand: Boolean = false) {
    val color = if (brand) Themes.attr(this, com.ziyongbaodian.R.attr.colorBrandG1)
    else android.graphics.Color.WHITE
    window.statusBarColor = color
    // 状态栏图标深浅按背景亮度自动选择：浅色底（灰白系主题）配深色图标，深色底配浅色图标
    val r = (color shr 16) and 0xFF
    val g = (color shr 8) and 0xFF
    val b = color and 0xFF
    val isLight = (r * 299 + g * 587 + b * 114) / 1000 >= 160
    var flags = window.decorView.systemUiVisibility
    flags = flags or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
    flags = if (isLight) flags or android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
    else flags and android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
    window.decorView.systemUiVisibility = flags
}

fun formatTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return when {
        diff < 60_000 -> "刚刚"
        diff < 3_600_000 -> "${diff / 60_000} 分钟前"
        diff < 86_400_000L -> "${diff / 3_600_000} 小时前"
        diff < 172_800_000L -> "昨天"
        else -> SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(Date(millis))
    }
}

/** 版本号，统一从 packageManager 读取，避免页面里写死 */
fun Context.appVersion(): String = try {
    packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0.1"
} catch (e: Exception) {
    "1.0.1"
}
