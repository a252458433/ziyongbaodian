package com.ziyongbaodian.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.data.Question
import com.ziyongbaodian.data.Tiku
import com.ziyongbaodian.databinding.ActivityTikuBinding
import com.ziyongbaodian.databinding.ItemTikuManageBinding
import com.ziyongbaodian.util.BankExport
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast

class TikuActivity : BaseActivity() {

    private lateinit var binding: ActivityTikuBinding
    private lateinit var adapter: TikuAdapter

    /** 待写出的导出内容，等用户选好保存位置后写进去 */
    private var pendingText: String? = null
    private var pendingCount: Int = 0

    private val createDoc =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
            val text = pendingText
            pendingText = null
            if (uri == null || text == null) return@registerForActivityResult
            val count = pendingCount
            Thread {
                val ok = try {
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(text.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    true
                } catch (e: Exception) {
                    false
                }
                runOnUiThread {
                    toast(if (ok) "已导出 $count 道题" else "导出失败，换个保存位置再试")
                }
            }.start()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivityTikuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = TikuAdapter()
        binding.rvTiku.layoutManager = LinearLayoutManager(this)
        binding.rvTiku.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
        binding.btnImport.setOnClickListener { openImport() }
        binding.btnImportTop.setOnClickListener { openImport() }
        binding.btnExportTop.setOnClickListener { exportAll() }
    }

    /** 导出全部题库，题号连续，重新导入会合成一个题库 */
    private fun exportAll() {
        Thread {
            val all = ArrayList<Question>()
            for (t in Db.tikuList()) all.addAll(Db.questionsOf(t.id))
            val res = BankExport.toText(all)
            runOnUiThread {
                if (res.count == 0) {
                    toast("还没有可导出的题目，先导入题库")
                    return@runOnUiThread
                }
                launchSave(res.text, res.count, BankExport.fileName("全部题库"))
            }
        }.start()
    }

    private fun exportTiku(t: Tiku) {
        Thread {
            val res = BankExport.toText(Db.questionsOf(t.id))
            runOnUiThread {
                if (res.count == 0) {
                    toast("「${t.name}」里没有可导出的题目")
                    return@runOnUiThread
                }
                launchSave(res.text, res.count, BankExport.fileName(t.name))
            }
        }.start()
    }

    /** 交给系统文件选择器保存，txt 可以再走一遍「导入题库」 */
    private fun launchSave(text: String, count: Int, fileName: String) {
        pendingText = text
        pendingCount = count
        createDoc.launch(fileName)
    }

    override fun onResume() {
        super.onResume()
        load()
    }

    private fun openImport() {
        startActivity(Intent(this, ImportActivity::class.java))
    }

    private fun load() {
        val list = Db.tikuList()
        adapter.submit(list)
        val total = list.sumOf { it.count }
        binding.tvSummary.text = "共 ${list.size} 个题库 · $total 道题"
        binding.emptyView.visibility = if (list.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    inner class TikuAdapter : RecyclerView.Adapter<TikuAdapter.VH>() {
        private val items = ArrayList<Tiku>()

        fun submit(list: List<Tiku>) {
            items.clear()
            items.addAll(list)
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val b = ItemTikuManageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(b)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

        inner class VH(private val b: ItemTikuManageBinding) : RecyclerView.ViewHolder(b.root) {
            fun bind(t: Tiku) {
                b.tvName.text = t.name
                b.tvCount.text = "${t.count} 道题"
                b.swEnabled.setOnCheckedChangeListener(null)
                b.swEnabled.isChecked = t.enabled
                b.swEnabled.setOnCheckedChangeListener { _, checked ->
                    Db.setTikuEnabled(t.id, checked)
                    Bank.refreshQuestions()
                    toast(if (checked) "已启用：${t.name}" else "已停用：${t.name}")
                }
                b.btnExport.setOnClickListener { exportTiku(t) }
                b.btnDelete.setOnClickListener {
                    MaterialAlertDialogBuilder(this@TikuActivity)
                        .setTitle("删除题库")
                        .setMessage("确定要删除「${t.name}」及其 ${t.count} 道题吗？删除后无法恢复。")
                        .setNegativeButton("取消", null)
                        .setPositiveButton("删除") { _, _ ->
                            Db.deleteTiku(t.id)
                            Bank.reload()
                            load()
                            toast("已删除")
                        }
                        .show()
                }
                b.root.setOnClickListener {
                    PracticeActivity.start(this@TikuActivity, PracticeActivity.MODE_ORDER, t.id)
                }
            }
        }
    }
}
