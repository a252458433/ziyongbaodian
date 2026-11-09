package com.ziyongbaodian.ui

import android.os.Bundle
import com.ziyongbaodian.databinding.ActivityAboutBinding
import com.ziyongbaodian.util.Themes
import com.ziyongbaodian.util.appVersion
import com.ziyongbaodian.util.setupStatusBar

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tvAppName.setText(Themes.current().appNameRes)  // 名称随主题联动
        binding.tvVersion.text = "版本 ${appVersion()}"
        binding.btnBack.setOnClickListener { finish() }
    }
}
