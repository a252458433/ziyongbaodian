package com.ziyongbaodian.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.ziyongbaodian.data.Bank
import com.ziyongbaodian.data.Db
import com.ziyongbaodian.databinding.ActivityImportBinding
import com.ziyongbaodian.search.QParser
import com.ziyongbaodian.util.DocxText
import com.ziyongbaodian.util.dp
import com.ziyongbaodian.util.setupStatusBar
import com.ziyongbaodian.util.toast
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

class ImportActivity : BaseActivity() {

    private lateinit var binding: ActivityImportBinding
    private var content: String = ""
    private var fileName: String = ""

    /** null 表示自动识别（默认）；用户手动指定后才有值 */
    private var manualFormat: QParser.Format? = null
    private var lastResult: QParser.Result? = null
    /** 上一次自动填入题库名称的值，用于判断用户有没有手动改过 */
    private var autoName: String = ""

    private val pickFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) readFile(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupStatusBar()
        binding = ActivityImportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
        binding.btnPickFile.setOnClickListener {
            pickFile.launch(
                arrayOf(
                    "text/*",
                    "application/json",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/octet-stream",
                    "*/*"
                )
            )
        }
        binding.btnPaste.setOnClickListener { showPasteDialog() }
        binding.btnPreview.setOnClickListener { preview(true) }
        binding.btnImport.setOnClickListener { doImport() }
        binding.rowFormat.setOnClickListener { showFormatDialog() }

        renderFormat()

        // 从其他 App「用其他应用打开」进来（微信 / 文件管理器等），直接读传入的文件
        if (intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { readFile(it) }
        }
    }

    // ==================== 格式（自动识别 + 可手动指定） ====================

    /** 按内容猜一个格式；没内容时按标准格式显示 */
    private fun detected(): QParser.Format =
        if (content.isBlank()) QParser.Format.STANDARD else QParser.detect(content)

    private fun effective(): QParser.Format = manualFormat ?: detected()

    /** 兜底检测到更合适的格式后，让界面显示的就是它 */
    private fun renderDetected(f: QParser.Format) {
        binding.tvFormat.text = "${f.label} · 自动识别"
    }

    private fun renderFormat() {
        val name = if (manualFormat == null) {
            "${effective().label} · 自动识别"
        } else {
            "${effective().label} · 手动指定"
        }
        binding.tvFormat.text = name
    }

    /** 内容变化后重新识别，并提示用户识别结果 */
    private fun autoDetect(showToast: Boolean) {
        // 换了一份内容，之前的手动指定就作废，回到自动识别
        manualFormat = null
        renderFormat()
        if (showToast && content.isNotBlank()) {
            toast("已自动识别为「${effective().label}」")
        }
        lastResult = null
        binding.tvPreview.visibility = View.GONE
    }

    private fun showFormatDialog() {
        val formats = QParser.Format.values()
        // 只列短名，格式说明放在对话框说明里，避免长文字把列表撑出屏幕
        val labels = arrayOf("自动识别（推荐）") + formats.map { it.label }
        val checked = manualFormat?.let { it.ordinal + 1 } ?: 0
        MaterialAlertDialogBuilder(this)
            .setTitle("题库格式")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                manualFormat = if (which == 0) null else formats[which - 1]
                renderFormat()
                lastResult = null
                binding.tvPreview.visibility = View.GONE
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // ==================== 读取内容 ====================

    private fun readFile(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null || bytes.isEmpty()) {
                toast("文件为空或无法读取")
                return
            }
            fileName = queryName(uri)
            // 第三方 App 传来的文件名可能不带扩展名（微信显示名常只有标题），
            // 此时按 Intent 的 mimeType 补一个，否则 Word 文档会被当成乱码文本
            if (!fileName.contains('.')) {
                when (intent?.type) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> fileName += ".docx"
                    "application/msword" -> fileName += ".doc"
                    "application/json" -> fileName += ".json"
                    "text/csv", "application/csv" -> fileName += ".csv"
                }
            }
            val kind = DocxText.kindOf(fileName, bytes)
            var fromWord = false
            content = when (kind) {
                DocxText.Kind.DOCX -> {
                    val text = DocxText.extract(bytes)
                    if (text.isNullOrBlank()) {
                        toast("这个 Word 文档里没读到文字，可能是加密文件或纯图片")
                        return
                    }
                    fromWord = true
                    text
                }
                DocxText.Kind.OLD_DOC -> {
                    toast("这是旧版 .doc，请用 Word/WPS 另存为 .docx 后再导入")
                    return
                }
                DocxText.Kind.XLSX -> {
                    toast("Excel 文件请先另存为 CSV 再导入")
                    return
                }
                DocxText.Kind.PPTX -> {
                    toast("PPT 暂不支持导入，请把题目复制成文本")
                    return
                }
                DocxText.Kind.ARCHIVE -> {
                    toast("这是压缩包，请先解压再选择题库文件")
                    return
                }
                DocxText.Kind.TEXT -> decode(bytes)
            }
            val auto = fileName.substringBeforeLast('.').take(30)
            val typed = binding.etName.text?.toString().orEmpty()
            // 用户改过名字就不覆盖；没动过（空的或还是上一个文件名）才跟着新文件走
            if (typed.isBlank() || typed == autoName) binding.etName.setText(auto)
            autoName = auto
            val lines = content.count { it == '\n' } + 1
            binding.tvSource.text = buildString {
                append(if (fromWord) "已从 Word 文档读取：" else "已读取：")
                append(fileName).append('\n')
                append("共 ").append(content.length).append(" 个字符，").append(lines).append(" 行")
            }
            autoDetect(true)
        } catch (e: Exception) {
            toast("读取失败：${e.message}")
        }
    }

    private fun decode(bytes: ByteArray): String {
        var start = 0
        if (bytes.size >= 3 && bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()) {
            start = 3
        }
        val buf = ByteBuffer.wrap(bytes, start, bytes.size - start)
        return try {
            val decoder = Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
            decoder.decode(buf).toString()
        } catch (e: CharacterCodingException) {
            try {
                String(bytes, start, bytes.size - start, Charset.forName("GB18030"))
            } catch (_: Exception) {
                String(bytes, start, bytes.size - start, Charsets.ISO_8859_1)
            }
        }
    }

    private fun queryName(uri: Uri): String {
        // file:// Uri 直接取路径末段；content:// 走 ContentProvider 查询
        if (uri.scheme == "file") return uri.lastPathSegment ?: "题库文件"
        var name = "题库文件"
        try {
            contentResolver.query(uri, null, null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) name = c.getString(idx) ?: name
            }
        } catch (_: Exception) {
        }
        return name
    }

    private fun showPasteDialog() {
        val ctx = this
        val edit = EditText(ctx)
        edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
        edit.minLines = 8
        edit.gravity = android.view.Gravity.TOP or android.view.Gravity.START
        edit.hint = "把题库文本粘贴到这里…"
        edit.setTextSize(13f)
        edit.setText(content)
        val pad = ctx.dp(18f)
        val wrap = FrameLayout(ctx)
        wrap.setPadding(pad, ctx.dp(6f), pad, 0)
        wrap.addView(
            edit,
            FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, ctx.dp(220f))
        )
        MaterialAlertDialogBuilder(ctx)
            .setTitle("粘贴题库文本")
            .setView(wrap)
            .setNegativeButton("取消", null)
            .setPositiveButton("确定") { _, _ ->
                content = edit.text.toString()
                fileName = ""
                binding.tvSource.text =
                    if (content.isBlank()) "还没有选择内容。" else "已粘贴 ${content.length} 个字符"
                autoDetect(false)
            }
            .show()
    }

    /**
     * 自动模式：先按内容猜一种格式解析；一道题都没解析出来时，把其余格式都试一遍，
     * 取识别题目最多的一种。这样「自动识别」在边角内容上也不至于直接给 0 道题。
     */
    private fun parseAuto(text: String): Pair<QParser.Format, QParser.Result> {
        val guess = QParser.detect(text)
        var bestFmt = guess
        var best = QParser.parse(text, guess)
        if (best.items.isNotEmpty()) return bestFmt to best
        QParser.Format.values().forEach { f ->
            if (f == guess) return@forEach
            val r = QParser.parse(text, f)
            if (r.items.size > best.items.size) {
                bestFmt = f
                best = r
            }
        }
        return bestFmt to best
    }

    private fun preview(showToast: Boolean) {
        if (content.isBlank()) {
            if (showToast) toast("先选择文件或粘贴文本")
            return
        }
        val manual = manualFormat
        val fmt: QParser.Format
        val result: QParser.Result
        if (manual == null) {
            val (f, r) = parseAuto(content)
            fmt = f
            result = r
            // 兜底找到了更合适的格式，同步显示出来
            if (f != detected()) renderDetected(f)
        } else {
            fmt = manual
            result = QParser.parse(content, manual)
        }
        lastResult = result
        val sb = StringBuilder()
        sb.append("格式：").append(fmt.label)
        if (manualFormat == null) sb.append("（自动识别）")
        sb.append("\n")
        sb.append("识别到 ").append(result.items.size).append(" 道题")
        if (result.logs.isNotEmpty()) sb.append("，跳过 ").append(result.logs.size).append(" 行")
        sb.append("\n")
        for (i in result.items.indices.take(2)) {
            val q = result.items[i]
            sb.append("\n").append(i + 1).append(". ").append(q.q.take(40))
            sb.append("\n   答案：").append(q.answerLabel())
            if (q.options.isNotEmpty()) sb.append("　选项 ").append(q.options.size).append(" 个")
        }
        if (result.logs.isNotEmpty()) {
            sb.append("\n\n跳过示例：\n")
            for (log in result.logs.take(4)) sb.append("· ").append(log).append("\n")
        }
        binding.tvPreview.text = sb.toString()
        binding.tvPreview.visibility = View.VISIBLE
        // 预览文本在页面底部，用户当前看不到，顺手滚过去
        binding.tvPreview.post {
            binding.scroll.smoothScrollTo(0, binding.tvPreview.bottom)
        }
        if (showToast && result.items.isEmpty()) toast("没识别到题目，检查一下格式")
    }

    private fun doImport() {
        if (content.isBlank()) {
            toast("先选择文件或粘贴文本")
            return
        }
        if (lastResult == null) {
            preview(false)
        }
        val result = lastResult ?: return
        if (result.items.isEmpty()) {
            toast("没有可导入的题目，检查格式后再试")
            return
        }
        val name = binding.etName.text?.toString()?.trim().orEmpty()
            .ifBlank { fileName.substringBeforeLast('.').ifBlank { "未命名题库" } }

        binding.btnImport.isEnabled = false
        binding.btnImport.text = "导入中…"
        Thread {
            val tid = Db.insertTiku(name, result.items)
            runOnUiThread {
                binding.btnImport.isEnabled = true
                binding.btnImport.text = "开始导入"
                if (tid > 0) {
                    Bank.reload {
                        toast("已导入「$name」${result.items.size} 道题")
                        finish()
                    }
                } else {
                    toast("导入失败，请检查题目内容")
                }
            }
        }.start()
    }
}
