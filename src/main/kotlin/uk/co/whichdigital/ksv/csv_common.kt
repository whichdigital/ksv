package uk.co.whichdigital.ksv

import uk.co.whichdigital.ksv.Util.toItemList


inline fun <reified T : Any> csv2List(
    sourceConfig: CsvSourceConfig,
    noinline logInvalidLine: ((line: String, msg: String)->Unit)? = null,
    noinline logRejectedRecord: ((record: String)->Unit)? = null,
    noinline logConversionError: ((record: String, msg: String)->Unit)? = null,
    noinline logSummary: ((
        invalidLineCount: Int,
        rejectedRecordCount: Int,
        conversionErrorCount: Int,
        itemsCreated: Int
    )->Unit)? = null
): List<T> {
    val itemFactory = ReflectiveItemFactory(T::class, sourceConfig.effectiveNormalizeColumnName)

    return csv2whatever(sourceConfig) {csvTable ->
        val header = csvTable.header
        csvTable.csvRecords.map { protoRecord ->
            when(protoRecord) {
                is ProtoCsvRecord.Failure -> ParsedCsvLine.InvalidLineError(protoRecord.line, protoRecord.msg)
                is ProtoCsvRecord.Success -> {
                    val record = protoRecord.record
                    if(!sourceConfig.keepCsvRecord(header, record)) {
                        ParsedCsvLine.RejectedRecord(record.toString())
                    } else {
                        when (val record2dataResult = record2Item(header, record, itemFactory)) {
                            is Record2ItemResult.ConversionException -> ParsedCsvLine.ConversionError(
                                record.toString(),
                                record2dataResult.e.toString()
                            )
                            is Record2ItemResult.Success -> ParsedCsvLine.Success(record2dataResult.item)
                        }
                    }
                }
            }
        }.toItemList(logInvalidLine, logRejectedRecord, logConversionError, logSummary)
    }
}

sealed class ParsedCsvLine<out T:Any> {
    class InvalidLineError(val line: String, val msg: String): ParsedCsvLine<Nothing>()
    class RejectedRecord(val record: String): ParsedCsvLine<Nothing>()
    class ConversionError(val record: String, val msg: String): ParsedCsvLine<Nothing>()
    class Success<out T:Any>(val item: T): ParsedCsvLine<T>()
}

inline fun <reified T : Any> csv2whatever(
    sourceConfig: CsvSourceConfig,
    consumeCsvTable: (CsvTable) -> T
): T {
    sourceConfig.bufferedReader().useLines { lines: Sequence<String> ->
        val lineIterator = lines.iterator()
        val headerLine = sourceConfig.fixLine(lineIterator.next())
        val header = CsvHeader(headerLine, sourceConfig.effectiveNormalizeColumnName, sourceConfig.splitByComma)
        return consumeCsvTable(
            CsvTable(
                header,
                lineIterator.asSequence().mapNotNull { line ->
                    val fixedLine = sourceConfig.fixLine(line)
                    if (fixedLine.isBlank()) return@mapNotNull null
                    return@mapNotNull CsvRecord.constructFrom(
                        fixedLine,
                        header.numberOrColumns,
                        sourceConfig.splitByComma
                    )
                }
            )
        )
    }
}

