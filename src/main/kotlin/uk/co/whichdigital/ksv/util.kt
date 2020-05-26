package uk.co.whichdigital.ksv


// junit had problems with importing top-level functions (not in IntelliJ but in Gradle)
object Util {
    fun createLineSplitter(
        commaChar: Char = ',',
        quoteChar: Char = '"'
    ): LineSplitter = {
        val line = this
        var numberOfDoubleQuotesEncountered = 0
        val separatorCommaIndexes = mutableListOf(0)
        line.forEachIndexed { index, char ->
            when (char) {
                quoteChar -> {
                    numberOfDoubleQuotesEncountered++
                }
                commaChar -> {
                    if ((numberOfDoubleQuotesEncountered % 2) == 0) {
                        separatorCommaIndexes.add(index)
                    }
                }
            }
        }
        separatorCommaIndexes.add(line.length)

        mutableListOf<String>().apply {
            if (separatorCommaIndexes.size == 2) {
                add(line)
            } else {
                val commaIndexIter = separatorCommaIndexes.iterator()
                var startIndex = commaIndexIter.next()
                while (commaIndexIter.hasNext()) {
                    val endIndex = commaIndexIter.next()
                    add(line.substring(startIndex, endIndex))
                    startIndex = endIndex + 1
                }
            }
        }.map { it.trimThenTrimQuotesThenTrim(quoteChar) }
    }

    private fun String.trimThenTrimQuotesThenTrim(quoteChar: Char): String = this.trim().let { trimmed ->
        if (trimmed.startsWith(quoteChar) && trimmed.endsWith(quoteChar) && trimmed.length != 1) {
            trimmed.substring(1, trimmed.lastIndex).trim()
        } else {
            trimmed
        }
    }

    fun addTrimQuotesToNormalizeColumnNames(
        quoteChar: Char,
        originalNormalizeColumnNames: StringModifier
    ): StringModifier = {
        originalNormalizeColumnNames(it.trimThenTrimQuotesThenTrim(quoteChar))
    }

    fun <T:Any> Sequence<ParsedCsvLine<T>>.toItemList(
        logInvalidLine: ((line: String, msg: String)->Unit)? = null,
        logRejectedRecord: ((record: String)->Unit)? = null,
        logConversionError: ((record: String, msg: String)->Unit)? = null,
        logSummary: ((
            invalidLineCount: Int,
            rejectedRecordCount: Int,
            conversionErrorCount: Int,
            itemsCreated: Int
        )->Unit)? = null
    ): List<T> {

        var invalidLinesCount = 0
        var rejectedCsvRecordCount = 0
        var conversionExceptionCount = 0

        return this.mapNotNull { parsedCsvLine: ParsedCsvLine<T> ->
            when (parsedCsvLine) {
                is ParsedCsvLine.InvalidLineError -> {
                    invalidLinesCount++
                    logInvalidLine?.let { logInvalidLine(parsedCsvLine.line, parsedCsvLine.msg) }
                    null
                }
                is ParsedCsvLine.RejectedRecord -> {
                    rejectedCsvRecordCount++
                    logRejectedRecord?.let { logRejectedRecord(parsedCsvLine.record) }
                    null
                }
                is ParsedCsvLine.ConversionError -> {
                    conversionExceptionCount++
                    logConversionError?.let { logConversionError(parsedCsvLine.record, parsedCsvLine.msg) }
                    null
                }
                is ParsedCsvLine.Success -> {
                    parsedCsvLine.item
                }
            }
        }.toList().also {
            val itemsCreatedCount = it.size
            logSummary?.let {
                logSummary(
                    invalidLinesCount,
                    rejectedCsvRecordCount,
                    conversionExceptionCount,
                    itemsCreatedCount
                )
            }
        }
    }

    fun String.removeSpace() = this.replace(" ", "")
}