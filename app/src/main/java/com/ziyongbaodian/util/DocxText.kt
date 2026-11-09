package com.ziyongbaodian.util

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * 从 .docx（OOXML 的 zip 包）里抽出纯文本，一段一行。
 *
 * 不依赖第三方库：docx 本质是 zip，正文在 word/document.xml 里，
 * 按 w:p 段落换行、w:tab 转制表符、w:br 转换行即可还原成题库文本。
 */
object DocxText {

    /** zip 头 "PK\x03\x04"：docx / xlsx / apk 都是这个头 */
    fun isZip(bytes: ByteArray): Boolean =
        bytes.size > 4 &&
            bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte() &&
            bytes[2] == 0x03.toByte() && bytes[3] == 0x04.toByte()

    /** 读取 docx 正文；不是 docx 或没有正文时返回 null */
    fun extract(bytes: ByteArray): String? {
        val xml = readDocumentXml(ByteArrayInputStream(bytes)) ?: return null
        val text = xmlToText(xml)
        return if (text.isBlank()) null else text
    }

    private fun readDocumentXml(input: InputStream): ByteArray? {
        try {
            ZipInputStream(input.buffered()).use { zis ->
                var entry = zis.nextEntry
                var guard = 0
                while (entry != null && guard++ < 5000) {
                    val name = entry.name ?: ""
                    if (name == "word/document.xml" || name.endsWith("/word/document.xml")) {
                        return zis.readBytes()
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }
        return null
    }

    private fun xmlToText(xml: ByteArray): String {
        val sb = StringBuilder()
        try {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(ByteArrayInputStream(xml), "UTF-8")
            var event = parser.eventType
            var inText = false
            while (event != XmlPullParser.END_DOCUMENT) {
                // 未开命名空间处理时标签名带前缀（w:p / w:t），取冒号后的本地名
                val tag = parser.name?.substringAfterLast(':')
                when (event) {
                    XmlPullParser.START_TAG -> when (tag) {
                        "t" -> inText = true
                        "tab" -> sb.append('\t')
                        "br", "cr" -> sb.append('\n')
                    }
                    XmlPullParser.END_TAG -> when (tag) {
                        "t" -> inText = false
                        "p" -> sb.append('\n')          // 段落结束
                        "tc" -> sb.append('\t')         // 单元格之间用制表符
                        "tr", "tbl" -> sb.append('\n')  // 表格行结束
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> if (inText) sb.append(parser.text)
                }
                event = parser.next()
            }
        } catch (_: Exception) {
        }
        return sb.toString()
    }

    /** 给用户看的文件类型判断 */
    fun kindOf(fileName: String, bytes: ByteArray): Kind {
        val lower = fileName.lowercase()
        if (isZip(bytes)) {
            return when {
                lower.endsWith(".docx") || lower.endsWith(".docm") || lower.endsWith(".dotx") -> Kind.DOCX
                lower.endsWith(".xlsx") || lower.endsWith(".xlsm") -> Kind.XLSX
                lower.endsWith(".pptx") -> Kind.PPTX
                else -> Kind.ARCHIVE
            }
        }
        if (lower.endsWith(".doc") || lower.endsWith(".wps")) return Kind.OLD_DOC
        return Kind.TEXT
    }

    enum class Kind { TEXT, DOCX, OLD_DOC, XLSX, PPTX, ARCHIVE }
}
