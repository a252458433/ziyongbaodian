package com.ziyongbaodian.search

import com.ziyongbaodian.data.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * 题库文本解析：把各种格式的题库文本转成题目列表。
 * 标准格式规则与原版一致（题号开头 + ABCD 选项 + 答案行）。
 */
object QParser {

    enum class Format(val label: String, val desc: String) {
        STANDARD("标准格式", "数字题号开头，ABCD 选项，最后一行「答案：B」或「标准答案：B」"),
        JSON("JSON 格式", "每行一个 JSON：{\"q\":\"题干\",\"a\":[\"选项1\",\"选项2\"],\"ans\":\"A\"}"),
        TABLE("表格格式", "CSV/TSV：题目,选项A,选项B,选项C,选项D,答案"),
        DELIMITER("分隔符格式", "题目#@=@#选项1|选项2|选项3#@=@#答案")
    }

    data class Result(val items: List<Question>, val logs: List<String>) {
        val ok: Boolean get() = items.isNotEmpty()
    }

    private const val LABELS = "ABCDEFGHIJK对错√×✔X正确错误YN"
    private const val ANSWER_LABEL = "ABCDEFGHIJK"
    private const val SKIP_CHARS = " 、.．，,\t"

    /** 题型小标题的常见开头，这类行要跳过而不是当成题干 */
    private val SECTION_WORDS = arrayOf(
        "单选题", "多选题", "判断题", "选择题", "共用题干", "案例分析", "填空题",
        "简答题", "名词解释", "配伍题", "不定项", "论述题", "问答题"
    )

    fun parse(raw: String, format: Format): Result {
        val lines = raw.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        return when (format) {
            Format.STANDARD -> parseStandard(lines)
            Format.JSON -> parseJson(lines)
            Format.TABLE -> parseTable(lines)
            Format.DELIMITER -> parseDelimiter(lines)
        }
    }

    /** 自动判断格式：给「从文件导入」用 */
    fun detect(raw: String): Format {
        val first = raw.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() } ?: return Format.STANDARD
        if (first.startsWith("{") && first.contains("\"q\"")) return Format.JSON
        if (first.contains("#@=@#")) return Format.DELIMITER
        val tabCount = first.count { it == '\t' }
        val commaCount = first.count { it == ',' }
        if (tabCount >= 3 || commaCount >= 3) return Format.TABLE
        return Format.STANDARD
    }

    // ---------------- 标准格式 ----------------

    private fun parseStandard(lines: List<String>): Result {
        val items = ArrayList<Question>()
        val logs = ArrayList<String>()
        var current: Question? = null
        // 题号在每个题型小节里从 1 开始递增，用它来判断「题号」和「题干开头的数字」的边界
        var expectedNo = 1

        fun flush() {
            val c = current ?: return
            val q = normalize(c)
            if (q != null) items.add(q)
            current = null
        }

        for ((index, rawLine) in lines.withIndex()) {
            val line = preProcess(rawLine)
            if (line.isBlank()) continue
            if (isSectionHeader(line)) {
                // 「一、单选题（题数：1465）」这类小节标题
                flush()
                expectedNo = 1
                continue
            }
            val first = line[0]
            if (first.isDigit()) {
                flush()
                val (no, body) = splitNumber(line, expectedNo)
                if (no != null) expectedNo = no + 1
                current = parseQuestion(body) ?: Question(q = body, options = emptyList(), ans = "")
            } else if (isAnswerLine(line)) {
                val c = current
                if (c == null) {
                    logs.add("第${index + 1}行：${trimLog(rawLine)} —— 还没识别到题干，已跳过")
                    continue
                }
                val ans = extractAnswer(line)
                if (ans.isEmpty()) {
                    logs.add("第${index + 1}行：${trimLog(rawLine)} —— 没解析到答案，已跳过")
                    continue
                }
                val merged = c.copy(ans = ans)
                val q = normalize(merged)
                if (q != null) {
                    items.add(q)
                    current = null
                } else {
                    logs.add("第${index + 1}行：${trimLog(rawLine)} —— 答案与选项不匹配，已跳过")
                    current = null
                }
            } else if (isOptionLine(line)) {
                val c = current
                if (c == null) {
                    logs.add("第${index + 1}行：${trimLog(rawLine)} —— 还没识别到题干，已跳过")
                    continue
                }
                current = c.copy(options = c.options + parseOptions(line))
            } else {
                val c = current
                // 题干被 Word 折成多行时，续行接回题干
                if (c != null && c.options.isEmpty() && c.ans.isEmpty()) {
                    current = c.copy(q = c.q + line)
                } else {
                    logs.add("第${index + 1}行：${trimLog(rawLine)} —— 不是题号/选项/答案行，已跳过")
                }
            }
        }
        flush()
        return Result(items, logs)
    }

    /** 「一、单选题（题数：1465，总分数：1465）」这种小节标题 */
    private fun isSectionHeader(line: String): Boolean {
        if (line.contains("题数") || line.contains("总分数")) return true
        var t = line.replace(" ", "").replace("\u3000", "")
        if (t.length > 40) return false
        t = t.replaceFirst(Regex("^[一二三四五六七八九十百]+[、.．)）]"), "")
        if (SECTION_WORDS.none { t.startsWith(it) }) return false
        return t.contains("（") || t.contains("(") || t.contains("题") || t.contains("分")
    }

    /** 「答案：B」「标准答案：B」「正确答案:AB」都算答案行 */
    private fun isAnswerLine(line: String): Boolean {
        if (line.startsWith("答案") || line.startsWith("标准答案") ||
            line.startsWith("正确答案") || line.startsWith("参考答案")
        ) return true
        val i = line.indexOf("答案")
        return i in 1..3 && line.length > i + 2 && (line[i + 2] == '：' || line[i + 2] == ':')
    }

    /**
     * 判断是否选项行。「A.心房」「A、心房」是选项；
     * 而「CT值…」「MRI检查…」这种题干开头的单个字母不算选项。
     */
    private fun isOptionLine(line: String): Boolean {
        if (ANSWER_LABEL.indexOf(line[0]) < 0) return false
        if (line.length == 1) return true
        val c = line[1]
        return !(c in 'A'..'Z' || c in 'a'..'z')
    }

    /**
     * 拆出「题号 + 题干」。
     * 题干本身可能以数字开头（「131I治疗的原理…」「1901年伦琴…」「60kg体重…」），
     * 单纯剥掉所有前导数字会把数字吃掉，所以优先按「题号连续递增」来找切点。
     */
    private fun splitNumber(line: String, expected: Int): Pair<Int?, String> {
        var end = 0
        while (end < line.length && line[end].isDigit()) end++
        if (end == 0) return null to removeQid(line)
        val digits = line.substring(0, end)
        val skips = SKIP_CHARS.toCharArray()
        for (k in 1..minOf(end, 4)) {
            val n = digits.substring(0, k).toIntOrNull() ?: continue
            if (n == expected) return n to line.substring(k).trimStart(*skips)
        }
        val k = if (end <= 3) end else minOf(end, 4)
        val n = digits.substring(0, k).toIntOrNull()
        return n to line.substring(k).trimStart(*skips)
    }

    private fun trimLog(s: String): String = if (s.length > 24) s.take(24) + "…" else s

    private fun preProcess(line: String): String {
        var s = line.trim().replace("\t", "").replace("\uFEFF", "")
        if (s.isEmpty()) return s
        s = s.replace('(', '（').replace(')', '）')
        if (!s.startsWith("（")) return s
        val parts = s.split("）", limit = 2)
        if (parts.size < 2) return s
        val ans = formatAnswer(parts[0].replace("（", ""))
        return if (isAnswer(ans)) "${parts[1].trim()}（$ans）" else s
    }

    private fun format(s: String): String = s
        .replace('Ａ', 'A').replace('Ｂ', 'B').replace('Ｃ', 'C').replace('Ｄ', 'D')
        .replace('．', '.').replace('(', '（').replace(')', '）')

    private fun formatAnswer(s: String): String = s
        .replace('Ａ', 'A').replace('Ｂ', 'B').replace('Ｃ', 'C').replace('Ｄ', 'D')
        .replace(" ", "").replace(".", "").replace("、", "")
        .replace("，", "").replace(",", "")

    private fun isAnswer(s: String): Boolean {
        if (s.isBlank()) return false
        return s.all { LABELS.indexOf(it) >= 0 }
    }

    private fun removeQid(s: String): String {
        var started = false
        for (i in s.indices) {
            val c = s[i]
            val isSkip = SKIP_CHARS.indexOf(c) >= 0
            if (isSkip) started = true
            if (started) {
                if (!isSkip) return s.substring(i)
            } else if (!c.isDigit()) {
                return s.substring(i)
            }
        }
        return s
    }

    private fun parseQuestion(s: String): Question? {
        if (s.isBlank()) return null
        val text = format(s.trim())
        if (text.contains("（") && text.contains("）")) {
            val parts = text.split("（").toMutableList()
            for (i in parts.indices) {
                parts[i] = parts[i].trim()
                if (parts[i].contains("）")) {
                    val sub = parts[i].split("）", limit = 2)
                    val ans = formatAnswer(sub[0])
                    if (isAnswer(ans)) {
                        parts[i] = "）" + sub[1].trim()
                        val q = if (i == parts.size - 1 && sub.size == 2 && sub[1].trim().isEmpty()) {
                            joinWith("（", parts, parts.size - 1)
                        } else {
                            joinWith("（", parts, -1)
                        }
                        return Question(q = q, options = emptyList(), ans = ans)
                    }
                }
            }
        }
        var q = ""
        var ans = ""
        for (i in text.indices.reversed()) {
            val tail = formatAnswer(text.substring(i))
            if (!isAnswer(tail)) break
            ans = tail
            q = text.substring(0, i)
        }
        if (q.isBlank()) return null
        return Question(q = q, options = emptyList(), ans = ans)
    }

    private fun joinWith(sep: String, arr: List<String>, exclude: Int): String {
        val sb = StringBuilder()
        for (i in arr.indices) {
            if (i == exclude) continue
            if (sb.isNotEmpty()) sb.append(sep)
            sb.append(arr[i])
        }
        return sb.toString()
    }

    /** 把 "A、选项一  B、选项二" 拆成 ["选项一", "选项二"] */
    private fun parseOptions(line: String): List<String> {
        var label = ANSWER_LABEL.indexOf(line[0])
        if (label < 0) return emptyList()
        var work = format(line)
        val out = ArrayList<String>()
        var i = 0
        var guard = 0
        while (guard++ < 20) {
            if (i + 1 > work.length) break
            work = work.substring(i + 1).trim()
            if (label > 11) {
                if (work.isNotEmpty()) out.add(work)
                break
            }
            var k = 0
            while (k < work.length && SKIP_CHARS.indexOf(work[k]) >= 0) k++
            if (k > 0) work = work.substring(k)
            val next = if (label + 1 < ANSWER_LABEL.length) ANSWER_LABEL[label + 1] else null
            if (next == null) {
                if (work.isNotEmpty()) out.add(work)
                break
            }
            val idx = nextLabelIndex(work, next, 1)
            if (idx < 0) {
                if (work.isNotEmpty()) out.add(work)
                break
            }
            out.add(work.substring(0, idx).trim())
            i = idx
            label++
        }
        return out.filter { it.isNotEmpty() }
    }

    /**
     * 找下一个选项标号的位置。标号后面必须紧跟分隔符（、.．，,：: 或结尾），
     * 否则「B.Gy和Ci」「国际辐射单位和测量委员会简称 ICRU」里的字母 C
     * 会被当成选项标号，把选项从中间切开。
     */
    private fun nextLabelIndex(s: String, letter: Char, from: Int): Int {
        var p = s.indexOf(letter, from)
        while (p >= 0) {
            if (isLabelBoundary(s, p)) return p
            p = s.indexOf(letter, p + 1)
        }
        return -1
    }

    private fun isLabelBoundary(s: String, p: Int): Boolean {
        val prev = if (p == 0) null else s[p - 1]
        // 前面紧贴英文字母或数字，说明是单词/公式里的字母（ICRU、X10-4C）
        if (prev != null && prev.code < 128 && prev.isLetterOrDigit()) return false
        // 前面必须是空白或标点，「形成E、A两峰」里的 A 才不会当成选项标号
        val prevOk = prev == null || prev == ' ' || prev == '\u3000' || prev == '\t' ||
            prev == '、' || prev == '，' || prev == ',' || prev == ';' || prev == '；' ||
            prev == '：' || prev == ':' || prev == '.' || prev == '．' ||
            prev == '）' || prev == ')'
        if (!prevOk) return false
        if (p + 1 >= s.length) return true
        val n = s[p + 1]
        return n == '、' || n == '.' || n == '．' || n == '，' || n == ',' ||
            n == '：' || n == ':' || n == ' ' || n == '\u3000' || n == '）' || n == ')'
    }

    private fun extractAnswer(line: String): String {
        var s = line
        val i = s.indexOf("答案")
        if (i >= 0) s = s.substring(i + 2)
        val sb = StringBuilder()
        for (ch in s) {
            val u = ch.uppercaseChar()
            if (LABELS.indexOf(u) >= 0) sb.append(u)
        }
        val ans = sb.toString()
        return normalizeAnswer(ans)
    }

    private fun normalizeAnswer(ans: String): String {
        if (ans.isEmpty()) return ans
        return when {
            ans.any { it == '×' || it == 'X' || it == 'N' || it == '错' } -> "B"
            ans.any { it == '√' || it == '✔' || it == 'Y' || it == '对' || it == '正' } -> "A"
            else -> ans.filter { ANSWER_LABEL.indexOf(it) >= 0 }
        }
    }

    private fun normalize(q: Question): Question? {
        if (q.q.isBlank() || q.ans.isBlank()) return null
        var options = q.options
        var ans = q.ans
        if (options.isEmpty()) {
            // 判断题：只有「（对）」「（√）」这类标记，补上两个选项
            if (ans == "A" || ans == "B") options = listOf("正确", "错误") else return null
        }
        if (ans.any { ANSWER_LABEL.indexOf(it) < 0 || ANSWER_LABEL.indexOf(it) >= options.size }) return null
        val type = when {
            ans.length > 1 -> Question.TYPE_MULTI
            options.size == 2 && (options[0] == "正确" || options[0] == "对") -> Question.TYPE_PANDUAN
            else -> Question.TYPE_SINGLE
        }
        return q.copy(options = options, ans = ans, type = type)
    }

    // ---------------- JSON ----------------

    private fun parseJson(lines: List<String>): Result {
        val items = ArrayList<Question>()
        val logs = ArrayList<String>()
        for ((i, raw) in lines.withIndex()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            try {
                val obj = JSONObject(line)
                val qText = obj.optString("q", obj.optString("question", obj.optString("题干", "")))
                val ansRaw = obj.optString("ans", obj.optString("answer", obj.optString("答案", "")))
                val arr: JSONArray? = obj.optJSONArray("a") ?: obj.optJSONArray("options")
                    ?: obj.optJSONArray("选项")
                val options = ArrayList<String>()
                if (arr != null) {
                    for (k in 0 until arr.length()) options.add(arr.optString(k))
                }
                if (qText.isBlank() || ansRaw.isBlank() || options.isEmpty()) {
                    logs.add("第${i + 1}行：缺少题干/选项/答案，已跳过")
                    continue
                }
                var ans = normalizeAnswer(formatAnswer(ansRaw))
                if (ans.isEmpty()) {
                    logs.add("第${i + 1}行：答案格式无法识别，已跳过")
                    continue
                }
                if (ans.length == 1 && options.size == 2 &&
                    (options[0] == "正确" || options[0] == "对") &&
                    (ansRaw.contains("对") || ansRaw.contains("正确") || ansRaw.contains("√"))
                ) {
                    ans = "A"
                }
                val idx = ANSWER_LABEL.indexOf(ans.last())
                if (idx < 0 || idx >= options.size) {
                    logs.add("第${i + 1}行：答案超出选项范围，已跳过")
                    continue
                }
                items.add(Question(q = qText.trim(), options = options, ans = ans, type = 0))
            } catch (e: Exception) {
                logs.add("第${i + 1}行：JSON 解析失败，已跳过")
            }
        }
        return Result(items, logs)
    }

    // ---------------- 表格（CSV/TSV） ----------------

    private fun parseTable(lines: List<String>): Result {
        val items = ArrayList<Question>()
        val logs = ArrayList<String>()
        for ((i, raw) in lines.withIndex()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val cells = splitCells(line)
            if (cells.size < 3) {
                logs.add("第${i + 1}行：列数不足（至少 题干+1个选项+答案），已跳过")
                continue
            }
            if (i == 0 && (cells[0].contains("题目") || cells[0].contains("题干"))) continue
            val qText = cells.first().trim()
            val ansRaw = cells.last().trim()
            val options = cells.subList(1, cells.size - 1).map { it.trim() }.filter { it.isNotEmpty() }
            val ans = normalizeAnswer(formatAnswer(ansRaw))
            if (qText.isEmpty() || options.isEmpty() || ans.isEmpty()) {
                logs.add("第${i + 1}行：内容不完整，已跳过")
                continue
            }
            val idx = ANSWER_LABEL.indexOf(ans.last())
            if (idx < 0 || idx >= options.size) {
                logs.add("第${i + 1}行：答案超出选项范围，已跳过")
                continue
            }
            items.add(Question(q = qText, options = options, ans = ans, type = 0))
        }
        return Result(items, logs)
    }

    private fun splitCells(line: String): List<String> {
        val sep = if (line.count { it == '\t' } >= line.count { it == ',' }) '\t' else ','
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inQuote = false
        for (ch in line) {
            when {
                ch == '"' -> inQuote = !inQuote
                ch == sep && !inQuote -> {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
                else -> sb.append(ch)
            }
        }
        out.add(sb.toString())
        return out
    }

    // ---------------- 分隔符 ----------------

    private fun parseDelimiter(lines: List<String>): Result {
        val items = ArrayList<Question>()
        val logs = ArrayList<String>()
        for ((i, raw) in lines.withIndex()) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            val parts = when {
                line.contains("#@=@#") -> line.split("#@=@#")
                line.contains("|") -> line.split("|")
                line.contains("##") -> line.split("##")
                else -> {
                    logs.add("第${i + 1}行：没找到分隔符 #@=@#，已跳过")
                    continue
                }
            }
            if (parts.size < 2) {
                logs.add("第${i + 1}行：字段不足，已跳过")
                continue
            }
            val qText = parts[0].trim()
            val ans = normalizeAnswer(formatAnswer(parts.last().trim()))
            val mid = parts.subList(1, parts.size - 1)
            val options = ArrayList<String>()
            for (m in mid) {
                if (m.contains("|")) options.addAll(m.split("|").map { it.trim() })
                else if (m.isNotBlank()) options.add(m.trim())
            }
            if (qText.isEmpty() || ans.isEmpty() || options.isEmpty()) {
                logs.add("第${i + 1}行：内容不完整，已跳过")
                continue
            }
            val idx = ANSWER_LABEL.indexOf(ans.last())
            if (idx < 0 || idx >= options.size) {
                logs.add("第${i + 1}行：答案超出选项范围，已跳过")
                continue
            }
            items.add(Question(q = qText, options = options, ans = ans, type = 0))
        }
        return Result(items, logs)
    }
}
