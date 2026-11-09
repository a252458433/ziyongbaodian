package com.ziyongbaodian.ui

import com.ziyongbaodian.util.Themes
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Prefs
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.databinding.ActivityPracticeBinding
import com.ziyongbaodian.databinding.ItemOptionBinding
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast

class PracticeActivity : BaseActivity() {

    private lateinit var binding: ActivityPracticeBinding
    private val list = ArrayList<Question>()
    private var index = 0
    private var answered = false
    private val selected = LinkedHashSet<Int>()
    /** 本屏选项顺序：下标是屏幕上的位置，值是原始选项下标（打乱选项时用） */
    private var order: List<Int> = emptyList()
    /** 每道题的打乱结果缓存，上一题/下一题来回翻时顺序不会乱跳 */
    private val orderCache = HashMap<Long, List<Int>>()
    private var title = "练习"
    private var mode = MODE_ORDER
    private var tid: Long? = null
    /** 按题型练习：single / multi / judge */
    private var typeKey: String = Question.TYPE_KEY_SINGLE
    /** 从「最近在练的题」进来时直接定位到这道题 */
    private var startQid: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivityPracticeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_ORDER
        val t = intent.getLongExtra(EXTRA_TID, -1L)
        tid = if (t > 0) t else null
        startQid = intent.getLongExtra(EXTRA_QID, -1L)
        typeKey = intent.getStringExtra(EXTRA_TYPE) ?: Question.TYPE_KEY_SINGLE

        title = when (mode) {
            MODE_RANDOM -> "随机练习"
            MODE_WRONG -> "错题重练"
            MODE_FAV -> "收藏复习"
            MODE_TYPE -> Question.typeLabel(typeKey) + "练习"
            else -> if (tid != null) "题库练习" else "顺序练习"
        }
        binding.tvTitle.text = title

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPrev.setOnClickListener { prev() }
        binding.btnNext.setOnClickListener { nextOrSubmit() }
        binding.btnFav.setOnClickListener { toggleFav() }
        binding.btnWrong.setOnClickListener { toggleWrong() }

        // 跳题 / 搜题：只在顺序练习、题库练习、按题型练习里出现（随机、错题、收藏顺序不固定，跳过）
        val canLocate = mode == MODE_ORDER || mode == MODE_TYPE
        binding.btnJump.visibility = if (canLocate) View.VISIBLE else View.GONE
        binding.btnFind.visibility = if (canLocate) View.VISIBLE else View.GONE
        binding.btnJump.setOnClickListener { showJumpDialog() }
        binding.btnFind.setOnClickListener { showFindDialog() }

        load()
    }

    private fun load() {
        Thread {
            val data = when (mode) {
                MODE_RANDOM -> Bank.questions.shuffled()
                MODE_WRONG -> Db.wrongQuestions()
                MODE_FAV -> Db.favoriteQuestions()
                MODE_TYPE -> Bank.questions.filter { it.typeKey() == typeKey }
                else -> if (tid != null) Db.questionsOf(tid!!) else Bank.questions
            }
            runOnUiThread {
                list.clear()
                list.addAll(data)
                if (list.isEmpty()) {
                    val msg = when (mode) {
                        MODE_WRONG -> "错题本还是空的，先去练习吧"
                        MODE_FAV -> "还没有收藏的题目"
                        MODE_TYPE -> "这个题型还没有题目，换一个试试"
                        else -> "题库里还没有题目，先导入题库"
                    }
                    toast(msg)
                    finish()
                    return@runOnUiThread
                }
                index = if (startQid > 0) list.indexOfFirst { it.id == startQid }.coerceAtLeast(0) else 0
                render()
            }
        }.start()
    }

    private fun render() {
        val q = list[index]
        // 记入首页「最近在练的题」
        if (q.id > 0) Thread { Db.addPractice(q.id) }.start()
        answered = false
        selected.clear()
        binding.tvProgress.text = "${index + 1} / ${list.size}"
        binding.progress.max = list.size
        binding.progress.setProgressCompat(index + 1, true)
        binding.tvType.text = q.typeName()
        binding.tvBank.text = Bank.tikus.firstOrNull { it.id == q.tid }?.name ?: ""
        binding.tvQuestion.text = q.q
        binding.feedback.visibility = View.GONE

        binding.optionContainer.removeAllViews()
        order = displayOrder(q)
        val inflater = LayoutInflater.from(this)
        for (i in order.indices) {
            val row = inflater.inflate(R.layout.item_option, binding.optionContainer, false)
            val b = ItemOptionBinding.bind(row)
            b.tvLetter.text = q.optionLabel(i)
            b.tvContent.text = q.options[order[i]]
            b.ivMark.visibility = View.GONE
            row.setOnClickListener { onOptionClick(i) }
            binding.optionContainer.addView(row)
        }

        binding.btnPrev.alpha = if (index == 0) 0.45f else 1f
        binding.btnFav.setImageResource(if (q.fav) R.drawable.ic_star_fill else R.drawable.ic_star)
        binding.btnFav.imageTintList = if (q.fav) null else
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_hint))
        binding.btnWrong.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, if (q.wrong) R.color.err else R.color.text_hint)
        )
        updateNextLabel()
    }

    private fun updateNextLabel() {
        val q = list.getOrNull(index) ?: return
        binding.btnNext.text = when {
            q.isMultiple && !answered -> "提交答案"
            index == list.size - 1 -> "完成练习"
            else -> "下一题"
        }
        binding.btnNext.alpha = if (q.isMultiple && !answered && selected.isEmpty()) 0.6f else 1f
    }

    private fun onOptionClick(i: Int) {
        val q = list[index]
        if (answered) return
        if (q.isMultiple) {
            if (selected.contains(i)) selected.remove(i) else selected.add(i)
            refreshSelection()
            updateNextLabel()
        } else {
            selected.clear()
            selected.add(i)
            refreshSelection()
            evaluate()
        }
    }

    private fun refreshSelection() {
        for (i in 0 until binding.optionContainer.childCount) {
            val row = binding.optionContainer.getChildAt(i)
            val b = ItemOptionBinding.bind(row)
            if (selected.contains(i)) {
                row.setBackgroundResource(R.drawable.bg_option_ok)
                b.tvLetter.setTextColor(ContextCompat.getColor(this, R.color.ok))
            } else {
                row.setBackgroundResource(R.drawable.bg_option)
                b.tvLetter.setTextColor(Themes.primary(this))
            }
        }
    }

    private fun evaluate() {
        val q = list[index]
        answered = true
        val answer = answerDisplayIndexes(q)
        val correct = selected == answer
        for (i in 0 until binding.optionContainer.childCount) {
            val row = binding.optionContainer.getChildAt(i)
            val b = ItemOptionBinding.bind(row)
            val isAnswer = answer.contains(i)
            val picked = selected.contains(i)
            when {
                isAnswer -> {
                    row.setBackgroundResource(R.drawable.bg_option_ok)
                    b.tvLetter.setTextColor(ContextCompat.getColor(this, R.color.ok))
                    b.ivMark.visibility = View.VISIBLE
                    b.ivMark.setImageResource(R.drawable.ic_check)
                    b.ivMark.imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.ok)
                    )
                }
                picked -> {
                    row.setBackgroundResource(R.drawable.bg_option_err)
                    b.tvLetter.setTextColor(ContextCompat.getColor(this, R.color.err))
                    b.ivMark.visibility = View.VISIBLE
                    b.ivMark.setImageResource(R.drawable.ic_close)
                    b.ivMark.imageTintList = android.content.res.ColorStateList.valueOf(
                        ContextCompat.getColor(this, R.color.err)
                    )
                }
                else -> {
                    row.setBackgroundResource(R.drawable.bg_option)
                    b.ivMark.visibility = View.GONE
                }
            }
        }

        binding.feedback.visibility = View.VISIBLE
        if (correct) {
            binding.feedback.setBackgroundResource(R.drawable.bg_option_ok)
            binding.tvFeedbackTitle.text = "回答正确"
            binding.tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.ok))
            onCorrect(q)
        } else {
            binding.feedback.setBackgroundResource(R.drawable.bg_option_err)
            binding.tvFeedbackTitle.text = "回答错误"
            binding.tvFeedbackTitle.setTextColor(ContextCompat.getColor(this, R.color.err))
            onWrong(q)
        }
        val ansDisplay = answer.sorted()
        val label: String
        val content: String
        if (ansDisplay.isEmpty()) {
            label = q.ans
            content = ""
        } else if (q.isJudge) {
            label = ansDisplay.joinToString("") { if (order[it] == 0) "正确" else "错误" }
            content = ""
        } else {
            label = ansDisplay.joinToString("") { ('A' + it).toString() }
            content = ansDisplay.mapNotNull { q.options.getOrNull(order[it]) }.joinToString("；")
        }
        binding.tvFeedbackAnswer.text =
            "正确答案：$label" + if (content.isBlank()) "" else "　$content"
        updateNextLabel()
    }

    /**
     * 屏幕上的选项顺序：下标是显示位置，值是原始选项下标。
     * 只有「随机自测」打乱选项；判断题的「正确/错误」不打乱，否则读起来别扭。
     */
    private fun displayOrder(q: Question): List<Int> {
        val n = q.options.size
        if (n <= 1 || q.isJudge || mode != MODE_RANDOM) return (0 until n).toList()
        val cached = orderCache[q.id]
        if (cached != null && cached.size == n) return cached
        return (0 until n).shuffled().also { orderCache[q.id] = it }
    }

    /** 正确答案在屏幕上的位置 */
    private fun answerDisplayIndexes(q: Question): Set<Int> =
        order.indices.filter { i -> q.answerIndexes.contains(order[i]) }.toSet()

    private fun onCorrect(q: Question) {
        if (!q.wrong) return
        val count = Db.bumpRightCount(q.id)
        val limit = Prefs.wrongRemove
        if (count >= limit) {
            Db.setWrong(q.id, false)
            toast("连续答对 $limit 次，已移出错题本")
        } else {
            toast("再答对 ${limit - count} 次即可移出错题本")
        }
        Bank.engine.update(q.copy(wrong = count < limit))
    }

    private fun onWrong(q: Question) {
        Db.setWrong(q.id, true)
        Bank.engine.update(q.copy(wrong = true))
        list[index] = q.copy(wrong = true)
        binding.btnWrong.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, R.color.err)
        )
    }

    private fun nextOrSubmit() {
        val q = list.getOrNull(index) ?: return
        if (q.isMultiple && !answered) {
            if (selected.isEmpty()) {
                toast("先选择答案")
                return
            }
            evaluate()
            return
        }
        if (index >= list.size - 1) {
            toast("已完成本轮练习，共 ${list.size} 题")
            finish()
            return
        }
        index++
        render()
    }

    private fun prev() {
        if (index == 0) return
        index--
        render()
    }

    // ---------------- 跳题 / 搜题 ----------------

    private fun dp(v: Int): Int = (resources.displayMetrics.density * v).toInt()

    private fun inputBox(hintText: String, initial: String, numeric: Boolean): EditText {
        val e = EditText(this)
        e.hint = hintText
        e.setText(initial)
        e.isSingleLine = true
        e.setTextSize(15f)
        e.inputType = if (numeric) InputType.TYPE_CLASS_NUMBER else InputType.TYPE_CLASS_TEXT
        if (initial.isNotEmpty()) e.setSelection(initial.length)
        return e
    }

    /** 给输入框套一层带内边距的容器，避免文字贴着对话框边缘 */
    private fun boxed(v: View): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        val h = dp(22)
        wrap.setPadding(h, dp(8), h, 0)
        wrap.addView(
            v,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )
        return wrap
    }

    private fun showJumpDialog() {
        if (list.isEmpty()) return
        val box = inputBox("输入 1 - ${list.size} 之间的题号", (index + 1).toString(), true)
        MaterialAlertDialogBuilder(this)
            .setTitle("跳题")
            .setMessage("当前第 ${index + 1} 题，共 ${list.size} 题")
            .setView(boxed(box))
            .setPositiveButton("跳转") { _, _ ->
                val n = box.text.toString().trim().toIntOrNull()
                if (n == null || n < 1 || n > list.size) {
                    toast("题号需在 1 - ${list.size} 之间")
                    return@setPositiveButton
                }
                index = n - 1
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showFindDialog() {
        if (list.isEmpty()) return
        val box = inputBox("输入题干里的关键词", "", false)
        MaterialAlertDialogBuilder(this)
            .setTitle("搜题")
            .setMessage("在当前 ${list.size} 道题里按题干查找")
            .setView(boxed(box))
            .setPositiveButton("搜索") { _, _ -> doFind(box.text.toString()) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun doFind(keyword: String) {
        val k = Bank.engine.norm(keyword)
        if (k.isEmpty()) {
            toast("先输入关键词")
            return
        }
        val hits = ArrayList<Int>()
        for (i in list.indices) {
            if (Bank.engine.norm(list[i].q).contains(k)) {
                hits.add(i)
                if (hits.size >= 60) break
            }
        }
        if (hits.isEmpty()) {
            toast("没有找到包含「$keyword」的题目")
            return
        }
        if (hits.size == 1) {
            index = hits[0]
            render()
            toast("已跳到第 ${index + 1} 题")
            return
        }
        val labels = hits.map {
            "第 ${it + 1} 题　${list[it].q.replace('\n', ' ').trim().take(26)}"
        }.toTypedArray()
        MaterialAlertDialogBuilder(this)
            .setTitle("找到 ${hits.size} 道题")
            .setItems(labels) { _, which ->
                index = hits[which]
                render()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toggleFav() {
        val q = list.getOrNull(index) ?: return
        val next = !q.fav
        Db.setFav(q.id, next)
        list[index] = q.copy(fav = next)
        Bank.engine.update(list[index])
        binding.btnFav.setImageResource(if (next) R.drawable.ic_star_fill else R.drawable.ic_star)
        binding.btnFav.imageTintList = if (next) null else
            android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_hint))
        toast(if (next) "已收藏" else "已取消收藏")
    }

    private fun toggleWrong() {
        val q = list.getOrNull(index) ?: return
        val next = !q.wrong
        Db.setWrong(q.id, next)
        list[index] = q.copy(wrong = next)
        Bank.engine.update(list[index])
        binding.btnWrong.imageTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(this, if (next) R.color.err else R.color.text_hint)
        )
        toast(if (next) "已加入错题" else "已移出错题")
    }

    companion object {
        const val MODE_ORDER = "order"
        const val MODE_RANDOM = "random"
        const val MODE_WRONG = "wrong"
        const val MODE_FAV = "fav"
        const val MODE_TYPE = "type"
        private const val EXTRA_MODE = "extra_mode"
        private const val EXTRA_TID = "extra_tid"
        private const val EXTRA_QID = "extra_qid"
        private const val EXTRA_TYPE = "extra_type"

        fun start(context: Context, mode: String, tid: Long?) {
            val i = Intent(context, PracticeActivity::class.java)
            i.putExtra(EXTRA_MODE, mode)
            if (tid != null) i.putExtra(EXTRA_TID, tid)
            context.startActivity(i)
        }

        /** 按题型练习：typeKey 取 Question.TYPE_KEY_SINGLE / MULTI / JUDGE */
        fun startType(context: Context, typeKey: String) {
            val i = Intent(context, PracticeActivity::class.java)
            i.putExtra(EXTRA_MODE, MODE_TYPE)
            i.putExtra(EXTRA_TYPE, typeKey)
            context.startActivity(i)
        }

        /** 直接打开某一道题（顺序练习里定位到它） */
        fun startAt(context: Context, qid: Long) {
            val i = Intent(context, PracticeActivity::class.java)
            i.putExtra(EXTRA_MODE, MODE_ORDER)
            i.putExtra(EXTRA_QID, qid)
            context.startActivity(i)
        }
    }
}
