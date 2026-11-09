package com.ziyongbaodian.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/** 本地 SQLite：题库 / 题目 / 搜题记录，全部在本机 */
object Db {
    private const val DB_NAME = "souti.db"
    private const val VERSION = 2
    private const val SEP = "\t"

    private lateinit var helper: Helper

    fun init(context: Context) {
        helper = Helper(context.applicationContext)
    }

    private class Helper(context: Context) :
        SQLiteOpenHelper(context, DB_NAME, null, VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(
                "CREATE TABLE tiku(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "name TEXT NOT NULL, created_at INTEGER, enabled INTEGER DEFAULT 1)"
            )
            db.execSQL(
                "CREATE TABLE question(_id INTEGER PRIMARY KEY AUTOINCREMENT, tid INTEGER, " +
                    "q TEXT, options TEXT, ans TEXT, type INTEGER DEFAULT 0, " +
                    "fav INTEGER DEFAULT 0, wrong INTEGER DEFAULT 0, right_cnt INTEGER DEFAULT 0)"
            )
            db.execSQL("CREATE INDEX idx_q_tid ON question(tid)")
            db.execSQL(
                "CREATE TABLE history(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "keyword TEXT, qid INTEGER, created_at INTEGER)"
            )
            db.execSQL(
                "CREATE TABLE practice(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "qid INTEGER, created_at INTEGER)"
            )
            db.execSQL("CREATE INDEX idx_p_qid ON practice(qid)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            // v2：新增「最近在练的题」记录表
            if (oldVersion < 2) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS practice(_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "qid INTEGER, created_at INTEGER)"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_p_qid ON practice(qid)")
            }
        }
    }

    // ---------------- 题库 ----------------

    fun tikuList(): List<Tiku> {
        val list = ArrayList<Tiku>()
        val sql = "SELECT t._id, t.name, t.created_at, t.enabled, COUNT(q._id) AS c " +
            "FROM tiku t LEFT JOIN question q ON q.tid = t._id GROUP BY t._id ORDER BY t.created_at DESC"
        helper.readableDatabase.rawQuery(sql, null).use { c ->
            while (c.moveToNext()) {
                list.add(
                    Tiku(
                        id = c.getLong(0),
                        name = c.getString(1) ?: "未命名题库",
                        createdAt = c.getLong(2),
                        enabled = c.getInt(3) == 1,
                        count = c.getInt(4)
                    )
                )
            }
        }
        return list
    }

    fun insertTiku(name: String, questions: List<Question>): Long {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("name", name)
                put("created_at", System.currentTimeMillis())
                put("enabled", 1)
            }
            val tid = db.insert("tiku", null, cv)
            var i = 0
            for (q in questions) {
                if (q.q.isBlank()) continue
                val v = ContentValues().apply {
                    put("tid", tid)
                    put("q", q.q)
                    put("options", q.options.joinToString(SEP))
                    put("ans", q.ans.uppercase())
                    put("type", q.type)
                    put("fav", if (q.fav) 1 else 0)
                    put("wrong", if (q.wrong) 1 else 0)
                    put("right_cnt", q.rightCount)
                }
                db.insert("question", null, v)
                i++
            }
            db.setTransactionSuccessful()
            return if (i == 0) -1L else tid
        } finally {
            db.endTransaction()
        }
    }

    fun deleteTiku(id: Long) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("question", "tid = ?", arrayOf(id.toString()))
            db.delete("tiku", "_id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun setTikuEnabled(id: Long, enabled: Boolean) {
        val cv = ContentValues().apply { put("enabled", if (enabled) 1 else 0) }
        helper.writableDatabase.update("tiku", cv, "_id = ?", arrayOf(id.toString()))
    }

    fun renameTiku(id: Long, name: String) {
        val cv = ContentValues().apply { put("name", name) }
        helper.writableDatabase.update("tiku", cv, "_id = ?", arrayOf(id.toString()))
    }

    // ---------------- 题目 ----------------

    fun allQuestions(): List<Question> {
        val list = ArrayList<Question>()
        helper.readableDatabase.rawQuery(
            "SELECT q._id, q.tid, q.q, q.options, q.ans, q.type, q.fav, q.wrong, q.right_cnt " +
                "FROM question q JOIN tiku t ON t._id = q.tid WHERE t.enabled = 1 ORDER BY q._id",
            null
        ).use { c ->
            while (c.moveToNext()) list.add(readQuestion(c))
        }
        return list
    }

    fun allQuestionsIncludeDisabled(): List<Question> {
        val list = ArrayList<Question>()
        helper.readableDatabase.rawQuery(
            "SELECT _id, tid, q, options, ans, type, fav, wrong, right_cnt FROM question ORDER BY _id",
            null
        ).use { c ->
            while (c.moveToNext()) list.add(readQuestion(c))
        }
        return list
    }

    fun questionsOf(tid: Long): List<Question> {
        val list = ArrayList<Question>()
        helper.readableDatabase.rawQuery(
            "SELECT _id, tid, q, options, ans, type, fav, wrong, right_cnt FROM question " +
                "WHERE tid = ? ORDER BY _id",
            arrayOf(tid.toString())
        ).use { c ->
            while (c.moveToNext()) list.add(readQuestion(c))
        }
        return list
    }

    fun favoriteQuestions(): List<Question> = queryWhere("fav = 1 ORDER BY _id DESC")

    fun wrongQuestions(): List<Question> = queryWhere("wrong = 1 ORDER BY _id DESC")

    private fun queryWhere(where: String): List<Question> {
        val list = ArrayList<Question>()
        helper.readableDatabase.rawQuery(
            "SELECT _id, tid, q, options, ans, type, fav, wrong, right_cnt FROM question WHERE $where",
            null
        ).use { c ->
            while (c.moveToNext()) list.add(readQuestion(c))
        }
        return list
    }

    fun questionById(id: Long): Question? {
        helper.readableDatabase.rawQuery(
            "SELECT _id, tid, q, options, ans, type, fav, wrong, right_cnt FROM question WHERE _id = ?",
            arrayOf(id.toString())
        ).use { c -> if (c.moveToNext()) return readQuestion(c) }
        return null
    }

    private fun readQuestion(c: android.database.Cursor): Question {
        val optionsStr = c.getString(3) ?: ""
        val options = if (optionsStr.isEmpty()) emptyList() else optionsStr.split(SEP)
        return Question(
            id = c.getLong(0),
            tid = c.getLong(1),
            q = c.getString(2) ?: "",
            options = options,
            ans = c.getString(4) ?: "",
            type = c.getInt(5),
            fav = c.getInt(6) == 1,
            wrong = c.getInt(7) == 1,
            rightCount = c.getInt(8)
        )
    }

    fun setFav(id: Long, fav: Boolean) {
        val cv = ContentValues().apply { put("fav", if (fav) 1 else 0) }
        helper.writableDatabase.update("question", cv, "_id = ?", arrayOf(id.toString()))
    }

    fun setWrong(id: Long, wrong: Boolean, resetCount: Boolean = true) {
        val cv = ContentValues().apply {
            put("wrong", if (wrong) 1 else 0)
            if (resetCount) put("right_cnt", 0)
        }
        helper.writableDatabase.update("question", cv, "_id = ?", arrayOf(id.toString()))
    }

    fun bumpRightCount(id: Long): Int {
        val db = helper.writableDatabase
        db.execSQL("UPDATE question SET right_cnt = right_cnt + 1 WHERE _id = ?", arrayOf(id))
        db.rawQuery("SELECT right_cnt FROM question WHERE _id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToNext()) return c.getInt(0)
        }
        return 0
    }

    // ---------------- 搜题记录 ----------------

    fun addHistory(keyword: String, qid: Long) {
        if (keyword.isBlank()) return
        val db = helper.writableDatabase
        val k = keyword.take(120)
        // 同一道题（或同一段搜索词）只保留最新一条，避免最近搜题里堆满重复项
        if (qid > 0) db.delete("history", "qid = ?", arrayOf(qid.toString()))
        db.delete("history", "keyword = ?", arrayOf(k))
        val cv = ContentValues().apply {
            put("keyword", k)
            put("qid", qid)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("history", null, cv)
        db.execSQL(
            "DELETE FROM history WHERE _id NOT IN (SELECT _id FROM history ORDER BY created_at DESC LIMIT 60)"
        )
    }

    fun history(limit: Int = 20): List<HistoryItem> {
        val list = ArrayList<HistoryItem>()
        val sql = "SELECT h._id, h.keyword, h.qid, h.created_at, q.q FROM history h " +
            "LEFT JOIN question q ON q._id = h.qid " +
            "WHERE h._id IN (SELECT MAX(_id) FROM history " +
            "GROUP BY CASE WHEN qid > 0 THEN 'q' || qid ELSE 'k' || keyword END) " +
            "ORDER BY h.created_at DESC LIMIT ?"
        helper.readableDatabase.rawQuery(sql, arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = c.getLong(0),
                        keyword = c.getString(1) ?: "",
                        qid = c.getLong(2),
                        time = c.getLong(3),
                        question = c.getString(4)
                    )
                )
            }
        }
        return list
    }

    fun clearHistory() {
        helper.writableDatabase.delete("history", null, null)
    }

    // ---------------- 练习记录 ----------------

    /** 记一道「刚练过」的题，同一题只留最新一条 */
    fun addPractice(qid: Long) {
        if (qid <= 0) return
        val db = helper.writableDatabase
        db.delete("practice", "qid = ?", arrayOf(qid.toString()))
        val cv = ContentValues().apply {
            put("qid", qid)
            put("created_at", System.currentTimeMillis())
        }
        db.insert("practice", null, cv)
        db.execSQL(
            "DELETE FROM practice WHERE _id NOT IN " +
                "(SELECT _id FROM practice ORDER BY created_at DESC LIMIT 60)"
        )
    }

    /** 最近练过的题（按时间倒序，题已删除的自动跳过） */
    fun recentPractice(limit: Int = 6): List<PracticeItem> {
        val list = ArrayList<PracticeItem>()
        // 同一道题练过多次只保留最近一次，避免首页出现重复条目
        val sql = "SELECT p.qid, MAX(p.created_at) AS t, q.q FROM practice p " +
            "JOIN question q ON q._id = p.qid GROUP BY p.qid ORDER BY t DESC LIMIT ?"
        helper.readableDatabase.rawQuery(sql, arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) {
                list.add(
                    PracticeItem(
                        qid = c.getLong(0),
                        time = c.getLong(1),
                        question = c.getString(2) ?: ""
                    )
                )
            }
        }
        return list
    }

    fun clearPractice() {
        helper.writableDatabase.delete("practice", null, null)
    }

    // ---------------- 统计 / 清理 ----------------

    fun stats(): Stats {
        val db = helper.readableDatabase
        var tiku = 0
        var total = 0
        var fav = 0
        var wrong = 0
        db.rawQuery("SELECT COUNT(*) FROM tiku", null).use { if (it.moveToNext()) tiku = it.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM question", null).use { if (it.moveToNext()) total = it.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM question WHERE fav = 1", null).use { if (it.moveToNext()) fav = it.getInt(0) }
        db.rawQuery("SELECT COUNT(*) FROM question WHERE wrong = 1", null).use { if (it.moveToNext()) wrong = it.getInt(0) }
        return Stats(tiku, total, fav, wrong)
    }

    fun clearAll() {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            db.delete("question", null, null)
            db.delete("tiku", null, null)
            db.delete("history", null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun questionCount(tid: Long): Int {
        helper.readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM question WHERE tid = ?", arrayOf(tid.toString())
        ).use { if (it.moveToNext()) return it.getInt(0) }
        return 0
    }
}
