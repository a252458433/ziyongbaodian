package com.ziyongbaodian.ui

import android.content.Intent
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.databinding.ActivityGuideBinding
import com.ziyongbaodian.util.canDrawOverlay
import com.ziyongbaodian.util.isAccessibilityEnabled
import com.ziyongbaodian.util.openAccessibilitySettings
import com.ziyongbaodian.util.openOverlaySettings
import com.ziyongbaodian.util.setupStatusBar

class GuideActivity : BaseActivity() {

    private lateinit var binding: ActivityGuideBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivityGuideBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnOpenAccess.setOnClickListener { openAccessibilitySettings(this) }
        binding.btnOpenOverlay.setOnClickListener { openOverlaySettings(this) }
        binding.btnImport.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }
        binding.btnDemo.setOnClickListener {
            startActivity(Intent(this, DemoActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        val serviceOn = isAccessibilityEnabled(this)
        binding.stateService.text = if (serviceOn) "已开启" else "未开启"
        binding.stateService.setBackgroundResource(if (serviceOn) R.drawable.bg_pill_green else R.drawable.bg_pill_gray)
        binding.stateService.setTextColor(
            ContextCompat.getColor(this, if (serviceOn) R.color.ok else R.color.text_sub)
        )
        binding.btnOpenAccess.text = if (serviceOn) "已开启，去系统设置查看" else "去开启无障碍"

        val overlay = canDrawOverlay(this)
        binding.stateOverlay.text = if (overlay) "已允许" else "可跳过"
        binding.stateOverlay.setBackgroundResource(if (overlay) R.drawable.bg_pill_green else R.drawable.bg_pill_gray)
        binding.stateOverlay.setTextColor(
            ContextCompat.getColor(this, if (overlay) R.color.ok else R.color.text_sub)
        )

        val count = Bank.questions.size
        binding.stateBank.text = "$count 题"
        binding.stateBank.setBackgroundResource(if (count > 0) R.drawable.bg_pill_green else R.drawable.bg_pill_gray)
        binding.stateBank.setTextColor(
            ContextCompat.getColor(this, if (count > 0) R.color.ok else R.color.text_sub)
        )
    }
}
