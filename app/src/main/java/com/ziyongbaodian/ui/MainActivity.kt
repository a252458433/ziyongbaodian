package com.ziyongbaodian.ui

import android.os.Bundle
import com.ziyongbaodian.R
import com.ziyongbaodian.databinding.ActivityMainBinding
import com.ziyongbaodian.util.setupStatusBar

/**
 * 定制版主界面：只有「练习」一个页面。
 * 无底栏、无标题区；设置入口在练习页右上角，无障碍接口与搜索都在练习页。
 */
class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar(brand = true)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, PracticeFragment(), "practice")
                .commit()
        }
    }
}
