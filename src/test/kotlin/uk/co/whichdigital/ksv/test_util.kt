package uk.co.whichdigital.ksv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import uk.co.whichdigital.ksv.Util.createLineSplitter

class TestUtil {

    @ParameterizedTest
    @MethodSource("createLineSplitterDataProvider")
    fun `test createLineSplitter`(commaChar: Char, quoteChar: Char, line: String, expectedSplits: List<String>) {
        val lineSplitter = createLineSplitter(commaChar, quoteChar)
        val actualSplits = lineSplitter(line)

        assertEquals(expectedSplits, actualSplits, "line being split: $line")
    }

    companion object {
        @JvmStatic
        private fun createLineSplitterDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    ',', '"',
                    """  House of the Rising Sun  , " test , shouldn't split " """ ,
                    listOf("House of the Rising Sun", "test , shouldn't split")
                ),
                Arguments.of(
                    ';', '\'',
                    """  House of the Rising Sun  ; ' test ; shouldn't split ' """ ,
                    listOf("House of the Rising Sun", "test ; shouldn't split")
                )
            )
        }
    }
}