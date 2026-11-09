package com.ziyongbaodian

import android.app.Application
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Prefs

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        Prefs.init(this)
        Db.init(this)
        // 桌面名称/图标与应用内主题保持一致（沉溺漫画·灰 / 即梦漫画·粉）
        com.ziyongbaodian.util.Themes.applyLauncherAlias(this)
        val dm = resources.displayMetrics
        screenW = dm.widthPixels
        screenH = dm.heightPixels

        if (Prefs.firstRun) {
            Bank.ensureDemoBank(this) { Prefs.firstRun = false }
        } else {
            Bank.reload()
        }
    }

    companion object {
        lateinit var instance: App
            private set
        var screenW: Int = 1080
        var screenH: Int = 1920
    }
}
