package uk.co.whichdigital.ksv

fun String.toCsvSourceConfig(
    commaChar: Char = ',',
    quoteChar: Char = '"',
    fixLine: StringModifier = ::removeBomChars,
    keepCsvRecord: RecordPredicate = keepAll,
    normalizeColumnName: StringModifier = ::toLowerCaseAndRemoveSpaceAndQuotes
) = CsvSourceConfig(
    stream = this.byteInputStream(),
    charset = Charsets.UTF_8,
    commaChar = commaChar,
    quoteChar = quoteChar,
    fixLine = fixLine,
    keepCsvRecord = keepCsvRecord,
    normalizeColumnName = normalizeColumnName
)