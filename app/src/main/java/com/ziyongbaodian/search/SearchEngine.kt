package com.ziyongbaodian.search

import com.ziyongbaodian.data.Question
import com.ziyongbaodian.data.SearchHit
import kotlin.math.log10
import kotlin.math.max

/**
 * 本地题库检索引擎。
 * 思路与原版一致：字符级倒排索引 + IDF/TF 打分 → 候选集 → 最长公共子串二次打分。
 */
class SearchEngine {

    private val postings = HashMap<String, MutableSet<Long>>()
    private val tf = HashMap<Long, Double>()
    private val fullText = HashMap<Long, String>()
    private val byId = HashMap<Long, Question>()

    @Volatile
    var size: Int = 0
        private set

    @Volatile
    var ready: Boolean = false
        private set

    fun build(questions: List<Question>) {
        ready = false
        postings.clear()
        tf.clear()
        fullText.clear()
        byId.clear()
        size = 0

        for (q in questions) {
            val text = buildText(q)
            if (text.isBlank()) continue
            val tokens = Segmenter.cut(text)
            if (tokens.isEmpty()) continue
            tf[q.id] = 1.0 / tokens.size
            fullText[q.id] = text
            byId[q.id] = q
            for (t in tokens) {
                postings.getOrPut(t) { HashSet(4) }.add(q.id)
            }
            size++
        }
        ready = true
    }

    fun update(question: Question) {
        byId[question.id] = question
    }

    fun get(id: Long): Question? = byId[id]

    /** 用于 LCS 的全文：题干 + 选项 */
    fun buildText(q: Question): String {
        val sb = StringBuilder(norm(stripMeta(q.q)))
        for (o in q.options) {
            sb.append('\n').append(norm(o))
        }
        return sb.toString()
    }

    /**
     * 检索与索引共用的归一化：去掉空格与常见标点、统一小写。
     * 必须两侧对称处理，否则「1 KB」与「1KB」会在最长公共子串处被空格截断，
     * 导致几乎完全相同的题目只拿到很低的相似度。
     */
    fun norm(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (PUNCT.indexOf(c) >= 0) continue
            sb.append(if (c in 'A'..'Z') c + 32 else c)
        }
        return sb.toString()
    }

    private fun stripMeta(text: String): String {
        var t = text.replace(':', '：')
        val keys = arrayOf("来源：", "出题：", "推荐：")
        for (k in keys) {
            val i = t.indexOf(k)
            if (i > 0) t = t.substring(0, i)
        }
        return t
    }

    /** 检索 TopN，返回按置信度排序的结果 */
    fun search(query: String, topN: Int = 3): List<SearchHit> {
        if (!ready || size == 0) return emptyList()
        val clean = normQuery(query)
        if (clean.length < 2) return emptyList()

        val scores = HashMap<Long, Double>()
        val n = size.toDouble()
        for (seg in Segmenter.cut(clean)) {
            val set = postings[seg] ?: continue
            if (set.isEmpty()) continue
            val idf = if (n == set.size.toDouble()) 0.0 else log10(n / (set.size + 1.0))
            if (idf <= 0.0) continue
            for (id in set) {
                val w = tf[id] ?: 1.0
                scores[id] = (scores[id] ?: 0.0) + w * idf
            }
        }
        if (scores.isEmpty()) return emptyList()

        // 粗排：取分数最高的若干候选，交给 LCS 精排
        val candidates = scores.entries
            .sortedByDescending { it.value }
            .take(max(topN * 4, 8))
            .map { it.key }

        val hits = ArrayList<SearchHit>(candidates.size)
        for (id in candidates) {
            val q = byId[id] ?: continue
            val text = fullText[id] ?: continue
            val lcs = lcsString(clean, text)
            if (lcs.isEmpty()) continue
            var score = 0.0
            for (ch in lcs) {
                val set = postings[ch.toString()] ?: continue
                if (set.isEmpty()) continue
                val idf = if (n == set.size.toDouble()) 0.0 else log10(n / (set.size + 1.0))
                score += idf
            }
            val ratio = lcs.length.toDouble() / clean.length
            hits.add(SearchHit(q, score, lcs.length, ratio))
        }
        return hits.sortedWith(compareByDescending<SearchHit> { it.lcsLen }.thenByDescending { it.score })
            .take(topN)
    }

    /** 浮窗用：命中一道最可信的题 */
    fun best(query: String): SearchHit? {
        val list = search(query, 3)
        if (list.isEmpty()) return null
        val clean = normQuery(query)
        // 短题干兜底：题干完整出现在屏幕文本里就认。
        // 例：「每块椎骨均具有」只有 7 个字，而 App 打乱选项后 LCS 最长也就是这 7 个字，
        // 永远到不了下面 8 字的门槛，会被误判成「未匹配到」。
        stemMatched(clean, list)?.let { return it }
        val top = list[0]
        // 命中长度太短视为没搜到，避免答错题。
        // 注意：浮窗传进来的是整屏文字（含标题栏、页码等），query 很长，
        // 因此不能用命中长度占 query 的比例来判断，那样正常题目也会被误杀。
        if (top.lcsLen < 8) return null
        if (top.lcsLen < 14 && top.ratio < 0.18) return null
        return top
    }

    /**
     * 题干完整命中的兜底匹配：屏幕文本里必须原样包含题库题干（归一化后），
     * 多个候选时按「题干长度 + 选项文字命中数」排优先级。
     * 题干太短（<6 字）且一个选项文字都没对上时宁可不出结果，避免张冠李戴。
     */
    private fun stemMatched(clean: String, list: List<SearchHit>): SearchHit? {
        var pick: SearchHit? = null
        var pickScore = -1
        var pickStemLen = 0
        var pickOptHit = 0
        for (h in list) {
            val stem = norm(stripMeta(h.question.q))
            if (stem.length < 5 || !clean.contains(stem)) continue
            val optHit = h.question.options.count { clean.contains(norm(it)) }
            val score = stem.length * 2 + optHit * 3
            if (score > pickScore) {
                pick = h
                pickScore = score
                pickStemLen = stem.length
                pickOptHit = optHit
            }
        }
        if (pick == null) return null
        if (pickStemLen < 6 && pickOptHit == 0) return null
        return pick
    }

    /**
     * 查询文本归一化：先把独立成行的选项标记（屏幕上「A」「B」单独一个节点）丢掉，
     * 否则这些字母会插进相邻选项之间，把最长公共子串切断。
     * 只影响匹配，ScreenOptions 解析屏幕选项时仍用原始文本。
     */
    private fun normQuery(text: String): String {
        val sb = StringBuilder(text.length)
        for (line in text.split('\n')) {
            val t = line.trim()
            if (t.length == 1 && t[0] in 'A'..'Z') continue
            sb.append(t)
        }
        return norm(sb.toString())
    }

    fun lcsString(a: String, b: String): String {
        if (a.isEmpty() || b.isEmpty()) return ""
        val n = a.length
        val m = b.length
        // 超长文本截断，避免内存与耗时过大
        val aa = if (n > 600) a.substring(0, 600) else a
        val bb = if (m > 900) b.substring(0, 900) else b
        val dp = Array(aa.length + 1) { IntArray(bb.length + 1) }
        var maxLen = 0
        var endIndex = 0
        for (i in 1..aa.length) {
            for (j in 1..bb.length) {
                if (aa[i - 1] == bb[j - 1]) {
                    val v = dp[i - 1][j - 1] + 1
                    dp[i][j] = v
                    if (v > maxLen) {
                        maxLen = v
                        endIndex = i
                    }
                }
            }
        }
        if (maxLen == 0) return ""
        return aa.substring(endIndex - maxLen, endIndex)
    }

    private companion object {
        /** 归一化时丢弃的字符：空白 + 中英文常见标点 */
        const val PUNCT = " \t\r\n\u3000，,。.、；;：:？?！!\"“”'‘’（）()【】[]《》〈〉…—~～·|/\\-_+*#@&%^$="
    }
}
