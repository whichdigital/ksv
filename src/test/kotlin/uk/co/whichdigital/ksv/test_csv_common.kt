package uk.co.whichdigital.ksv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime


private const val DEFAULT_TOWN = "Tokio (default)"
private const val DEFAULT_NR = 96

@CsvRow
data class Row1(
    @CsvValue(name = "town") val a: String,
    @CsvValue var nr: Int?
)

@CsvRow
data class Row2(
    @CsvValue(name = "town") val a: String = DEFAULT_TOWN,
    @CsvValue var nr: Int? = DEFAULT_NR
)

@CsvRow
data class Row3(
    @CsvTimestamp(format = "[d][dd]/[M][MM]/yyyy") val date: LocalDate,
    @CsvTimestamp(name = "date_time", format = "yyyy-[M][MM]-[d][dd] [H][HH]:mm:ss") val dateTime: LocalDateTime?
)

@CsvRow
data class Row4(
    @CsvTimestamp(format = "dd/MM/yyyy|yyyy-MM-dd") val date: LocalDate
)

@CsvRow
data class Row5(
    @CsvGeneric(converterName = "fuzyBooleanConverter") val truthiness1: FuzyBoolean = FuzyBoolean.UNKNOWN,
    @CsvGeneric(converterName = "fuzyBooleanConverter") val truthiness2: FuzyBoolean?
)

enum class FuzyBoolean {
    YES, NO, MAYBE, UNKNOWN;
}

class TestParseCsv {

    @BeforeAll
    fun setup() {
        // for test case `test csv generic parsing`
        registerGenericConverter("fuzyBooleanConverter") { token: String ->
            try {
                FuzyBoolean.valueOf(token.toUpperCase())
            } catch (e: IllegalArgumentException) {
                FuzyBoolean.UNKNOWN
            }
        }
    }

    @ParameterizedTest
    @MethodSource("csvParsingTestDataProvider")
    fun `test basic csv parsing`(msg: String, csv: String, expectedRows: List<Row1>) {
        val actualRows: List<Row1> = csv2List(csv.toCsvSourceConfig())
        assertEquals(expectedRows, actualRows, msg)
    }

    @ParameterizedTest
    @MethodSource("csvParsingWithDefaultParamsTestDataProvider")
    fun `test csv parsing with parameter default values`(msg: String, csv: String, expectedRows: List<Row2>) {
        val actualRows: List<Row2> = csv2List(csv.toCsvSourceConfig())

        assertEquals(expectedRows, actualRows, msg)
    }

    @ParameterizedTest
    @MethodSource("csvTimestampParsingTestDataProvider")
    fun `test csv timestamp parsing`(msg: String, csv: String, expectedRows: List<Row3>) {
        val actualRows: List<Row3> = csv2List(csv.toCsvSourceConfig())

        assertEquals(expectedRows, actualRows, msg)
    }

    @ParameterizedTest
    @MethodSource("csvTimestampParsingInMultipleFormatsTestDataProvider")
    fun `test csv timestamp parsing with multiple formats`(msg: String, csv: String, expectedRows: List<Row4>) {
        val actualRows: List<Row4> = csv2List(csv.toCsvSourceConfig())

        assertEquals(expectedRows, actualRows, msg)
    }

    @ParameterizedTest
    @MethodSource("csvGenericTestDataProvider")
    fun `test csv generic parsing`(msg: String, csv: String, expectedRows: List<Row5>) {
        val actualRows: List<Row5> = csv2List(csv.toCsvSourceConfig())

        assertEquals(expectedRows, actualRows, msg)
    }

    companion object {
        @JvmStatic
        private fun csvParsingTestDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "basic functionality, expected: get two instances",
                    """
                            |town,nr
                            |Copenhagen,53
                            |Malmo, 64
                            """.trimMargin(),
                    listOf(
                        Row1("Copenhagen", 53),
                        Row1("Malmo", 64)
                    )
                ),
                Arguments.of(
                    "missing nullable value, expected: initialize value with null",
                    """
                            |town,nr
                            |Malmo,
                            """.trimMargin(),
                    listOf(
                        Row1("Malmo", null)
                    )
                )
            )
        }

        @JvmStatic
        private fun csvParsingWithDefaultParamsTestDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "missing non-nullable value with default value, expected: initialize value with default value",
                    """
                            |town,nr
                            |, 64
                            """.trimMargin(),
                    listOf(
                        Row2(
                            DEFAULT_TOWN,
                            64
                        )
                    )
                ),
                Arguments.of(
                    "missing nullable value with default value, expected: initialize value with default value",
                    """
                            |town,nr
                            |Cairo,
                            """.trimMargin(),
                    listOf(
                        Row2(
                            "Cairo",
                            DEFAULT_NR
                        )
                    )
                )
            )
        }

        @JvmStatic
        private fun csvTimestampParsingTestDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "converting timestamp values, expected: initialize value with timestamp value",
                    """
                    |date,date_time
                    |26/04/2018, 2019-03-27 10:15:30
                    |2/4/2018, 2019-3-7 8:15:30
                    """.trimMargin(),
                    listOf(
                        Row3(
                            LocalDate.of(2018, 4, 26),
                            LocalDateTime.of(2019, 3, 27, 10, 15, 30)
                        ),
                        Row3(
                            LocalDate.of(2018, 4, 2),
                            LocalDateTime.of(2019, 3, 7, 8, 15, 30)
                        )
                    )
                ),
                Arguments.of(
                    "converting nullable timestamp values, expected: missing value becomes null",
                    """
                    |date,date_time
                    |11/12/2015,
                    """.trimMargin(),
                    listOf(
                        Row3(
                            LocalDate.of(2015, 12, 11),
                            null
                        )
                    )
                )
            )
        }

        @JvmStatic
        private fun csvTimestampParsingInMultipleFormatsTestDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "converting timestamp values, expected: initialize value with timestamp value",
                    """
                    |date
                    |26/04/2018
                    |2013-10-12
                    """.trimMargin(),
                    listOf(
                        Row4(LocalDate.of(2018, 4, 26)),
                        Row4(LocalDate.of(2013, 10, 12))
                    )
                )
            )
        }

        @JvmStatic
        private fun csvGenericTestDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "converting generic values, expected: initialize value custom enum, use default FuzyBoolean.UNKNOWN if invalid or null token",
                    """
                    |truthiness1,truthiness2
                    |YES, yes
                    |NO, no
                    |MAYBE, maybe
                    |UNKNOWN, unknown
                    |truly new, even more different
                    |,
                    """.trimMargin(),
                    listOf(
                        Row5(
                            FuzyBoolean.YES,
                            FuzyBoolean.YES
                        ),
                        Row5(
                            FuzyBoolean.NO,
                            FuzyBoolean.NO
                        ),
                        Row5(
                            FuzyBoolean.MAYBE,
                            FuzyBoolean.MAYBE
                        ),
                        Row5(
                            FuzyBoolean.UNKNOWN,
                            FuzyBoolean.UNKNOWN
                        ),
                        Row5(
                            FuzyBoolean.UNKNOWN,
                            FuzyBoolean.UNKNOWN
                        ),
                        Row5(
                            FuzyBoolean.UNKNOWN,
                            null
                        )
                    )
                )
            )
        }

        //        @JvmStatic
//        private fun csvFilteringProviderMissingColumnName(): List<Arguments?> {
//            Arguments.of(
//                "Filtering will throw an Exception as a column name is missing",
//                """
//                            |town,number
//                            |Copenhagen,1
//                            |Copenhagen, 5
//                            """.trimMargin(),
//                listOf(
//                    Row1("Copenhagen", 1),
//                    Row1("Copenhagen", 5)
//                )
//            )
//        }
        @JvmStatic
        private fun csvFilteringTestProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "Filtering will throw an Exception as a column name is missing",
                    """
                            |town,nr
                            |Copenhagen,1
                            |Copenhagen, 5
                            """.trimMargin(),
                    listOf(
                        Row1("Copenhagen", 1),
                        Row1("Copenhagen", 5)
                    )
                ),
                Arguments.of(
                    "Filter will discard nothing, expected: get two instances",
                    """
                            |town,nr
                            |Copenhagen,1
                            |Copenhagen, 5
                            """.trimMargin(),
                    listOf(
                        Row1("Copenhagen", 1),
                        Row1("Copenhagen", 5)
                    )
                ), Arguments.of(
                    "Filter will discard Malmo, expected: get one instance",
                    """
                            |town,nr
                            |Copenhagen,1
                            |Malmo, 1
                            """.trimMargin(),
                    listOf(
                        Row1("Copenhagen", 1)
                    )
                ), Arguments.of(
                    "Filter will discard nr 2, expected: get one instance",
                    """
                            |town,nr
                            |Copenhagen,1
                            |Copenhagen, 2
                            """.trimMargin(),
                    listOf(
                        Row1("Copenhagen", 1)
                    )
                ),
                Arguments.of(
                    "filter will discard both Malmo and nr 2, expected: get 0 instances",
                    """
                            |town,nr
                            |Copenhagen,2
                            |Malmo, 1
                            """.trimMargin(),
                    emptyList<Row1>()
                )
            )
        }
    }
}
