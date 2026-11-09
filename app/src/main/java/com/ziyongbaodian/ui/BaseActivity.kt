package com.ziyongbaodian.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ziyongbaodian.util.Themes

/**
 * 所有页面的基类：在 super.onCreate 之前套用当前主题；
 * onResume 检测到主题在别处被切换时自动 recreate（如设置页改主题后返回其他页面）。
 */
open class BaseActivity : AppCompatActivity() {

    private var appliedStyle: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        appliedStyle = Themes.styleRes()
        setTheme(appliedStyle)
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        if (appliedStyle != 0 && Themes.styleRes() != appliedStyle) recreate()
    }
}
