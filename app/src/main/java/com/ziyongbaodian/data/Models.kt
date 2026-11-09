package com.ziyongbaodian.data

/** 题库 */
data class Tiku(
    val id: Long,
    val name: String,
    val count: Int,
    val enabled: Boolean,
    val createdAt: Long
)

/** 题目 */
data class Question(
    val id: Long = 0L,
    val tid: Long = 0L,
    val q: String = "",
    val options: List<String> = emptyList(),
    val ans: String = "",
    val type: Int = 0,
    val fav: Boolean = false,
    val wrong: Boolean = false,
    val rightCount: Int = 0
) {
    /** 答案对应的选项下标 */
    val answerIndexes: List<Int>
        get() = ans.uppercase().filter { it in 'A'..'Z' }.map { it - 'A' }

    val isMultiple: Boolean get() = answerIndexes.size > 1

    /** 判断题：选项本身就是「正确/错误」，答案文字与选项内容重复，展示时无需再拼接选项 */
    val isJudge: Boolean
        get() = type == TYPE_PANDUAN ||
            (options.size == 2 && options[0].let { it == "正确" || it == "对" })

    fun typeName(): String = when {
        isJudge -> "判断题"
        isMultiple -> "多选题"
        else -> "单选题"
    }

    /** 按题型练习用的分类键，与 [TYPE_KEY_*] 对应 */
    fun typeKey(): String = when {
        isJudge -> TYPE_KEY_JUDGE
        isMultiple -> TYPE_KEY_MULTI
        else -> TYPE_KEY_SINGLE
    }

    /** 答案文本，如 "A"、"AC"、"正确" */
    fun answerLabel(): String {
        val idx = answerIndexes
        if (idx.isEmpty()) return ans
        if (isJudge) {
            return idx.joinToString("") { if (it == 0) "正确" else "错误" }
        }
        return idx.joinToString("") { ('A' + it).toString() }
    }

    /** 答案内容，如 "光合作用"；判断题返回空串 */
    fun answerContent(): String =
        if (isJudge) "" else answerIndexes.mapNotNull { options.getOrNull(it) }.joinToString("；")

    fun optionLabel(i: Int): String = ('A' + i).toString()

    companion object {
        const val TYPE_UNKNOWN = 0
        const val TYPE_SINGLE = 1
        const val TYPE_MULTI = 2
        const val TYPE_PANDUAN = 3

        const val TYPE_KEY_SINGLE = "single"
        const val TYPE_KEY_MULTI = "multi"
        const val TYPE_KEY_JUDGE = "judge"

        fun typeLabel(key: String): String = when (key) {
            TYPE_KEY_MULTI -> "多选题"
            TYPE_KEY_JUDGE -> "判断题"
            else -> "单选题"
        }
    }
}

data class HistoryItem(
    val id: Long,
    val keyword: String,
    val qid: Long,
    val time: Long,
    val question: String?
)

/** 最近练过的题 */
data class PracticeItem(
    val qid: Long,
    val time: Long,
    val question: String
)

data class Stats(
    val tikuCount: Int = 0,
    val questionCount: Int = 0,
    val favCount: Int = 0,
    val wrongCount: Int = 0
)

data class SearchHit(
    val question: Question,
    val score: Double,
    val lcsLen: Int,
    val ratio: Double
)
