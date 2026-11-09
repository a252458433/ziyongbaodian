package com.ziyongbaodian.util

import com.ziyongbaodian.data.Question
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 题库导出：把库里的题目还原成「标准格式」文本，
 * 导出的文件可以再走一遍「导入题库」原样导入，方便换手机 / 备份 / 二次编辑。
 */
object BankExport {

    data class Result(val text: String, val count: Int, val skipped: Int)

    /** 标准格式文本：1、题干 / A、选项 / 答案：B */
    fun toText(questions: List<Question>): Result {
        val sb = StringBuilder()
        var no = 0
        var skipped = 0
        for (q in questions) {
            val stem = q.q.trim()
            // 没有选项、没有答案的条目导出去也导不回来，直接跳过并计数
            if (stem.isEmpty() || q.options.isEmpty() || q.ans.isBlank()) {
                skipped++
                continue
            }
            no++
            sb.append(no).append('、').append(oneLine(stem)).append('\n')
            for (i in q.options.indices) {
                sb.append(('A' + i)).append('、').append(oneLine(q.options[i])).append('\n')
            }
            sb.append("答案：").append(q.ans.uppercase()).append("\n\n")
        }
        return Result(sb.toString(), no, skipped)
    }

    /** 导出文件名：题库名_20261028.txt，非法字符替换掉 */
    fun fileName(prefix: String): String {
        val day = SimpleDateFormat("yyyyMMdd", Locale.CHINA).format(Date())
        val safe = prefix.trim()
            .replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_")
            .take(40)
            .ifBlank { "题库" }
        return "${safe}_$day.txt"
    }

    /** 解析器按行处理，题干里的换行需要压平，否会被当成新的一行 */
    private fun oneLine(s: String): String =
        s.replace("\r", "").replace("\n", " ").replace(Regex("\\s+"), " ").trim()
}
