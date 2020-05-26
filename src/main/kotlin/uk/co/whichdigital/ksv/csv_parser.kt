package uk.co.whichdigital.ksv


data class CsvTable(val header: CsvHeader, val csvRecords: Sequence<ProtoCsvRecord>)

class CsvHeader(
    private val line: String,
    private val normalizeColumnName: StringModifier,
    splitByComma: LineSplitter
) {
    val normalizedColumnNames: List<String> = line.splitByComma().map { normalizeColumnName(it) }
    val numberOrColumns: Int = normalizedColumnNames.size

    /**
     * @return the column indexes of these columnNames within the header, or null (instead of -1) if the columnName couldn't be found
     */
    fun getIndexesOrNullOf(vararg columnNames: String): List<Int?> = columnNames.map { columnName ->
        this.normalizedColumnNames.indexOf(
            normalizeColumnName(columnName)
        ).let { index ->
            if (index == -1) {
                null
            } else {
                index
            }
        }
    }

    /**
     * @return the positive column indexes of these columnNames within the header orderd in the same order than the columnNames in the parameter list
     *         or throws an IllegalArgumentException if not all columnNames where present
     */
    fun getIndexesOf(vararg columnNames: String): List<Int> = columnNames.map { columnName ->
        val index = this.normalizedColumnNames.indexOf(
            normalizeColumnName(columnName)
        )
        return@map if (index == -1) {
            IndexResult.NotFound(columnName)
        } else {
            IndexResult.Found(index)
        }
    }.let { indexResults: List<IndexResult> ->
        val foundIndexes = mutableListOf<Int>()
        val notFoundColumnNames = mutableListOf<String>()
        indexResults.forEach {
            when (it) {
                is IndexResult.Found -> foundIndexes.add(it.index)
                is IndexResult.NotFound -> notFoundColumnNames.add(it.columnNameThatWasNotFound)
            }
        }
        if (notFoundColumnNames.isNotEmpty()) {
            throw IllegalArgumentException(
                "couldn't find the columns named: ${notFoundColumnNames.joinToString(
                    prefix = "[",
                    postfix = "]"
                )} in header $this"
            )
        } else {
            return@let foundIndexes
        }
    }

    private sealed class IndexResult {
        class Found(val index: Int) : IndexResult()
        class NotFound(val columnNameThatWasNotFound: String) : IndexResult()
    }

    override fun toString() = "Header($line)"
}

class CsvRecord private constructor(
    private val elements: List<String>
) {
    private fun get(index: Int): String? = if (index < elements.size) elements[index] else null
    fun getAsNonBlankStringOrNull(index: Int): String? =get(index)?.let { if (it.isNotBlank()) it else null }

    override fun toString() = "CsvRecord(${elements.joinToString { "\"$it\"" }})"

    companion object {
        fun constructFrom(
            line: String,
            expectedNumberOfElements: Int,
            splitByComma: LineSplitter
        ): ProtoCsvRecord {
            val elements: List<String> = line.splitByComma()
            val actualSize = elements.size
            return if (expectedNumberOfElements != actualSize) {
                ProtoCsvRecord.Failure(
                    line,
                    "line doesn't contain as many arguments as expected! expected: $expectedNumberOfElements, actual: $actualSize"
                )
            } else {
                ProtoCsvRecord.Success(
                    CsvRecord(elements)
                )
            }
        }
    }
}

sealed class ProtoCsvRecord {
    class Success(val record: CsvRecord): ProtoCsvRecord()
    class Failure(val line: String, val msg: String): ProtoCsvRecord()
}