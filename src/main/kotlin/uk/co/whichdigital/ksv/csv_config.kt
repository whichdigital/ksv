package uk.co.whichdigital.ksv

import uk.co.whichdigital.ksv.Util.addTrimQuotesToNormalizeColumnNames
import uk.co.whichdigital.ksv.Util.createLineSplitter
import uk.co.whichdigital.ksv.Util.removeSpace
import java.io.InputStream
import java.nio.charset.Charset


data class CsvSourceConfig (
    val stream: InputStream,
    val charset: Charset = Charsets.UTF_8,
    private val commaChar: Char = ',',
    private val quoteChar: Char = '"',
    val fixLine: StringModifier = ::removeBomChars,
    val keepCsvRecord: (CsvHeader, CsvRecord) -> Boolean = keepAll,
    private val normalizeColumnName: StringModifier = ::toLowerCaseAndRemoveSpaceAndQuotes
) {
    val splitByComma: LineSplitter = createLineSplitter(commaChar, quoteChar)
    val effectiveNormalizeColumnName: StringModifier = addTrimQuotesToNormalizeColumnNames(quoteChar, normalizeColumnName)
}

fun CsvSourceConfig.bufferedReader() = stream.bufferedReader(charset)

internal fun toLowerCaseAndRemoveSpaceAndQuotes(s: String) = s.toLowerCase().removeSpace()
// removing the possible UTF-8 BOM character at the start of each line
internal fun removeBomChars(s: String) = s.trimStart('\uFEFF', '\u200B')
internal val keepAll: RecordPredicate = { _, _ -> true }

typealias RecordPredicate = (CsvHeader, CsvRecord) -> Boolean
typealias LineSplitter = String.() -> List<String>
typealias StringModifier = (String) -> String