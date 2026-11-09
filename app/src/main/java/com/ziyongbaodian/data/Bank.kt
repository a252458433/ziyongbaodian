package com.ziyongbaodian.data

import android.content.Context
import com.ziyongbaodian.search.QParser
import com.ziyongbaodian.search.SearchEngine
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors

/**
 * 题库内存缓存 + 搜索索引。导入/删除后调用 reload()。
 */
object Bank {

    val engine = SearchEngine()

    @Volatile
    var tikus: List<Tiku> = emptyList()
        private set

    @Volatile
    var questions: List<Question> = emptyList()
        private set

    @Volatile
    var ready: Boolean = false
        private set

    @Volatile
    var loading: Boolean = false
        private set

    private val executor = Executors.newSingleThreadExecutor()
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    fun addListener(l: () -> Unit) {
        if (!listeners.contains(l)) listeners.add(l)
    }

    fun removeListener(l: () -> Unit) {
        listeners.remove(l)
    }

    private fun notifyChanged() {
        for (l in listeners) {
            try {
                l()
            } catch (_: Exception) {
            }
        }
    }

    fun reload(onDone: (() -> Unit)? = null) {
        if (loading) return
        loading = true
        executor.execute {
            try {
                val list = Db.tikuList()
                val qs = Db.allQuestions()
                tikus = list
                questions = qs
                engine.build(qs)
                ready = true
            } catch (e: Exception) {
                ready = true
            } finally {
                loading = false
            }
            notifyChanged()
            if (onDone != null) android.os.Handler(android.os.Looper.getMainLooper()).post(onDone)
        }
    }

    fun refreshQuestions() {
        executor.execute {
            try {
                questions = Db.allQuestions()
                tikus = Db.tikuList()
                engine.build(questions)
                ready = true
            } catch (_: Exception) {
            }
            notifyChanged()
        }
    }

    fun search(query: String, topN: Int = Prefs.topN): List<SearchHit> = engine.search(query, topN)

    fun best(query: String): SearchHit? = engine.best(query)

    /** 首次启动导入内置示例题库，保证开箱可搜 */
    fun ensureDemoBank(context: Context, done: (() -> Unit)? = null) {
        executor.execute {
            if (Db.stats().tikuCount == 0) {
                try {
                    val text = context.assets.open("demo_bank.txt").bufferedReader().use { it.readText() }
                    val result = QParser.parse(text, QParser.Format.STANDARD)
                    if (result.items.isNotEmpty()) {
                        Db.insertTiku("示例题库 · 计算机基础", result.items)
                    }
                } catch (_: Exception) {
                }
            }
            val qs = Db.allQuestions()
            tikus = Db.tikuList()
            questions = qs
            engine.build(qs)
            ready = true
            loading = false
            notifyChanged()
            if (done != null) android.os.Handler(android.os.Looper.getMainLooper()).post(done)
        }
    }
}
