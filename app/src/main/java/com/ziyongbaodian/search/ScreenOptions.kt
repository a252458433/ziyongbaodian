package com.ziyongbaodian.search

/**
 * 从无障碍读到的屏幕文字里解析出「屏幕上真实显示的选项」。
 *
 * 有些 App（如某些刷题/考试 App）每次进入题目都会打乱选项顺序，
 * 题库里记的「答案是 B」到屏幕上可能已经变成 C。
 * 这里把屏幕上的 A/B/C/D 与内容抓出来，浮窗再用答案内容反查屏幕上的字母。
 */
object ScreenOptions {

    data class Opt(val label: Char, val text: String)

    private const val LABELS = "ABCDEFGH"

    /** 选项标记：字母前面不能紧挨着别的字母或数字 */
    private val MARKER = Regex("(?<![A-Za-z0-9])([A-H])(?=[、.．,，:：)）\\]]|\\s|$)")

    /** 去掉标记后面的分隔符与空白 */
    private val LEAD = Regex("^[\\s、.．,，:：)）\\]]+")

    fun parse(raw: String): List<Opt> {
        if (raw.isBlank()) return emptyList()
        val marks = MARKER.findAll(raw).toList()
        if (marks.isEmpty()) return emptyList()
        val out = ArrayList<Opt>()
        var expect = 0
        for (m in marks) {
            if (expect >= LABELS.length) break
            val label = m.groupValues[1][0]
            if (label != LABELS[expect]) continue
            val nextLabel = if (expect + 1 < LABELS.length) LABELS[expect + 1] else null
            var end = raw.length
            if (nextLabel != null) {
                for (m2 in marks) {
                    if (m2.range.first <= m.range.first) continue
                    if (m2.groupValues[1][0] == nextLabel) {
                        end = m2.range.first
                        break
                    }
                }
            }
            val text = LEAD.replace(raw.substring(m.range.last + 1, end), "").trim()
            out.add(Opt(label, text))
            expect++
        }
        return out
    }

    /** 比较用的归一化：去掉空白与常见标点，忽略大小写 */
    fun norm(s: String): String = s.lowercase()
        .replace(Regex("[\\s\\u3000]"), "")
        .replace(Regex("[，,。.、；;：:（）()\\[\\]【】\"'“”‘’!！?？·…—\\-]"), "")
}
