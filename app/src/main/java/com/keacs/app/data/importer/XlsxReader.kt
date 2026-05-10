package com.keacs.app.data.importer

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

internal object XlsxReader {
    fun readRows(inputStream: InputStream): List<Map<Int, String>> {
        val entries = unzip(inputStream)
        val sharedStrings = parseSharedStrings(entries["xl/sharedStrings.xml"])
        val sheetPath = firstSheetPath(entries) ?: "xl/worksheets/sheet1.xml"
        val sheetBytes = entries[sheetPath] ?: error("没有找到第一个工作表")
        return parseSheet(sheetBytes, sharedStrings)
    }

    private fun unzip(inputStream: InputStream): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) result[entry.name] = zip.readBytes()
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }

    private fun firstSheetPath(entries: Map<String, ByteArray>): String? {
        val workbook = entries["xl/workbook.xml"] ?: return null
        val firstRid = firstSheetRid(workbook) ?: return null
        val rels = entries["xl/_rels/workbook.xml.rels"] ?: return null
        val target = relationTarget(rels, firstRid) ?: return null
        return if (target.startsWith("xl/")) target else "xl/$target"
    }

    private fun firstSheetRid(bytes: ByteArray): String? {
        val parser = parser(bytes)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.endsWith("sheet")) {
                return parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "id")
                    ?: parser.getAttributeValue(null, "r:id")
            }
        }
        return null
    }

    private fun relationTarget(bytes: ByteArray, rid: String): String? {
        val parser = parser(bytes)
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name.endsWith("Relationship")) {
                if (parser.getAttributeValue(null, "Id") == rid) return parser.getAttributeValue(null, "Target")
            }
        }
        return null
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val parser = parser(bytes)
        val result = mutableListOf<String>()
        var inItem = false
        var readingText = false
        val builder = StringBuilder()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when {
                parser.eventType == XmlPullParser.START_TAG && parser.name.endsWith("si") -> {
                    inItem = true
                    builder.clear()
                }
                parser.eventType == XmlPullParser.START_TAG && inItem && parser.name.endsWith("t") -> readingText = true
                parser.eventType == XmlPullParser.TEXT && readingText -> builder.append(parser.text)
                parser.eventType == XmlPullParser.END_TAG && parser.name.endsWith("t") -> readingText = false
                parser.eventType == XmlPullParser.END_TAG && parser.name.endsWith("si") -> {
                    result.add(builder.toString())
                    inItem = false
                }
            }
        }
        return result
    }

    private fun parseSheet(bytes: ByteArray, sharedStrings: List<String>): List<Map<Int, String>> {
        val parser = parser(bytes)
        val rows = mutableListOf<MutableMap<Int, String>>()
        var currentRow: MutableMap<Int, String>? = null
        var currentColumn = -1
        var currentType: String? = null
        var readingValue = false
        val value = StringBuilder()

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            when {
                parser.eventType == XmlPullParser.START_TAG && parser.name.endsWith("row") -> currentRow = mutableMapOf()
                parser.eventType == XmlPullParser.START_TAG && parser.name.endsWith("c") -> {
                    currentColumn = columnIndex(parser.getAttributeValue(null, "r").orEmpty())
                    currentType = parser.getAttributeValue(null, "t")
                    value.clear()
                }
                parser.eventType == XmlPullParser.START_TAG && (parser.name.endsWith("v") || parser.name.endsWith("t")) -> {
                    readingValue = true
                }
                parser.eventType == XmlPullParser.TEXT && readingValue -> value.append(parser.text)
                parser.eventType == XmlPullParser.END_TAG && (parser.name.endsWith("v") || parser.name.endsWith("t")) -> {
                    readingValue = false
                }
                parser.eventType == XmlPullParser.END_TAG && parser.name.endsWith("c") -> {
                    val raw = value.toString()
                    val text = if (currentType == "s") sharedStrings.getOrNull(raw.toIntOrNull() ?: -1).orEmpty() else raw
                    if (currentColumn >= 0 && text.isNotBlank()) currentRow?.put(currentColumn, text.trim())
                }
                parser.eventType == XmlPullParser.END_TAG && parser.name.endsWith("row") -> {
                    currentRow?.let { rows.add(it) }
                    currentRow = null
                }
            }
        }
        return rows
    }

    private fun parser(bytes: ByteArray): XmlPullParser =
        Xml.newPullParser().apply {
            setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
            setInput(ByteArrayInputStream(bytes), "UTF-8")
        }

    private fun columnIndex(cellRef: String): Int {
        var result = 0
        val letters = cellRef.takeWhile { it.isLetter() }
        if (letters.isBlank()) return -1
        letters.forEach { result = result * 26 + (it.uppercaseChar() - 'A' + 1) }
        return result - 1
    }
}
