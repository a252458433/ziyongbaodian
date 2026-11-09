package com.ziyongbaodian.ui

import android.os.Bundle
import com.ziyongbaodian.databinding.ActivityDemoBinding
import com.ziyongbaodian.util.setupStatusBar

/** 演练页：整屏只有一道题，用来验证浮窗识别是否正常 */
class DemoActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        val binding = ActivityDemoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.btnClose.setOnClickListener { finish() }
    }
}
