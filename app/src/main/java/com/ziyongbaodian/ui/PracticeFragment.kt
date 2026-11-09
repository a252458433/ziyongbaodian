package com.ziyongbaodian.ui

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.databinding.FragmentPracticeBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PracticeFragment : Fragment() {

    private var _binding: FragmentPracticeBinding? = null
    private val binding get() = _binding!!

    private val refreshListener: () -> Unit = {
        _binding?.root?.post { render() }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPracticeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.modeOrder.setOnClickListener {
            PracticeActivity.start(requireContext(), PracticeActivity.MODE_ORDER, null)
        }
        binding.modeRandom.setOnClickListener {
            PracticeActivity.start(requireContext(), PracticeActivity.MODE_RANDOM, null)
        }
        binding.modeWrong.setOnClickListener {
            PracticeActivity.start(requireContext(), PracticeActivity.MODE_WRONG, null)
        }
        binding.modeFav.setOnClickListener {
            PracticeActivity.start(requireContext(), PracticeActivity.MODE_FAV, null)
        }
        // 题型练习：弹窗选择单选/多选/判断
        binding.modeType.setOnClickListener { showTypeDialog() }
        // 题库练习：弹窗选择题库
        binding.modeBank.setOnClickListener { showBankDialog() }
        // 手动搜题；长按 = 无障碍接口入口（开启指引）
        binding.searchBox.setOnClickListener { openSearch() }
        binding.searchBox.setOnLongClickListener {
            start(GuideActivity::class.java)
            Toast.makeText(requireContext(), "长按激活：无障碍开启指引", Toast.LENGTH_SHORT).show()
            true
        }
        // 右上角设置入口
        binding.btnSettings.setOnClickListener { start(SettingsActivity::class.java) }
        Bank.addListener(refreshListener)
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    override fun onDestroyView() {
        Bank.removeListener(refreshListener)
        super.onDestroyView()
        _binding = null
    }

    /** 题型练习：三个题型带数量，选完直接开练 */
    private fun showTypeDialog() {
        val qs = Bank.questions
        val names = listOf("单选题", "多选题", "判断题")
        val keys = listOf(
            Question.TYPE_KEY_SINGLE,
            Question.TYPE_KEY_MULTI,
            Question.TYPE_KEY_JUDGE
        )
        val icons = listOf(R.drawable.ic_check, R.drawable.ic_copy, R.drawable.ic_text)
        val counts = listOf(
            qs.count { it.typeKey() == Question.TYPE_KEY_SINGLE },
            qs.count { it.typeKey() == Question.TYPE_KEY_MULTI },
            qs.count { it.typeKey() == Question.TYPE_KEY_JUDGE }
        ).map { "$it 道" }
        showPickDialog(
            "题型练习", names, counts, icons,
            R.drawable.bg_icon_green, R.color.ok
        ) { i -> startType(keys[i]) }
    }

    /** 题库练习：列出全部题库，选完按顺序开练 */
    private fun showBankDialog() {
        val list = Bank.tikus
        if (list.isEmpty()) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("题库练习")
                .setMessage("还没有题库，先导入一份吧")
                .setNegativeButton("取消", null)
                .setPositiveButton("去导入") { _, _ ->
                    startActivity(Intent(requireContext(), ImportActivity::class.java))
                }
                .show()
            return
        }
        showPickDialog(
            "选择题库",
            list.map { it.name },
            list.map { "${it.count} 道" },
            List(list.size) { R.drawable.ic_bank },
            R.drawable.bg_icon_blue, R.color.t_manga
        ) { i ->
            PracticeActivity.start(
                requireContext(), PracticeActivity.MODE_ORDER, list[i].id
            )
        }
    }

    /**
     * 通用选择弹窗：每行 = 彩色图标胶囊 + 名称 + 数量小胶囊 + 箭头
     */
    private fun showPickDialog(
        title: String,
        names: List<String>,
        counts: List<String>,
        icons: List<Int>,
        chipRes: Int,
        iconTintRes: Int,
        onPick: (Int) -> Unit
    ) {
        val ctx = requireContext()
        val d = resources.displayMetrics.density
        fun dp(v: Number): Int = (v.toFloat() * d).toInt()

        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(6), dp(6), dp(6), dp(6))
        }
        val holder = arrayOfNulls<AlertDialog>(1)

        names.forEachIndexed { i, name ->
            val row = LayoutInflater.from(ctx)
                .inflate(R.layout.view_pick_row, wrap, false) as LinearLayout
            row.findViewById<TextView>(R.id.tv_name).text = name
            row.findViewById<TextView>(R.id.tv_count).text = counts[i]

            val chip = row.findViewById<FrameLayout>(R.id.chip)
            chip.background = ContextCompat.getDrawable(ctx, chipRes)
            val iv = row.findViewById<ImageView>(R.id.iv_icon)
            iv.setImageResource(icons[i])
            iv.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(ctx, iconTintRes)
            )

            row.setOnClickListener {
                holder[0]?.dismiss()
                onPick(i)
            }
            wrap.addView(row)
        }

        val dlg = MaterialAlertDialogBuilder(ctx)
            .setTitle(title)
            .setView(wrap)
            .setNegativeButton("取消", null)
            .create()
        holder[0] = dlg
        dlg.show()
    }

    private fun startType(key: String) {
        val n = Bank.questions.count { it.typeKey() == key }
        if (n == 0) {
            android.widget.Toast.makeText(
                requireContext(), "这个题型还没有题目", android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        PracticeActivity.startType(requireContext(), key)
    }

    private fun render() {
        val b = _binding ?: return
        val stats = Db.stats()
        b.tvWrongDesc.text = if (stats.wrongCount > 0) "错题本 ${stats.wrongCount} 道" else "自动收错题本"
        b.tvBankHint.text = when {
            Bank.loading -> "题库加载中"
            Bank.tikus.isEmpty() -> "还没有题库"
            else -> "${Bank.tikus.size} 个题库可选"
        }
        // 学习概况
        b.tvStatTiku.text = stats.tikuCount.toString()
        b.tvStatTotal.text = stats.questionCount.toString()
        b.tvStatWrong.text = stats.wrongCount.toString()
        b.tvStatFav.text = stats.favCount.toString()
    }

    private fun start(cls: Class<*>) {
        startActivity(Intent(requireContext(), cls))
    }

    private fun openSearch() {
        startActivity(Intent(requireContext(), SearchActivity::class.java))
    }
}
