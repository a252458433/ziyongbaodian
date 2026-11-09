package com.ziyongbaodian.ui

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.data.SearchHit
import com.ziyongbaodian.databinding.ActivitySearchBinding
import com.ziyongbaodian.databinding.ItemSearchResultBinding
import com.ziyongbaodian.util.dp
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast

class SearchActivity : BaseActivity() {

    private lateinit var binding: ActivitySearchBinding
    private lateinit var adapter: ResultAdapter
    private var searching = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ResultAdapter()
        binding.rvResult.layoutManager = LinearLayoutManager(this)
        binding.rvResult.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnGo.setOnClickListener { doSearch() }
        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                doSearch()
                true
            } else false
        }
        binding.btnEmptyImport.setOnClickListener {
            startActivity(Intent(this, ImportActivity::class.java))
        }

        val q = intent.getStringExtra(EXTRA_QUERY)
        if (!q.isNullOrBlank()) {
            binding.etQuery.setText(q)
            binding.etQuery.setSelection(binding.etQuery.text.length)
            doSearch()
        } else {
            showIdle()
        }
    }

    /** 进入页面、尚未搜索时的引导空态 */
    private fun showIdle() {
        adapter.submit(emptyList())
        binding.emptyView.visibility = View.VISIBLE
        if (Bank.questions.isEmpty()) {
            binding.tvEmptyTitle.text = "还没有题库"
            binding.tvEmptyDesc.text = "先导入一份题库文件，再来这里搜题"
            binding.btnEmptyImport.visibility = View.VISIBLE
        } else {
            binding.tvEmptyTitle.text = "输入题目开始搜题"
            binding.tvEmptyDesc.text = "粘贴题干或其中一段，选项字母和标点会被自动忽略\n当前题库共 ${Bank.questions.size} 道题"
            binding.btnEmptyImport.visibility = View.GONE
        }
    }

    private fun doSearch() {
        val query = binding.etQuery.text.toString().trim()
        if (query.isEmpty()) {
            toast("先输入或粘贴题目")
            return
        }
        if (searching) return
        searching = true
        hideKeyboard()
        binding.emptyView.visibility = View.GONE
        val topN = com.ziyongbaodian.data.Prefs.topN
        Thread {
            val hits = Bank.search(query, maxOf(topN, 5))
            runOnUiThread {
                searching = false
                showResult(query, hits)
            }
        }.start()
    }

    private fun showResult(query: String, hits: List<SearchHit>) {
        adapter.submit(hits)
        if (hits.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.tvEmptyTitle.text = "没搜到匹配的题目"
            binding.tvEmptyDesc.text = "换个关键词，或先导入包含这道题的题库"
            if (Bank.questions.isEmpty()) {
                binding.tvEmptyDesc.text = "当前还没有题库，先导入一份再来搜"
            }
            binding.btnEmptyImport.visibility = View.VISIBLE
        } else {
            binding.emptyView.visibility = View.GONE
            Db.addHistory(query, hits[0].question.id)
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etQuery.windowToken, 0)
    }

    // ---------------- adapter ----------------

    inner class ResultAdapter : RecyclerView.Adapter<ResultAdapter.VH>() {
        private val items = ArrayList<SearchHit>()

        fun submit(list: List<SearchHit>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position], position)
        }

        inner class VH(private val b: ItemSearchResultBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(hit: SearchHit, index: Int) {
                val ctx = b.root.context
                val question = hit.question
                b.tvIndex.text = if (index == 0) "最匹配" else "匹配 ${index + 1}"
                b.tvType.text = question.typeName()
                b.tvScore.text = "相似度 ${(hit.ratio * 100).toInt()}%"
                b.tvQuestion.text = question.q
                b.optionContainer.removeAllViews()

                val answerIdx = question.answerIndexes.toSet()
                for (i in question.options.indices) {
                    val row = TextView(ctx)
                    row.text = "${question.optionLabel(i)}  ${question.options[i]}"
                    row.textSize = 13.5f
                    row.setTextColor(ContextCompat.getColor(ctx, R.color.text))
                    row.setLineSpacing(ctx.dp(2f).toFloat(), 1f)
                    val p = ctx.dp(9f)
                    row.setPadding(p, p, p, p)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    lp.bottomMargin = ctx.dp(6f)
                    row.layoutParams = lp
                    if (i in answerIdx) {
                        row.setBackgroundResource(R.drawable.bg_option_ok)
                        row.setTextColor(ContextCompat.getColor(ctx, R.color.ok))
                        row.setTypeface(null, Typeface.BOLD)
                    } else {
                        row.setBackgroundResource(R.drawable.bg_option)
                    }
                    b.optionContainer.addView(row)
                }

                val label = question.answerLabel()
                val content = question.answerContent()
                b.tvAnswer.text = if (content.isBlank()) label else "$label  $content"

                b.btnFav.setImageResource(if (question.fav) R.drawable.ic_star_fill else R.drawable.ic_star)
                b.btnFav.imageTintList = if (question.fav) null
                else android.content.res.ColorStateList.valueOf(ContextCompat.getColor(ctx, R.color.text_hint))
                b.btnWrong.imageTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, if (question.wrong) R.color.err else R.color.text_hint)
                )

                b.btnFav.setOnClickListener {
                    val next = !question.fav
                    Db.setFav(question.id, next)
                    val updated = question.copy(fav = next)
                    Bank.engine.update(updated)
                    items[index] = hit.copy(question = updated)
                    notifyItemChanged(index)
                    toast(if (next) "已收藏" else "已取消收藏")
                }
                b.btnWrong.setOnClickListener {
                    val next = !question.wrong
                    Db.setWrong(question.id, next)
                    val updated = question.copy(wrong = next, rightCount = 0)
                    Bank.engine.update(updated)
                    items[index] = hit.copy(question = updated)
                    notifyItemChanged(index)
                    toast(if (next) "已加入错题" else "已移出错题")
                }
            }
        }
    }

    companion object {
        const val EXTRA_QUERY = "extra_query"
    }
}
