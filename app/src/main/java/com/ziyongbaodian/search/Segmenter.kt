package com.ziyongbaodian.search

/**
 * 分词：中文按单字切分，英文/数字按连续串切分，过滤标点与空白。
 * 与原版 SegmentEngine 的策略保持一致（字符级倒排，适合题库模糊检索）。
 */
object Segmenter {

    private const val STOP = "\t【】，。,.、 ？?_;；：:…'\"\n（）()《》！!\"“”‘’-—·～~"

    const val ALPLIST = "ABCDEFGHIJKLMNOPQRST"

    fun cut(text: String): List<String> {
        if (text.isEmpty()) return emptyList()
        val out = ArrayList<String>(text.length)
        val sb = StringBuilder()
        for (ch in text) {
            if (STOP.indexOf(ch) >= 0) continue
            when (type(ch)) {
                1 -> sb.append(ch)
                2 -> {
                    if (sb.isNotEmpty()) {
                        out.add(sb.toString())
                        sb.setLength(0)
                    }
                    out.add(ch.toString())
                }
                else -> if (sb.isNotEmpty()) {
                    out.add(sb.toString())
                    sb.setLength(0)
                }
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }

    /** 1=字母数字 2=汉字 -1=其他 */
    fun type(c: Char): Int {
        if (isChinese(c)) return 2
        if (c in 'a'..'z') return 1
        if (c in 'A'..'Z') return 1
        if (c in '0'..'9') return 1
        return -1
    }

    private fun isChinese(c: Char): Boolean {
        val b = Character.UnicodeBlock.of(c) ?: return false
        return b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
            b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A ||
            b == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B ||
            b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
            b == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT
    }

    /** 去掉“1.”“A、”这类题号前缀 */
    fun stripNumberPrefix(text: String): String {
        val t = text.trim()
        if (t.isEmpty()) return t
        if (t.startsWith("出题：") || t.startsWith("推荐：") || t.startsWith("来源：")) return ""
        val parts = t.split(Regex("[. 、．]"), limit = 2)
        if (parts.size <= 1) return t
        val head = parts[0].trim()
        val rest = t.substring(head.length).trim().trimStart('.', '、', '．', ' ')
        return when {
            head.isNotEmpty() && head.all { it.isDigit() } -> rest
            head.length == 1 && ALPLIST.contains(head.uppercase()) -> rest
            else -> t
        }
    }

    fun isAnswerOnly(text: String): Boolean {
        if (text.isBlank()) return false
        return text.all { "ABCDEFGHIJK".indexOf(it) >= 0 }
    }
}
