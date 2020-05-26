package uk.co.whichdigital.ksv

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType


class TestConvertToken {

    class TestNoAnnotation {
        @ParameterizedTest
        @MethodSource("no csv annotation to basic type TestDataProvider")
        fun `test no csv annotation to basic type conversion`(
            rowParam: CsvRowParam.ByNoAnnotation,
            token: String?,
            expectedValue: Any?
        ) {
            val actualValue: Any? = convert(token, rowParam)

            assertEquals(expectedValue, actualValue, "for token \"$token\"")
        }


        companion object {
            @JvmStatic
            private fun `no csv annotation to basic type TestDataProvider`(): List<Arguments?> {
                fun mockNoCsvAnnoForBasicTypes(clazz: KClass<*>) = mockk<CsvRowParam.ByNoAnnotation>().also {
                    every { it.paramType } returns mockk<KType>().also { every { it.classifier } returns clazz }
                }
                return listOf(
                    Arguments.of(mockNoCsvAnnoForBasicTypes(String::class), " some value ", "some value"),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Int::class), "123", 123),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Double::class), " 34.5 ", 34.5),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "true", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "yes", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "y", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "TRUE", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "YES", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "Y", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), " 1 ", true),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "false", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "no", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "n", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "0", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "MAYBE", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), "not yes", false),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(String::class), "  ", null),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Int::class), "     ", null),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Double::class), "  ", null),
                    Arguments.of(mockNoCsvAnnoForBasicTypes(Boolean::class), " ", null)
                )
            }
        }
    }

    class TestCsvValue {
        @ParameterizedTest
        @MethodSource("csvValue to basic type TestDataProvider")
        fun `test csvValue to basic type conversion`(
            rowParam: CsvRowParam.ByCsvValue,
            token: String?,
            expectedValue: Any?
        ) {
            val actualValue: Any? = convert(token, rowParam)

            assertEquals(expectedValue, actualValue, "for token \"$token\"")
        }


        companion object {
            @JvmStatic
            private fun `csvValue to basic type TestDataProvider`(): List<Arguments?> {
                fun mockCsvValueForBasicTypes(clazz: KClass<*>) = mockk<CsvRowParam.ByCsvValue>().also {
                    every { it.paramType } returns mockk<KType>().also { every { it.classifier } returns clazz }
                }
                return listOf(
                    Arguments.of(mockCsvValueForBasicTypes(String::class), " some value ", "some value"),
                    Arguments.of(mockCsvValueForBasicTypes(Int::class), "123", 123),
                    Arguments.of(mockCsvValueForBasicTypes(Double::class), " 34.5 ", 34.5),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "true", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "yes", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "y", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "TRUE", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "YES", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "Y", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), " 1 ", true),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "false", false),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "no", false),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "n", false),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "0", false),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "MAYBE", false),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), "not yes", false),
                    Arguments.of(mockCsvValueForBasicTypes(String::class), "  ", null),
                    Arguments.of(mockCsvValueForBasicTypes(Int::class), "     ", null),
                    Arguments.of(mockCsvValueForBasicTypes(Double::class), "  ", null),
                    Arguments.of(mockCsvValueForBasicTypes(Boolean::class), " ", null)
                )
            }
        }
    }

    class TestCsvTimestampConversion {
        @ParameterizedTest
        @MethodSource("csvTimestamp to LocalDateTime TestDataProvider")
        fun `test CsvTimestamp to LocalDateTime conversion`(
            rowParam: CsvRowParam.ByCsvTimestamp,
            token: String?,
            expectedDateTime: LocalDateTime?
        ) {
            val actualDateTime = convert(token, rowParam) as LocalDateTime?

            assertEquals(expectedDateTime, actualDateTime, "token: \"$token\", expected format: \"${rowParam.format}\"")
        }

        @ParameterizedTest
        @MethodSource("csvTimestamp to LocalDate TestDataProvider")
        fun `test CsvTimestamp to LocalDate conversion`(
            rowParam: CsvRowParam.ByCsvTimestamp,
            token: String?,
            expectedDate: LocalDate?
        ) {
            val actualDate = convert(token, rowParam) as LocalDate?

            assertEquals(expectedDate, actualDate, "token: \"$token\", expected format: \"${rowParam.format}\"")
        }


        companion object {
            @JvmStatic
            private fun `csvTimestamp to LocalDateTime TestDataProvider`(): List<Arguments?> {
                fun mockCsvTimestampForLocalDateTime(format: String) = mockk<CsvRowParam.ByCsvTimestamp>().also {
                    every { it.paramType } returns mockk<KType>().also { every { it.classifier } returns LocalDateTime::class }
                    every { it.format } returns format
                }
                return listOf(
                    Arguments.of(
                        mockCsvTimestampForLocalDateTime("yyyy/MM/dd - HH:mm|dd/MM/yyyy - HH:mm"),
                        "2018/09/21 - 00:00",
                        LocalDateTime.of(2018, 9, 21, 0, 0)
                    ),
                    Arguments.of(
                        mockCsvTimestampForLocalDateTime("yyyy/MM/dd - HH:mm"),
                        "2018/09/21 - 20:35",
                        LocalDateTime.of(2018, 9, 21, 20, 35)
                    ),
                    Arguments.of(
                        mockCsvTimestampForLocalDateTime("dd/MM/yyyy - HH:mm"),
                        "21/09/2018 - 11:35",
                        LocalDateTime.of(2018, 9, 21, 11, 35)
                    ),
                    Arguments.of(
                        mockCsvTimestampForLocalDateTime("yyyy/MM/dd - HH:mm"),
                        "",
                        null
                    )
                )
            }

            @JvmStatic
            private fun `csvTimestamp to LocalDate TestDataProvider`(): List<Arguments?> {
                fun mockCsvTimestampForLocalDate(format: String) = mockk<CsvRowParam.ByCsvTimestamp>().also {
                    every { it.paramType } returns mockk<KType>().also { every { it.classifier } returns LocalDate::class }
                    every { it.format } returns format
                }
                return listOf(
                    Arguments.of(
                        mockCsvTimestampForLocalDate("yyyy/MM/dd"),
                        "2018/09/21",
                        LocalDate.of(2018, 9, 21)
                    ),
                    Arguments.of(
                        mockCsvTimestampForLocalDate("yyyy/MM/dd"),
                        "",
                        null
                    ),
                    Arguments.of(
                        mockCsvTimestampForLocalDate("dd/MM/yyyy - HH:mm"),
                        "21/09/2018 - 00:00",
                        LocalDate.of(2018, 9, 21)
                    )
                )
            }

        }
    }

    class TestCsvGeneric {
        private fun mockCsvGeneric(
            converterName: String,
            clazz: KClass<*>
        ) = mockk<CsvRowParam.ByCsvGeneric>().also {
            every { it.paramType } returns mockk<KType>().also { every { it.classifier } returns clazz }
            every { it.converterName } returns converterName
        }

        @Test
        fun `test csvGeneric to basic type conversion`() {
            registerGenericConverter("gooToTrue") {
                it == "goo"
            }
            val gooToTrueCsvRowParam = mockCsvGeneric("gooToTrue", Boolean::class)

            assertTrue(convert("goo", gooToTrueCsvRowParam) as Boolean)
            assertFalse(convert("GOO", gooToTrueCsvRowParam) as Boolean)
        }
    }
}
