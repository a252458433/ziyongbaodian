package com.ziyongbaodian.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ziyongbaodian.R
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.databinding.ActivityQuestionListBinding
import com.ziyongbaodian.databinding.ItemQuestionRowBinding
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast

class QuestionListActivity : BaseActivity() {

    private lateinit var binding: ActivityQuestionListBinding
    private lateinit var adapter: RowAdapter
    private var type = TYPE_WRONG

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivityQuestionListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        type = intent.getIntExtra(EXTRA_TYPE, TYPE_WRONG)
        binding.tvTitle.text = if (type == TYPE_WRONG) "错题本" else "收藏夹"
        binding.tvEmpty.text = if (type == TYPE_WRONG) "还没有错题" else "还没有收藏"
        binding.tvEmptyDesc.text =
            if (type == TYPE_WRONG) "练习中答错的题会自动收进这里" else "在搜题结果或练习中点星标即可收藏"

        adapter = RowAdapter()
        binding.rvList.layoutManager = LinearLayoutManager(this)
        binding.rvList.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPractice.setOnClickListener {
            if (adapter.itemCount == 0) {
                toast("这里还是空的")
                return@setOnClickListener
            }
            PracticeActivity.start(
                this,
                if (type == TYPE_WRONG) PracticeActivity.MODE_WRONG else PracticeActivity.MODE_FAV,
                null
            )
        }
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun load() {
        val data = if (type == TYPE_WRONG) Db.wrongQuestions() else Db.favoriteQuestions()
        adapter.submit(data)
        binding.tvSummary.text = "共 ${data.size} 道题"
        binding.emptyView.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
        binding.btnPractice.visibility = if (data.isEmpty()) View.GONE else View.VISIBLE
    }

    inner class RowAdapter : RecyclerView.Adapter<RowAdapter.VH>() {
        private val items = ArrayList<Question>()

        fun submit(list: List<Question>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemQuestionRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position], position)

        inner class VH(private val b: ItemQuestionRowBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(q: Question, pos: Int) {
                b.tvQuestion.text = q.q
                val content = q.answerContent()
                b.tvAnswer.text =
                    "答案：${q.answerLabel()}" + if (content.isBlank()) "" else "　$content"
                b.root.setOnClickListener {
                    val i = Intent(this@QuestionListActivity, SearchActivity::class.java)
                    i.putExtra(SearchActivity.EXTRA_QUERY, q.q)
                    startActivity(i)
                }
                b.btnRemove.setOnClickListener {
                    MaterialAlertDialogBuilder(this@QuestionListActivity)
                        .setTitle(if (type == TYPE_WRONG) "移出错题本" else "取消收藏")
                        .setMessage("确定要移除这道题吗？")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("确定") { _, _ ->
                            if (type == TYPE_WRONG) Db.setWrong(q.id, false) else Db.setFav(q.id, false)
                            load()
                            toast("已移除")
                        }
                        .show()
                }
            }
        }
    }

    companion object {
        const val TYPE_WRONG = 1
        const val TYPE_FAV = 2
        private const val EXTRA_TYPE = "extra_type"

        fun start(context: Context, type: Int) {
            val i = Intent(context, QuestionListActivity::class.java)
            i.putExtra(EXTRA_TYPE, type)
            context.startActivity(i)
        }
    }
}
