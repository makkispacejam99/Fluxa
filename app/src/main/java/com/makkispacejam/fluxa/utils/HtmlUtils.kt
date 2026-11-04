package com.makkispacejam.fluxa.utils

fun String.stripHtml(): String = this
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<p\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&nbsp;", " ")
    .replace(Regex("\n{3,}"), "\n\n")
    .trim()

fun String.restoreLineBreaks(): String {
    var result = replace("\r\n", "\n").replace('\r', '\n')

    val timecodeRegex = Regex("(?<!\\d)(?:\\d{1,3}:\\d{2}:\\d{2}|\\d{1,3}:\\d{2})(?=\\s|$)")
    val timecodes = timecodeRegex.findAll(result).toList()

    if (timecodes.size >= 2 && result.count { it == '\n' } == 0) {
        val builder = StringBuilder(result.length + timecodes.size)
        var cursor = 0
        timecodes.forEach { match ->
            val start = match.range.first
            builder.append(result, cursor, start)
            if (builder.length > 0 && builder[builder.length - 1] != '\n') builder.append('\n')
            cursor = start
        }
        builder.append(result, cursor, result.length)
        result = builder.toString()
    }

    return result
        .replace(Regex("[ \t]+(?=\n)"), "")
        .replace(Regex("(?<=\n)[ \t]+"), "")
        .replace(Regex("\n{3,}"), "\n\n")
        .trim()
}
