package uk.co.whichdigital.ksv

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource


class TestHeader {
    private lateinit var defaultFixLine: StringModifier
    private lateinit var defaultColumnNormalizer: StringModifier
    private lateinit var defaultLineSplitter: LineSplitter

    @BeforeAll
    fun setup() {
        val config = CsvSourceConfig(
            stream = "unused".byteInputStream()
        )
        defaultColumnNormalizer = config.effectiveNormalizeColumnName
        defaultLineSplitter = config.splitByComma
        defaultFixLine = config.fixLine
    }

    @ParameterizedTest
    @MethodSource("initHeaderDataProvider")
    fun `test initialization of header`(line: String, expectedColumnNames: List<String>) {
        val header = CsvHeader(defaultFixLine(line), defaultColumnNormalizer, defaultLineSplitter)
        val actualColumnNames = header.normalizedColumnNames

        assertEquals(expectedColumnNames, actualColumnNames, "line being split: $line")
    }

    @Test
    fun `test header removes BOM character`() {
        // there is an invisible UTF-8 character ('\uFEFF') call BOM at the start of this line
        val line = """﻿"Org ID","Org  Name","Org Type" """
        val header = CsvHeader(defaultFixLine(line), defaultColumnNormalizer, defaultLineSplitter)

        assertEquals(
            listOf("orgid", "orgname", "orgtype"),
            header.normalizedColumnNames
        )
    }

    companion object {
        @JvmStatic
        private fun initHeaderDataProvider(): List<Arguments?> {
            return listOf(
                Arguments.of(
                    "",
                    listOf("")
                ),
                Arguments.of(
                    "12,hi",
                    listOf("12", "hi")
                ),
                Arguments.of(
                    "12,  , hi  ",
                    listOf("12", "", "hi")
                ),
                Arguments.of(
                    """12,,hi,"hi there"""",
                    listOf("12", "", "hi", "hithere")
                ),
                Arguments.of(
                    """"12","","hi","hi, there's a dog"""",
                    listOf("12", "", "hi", "hi,there'sadog")
                ),
                Arguments.of(
                    """"12","",,hi,"hi, there's a dog"""",
                    listOf("12", "", "", "hi", "hi,there'sadog")
                ),
                Arguments.of(
                    """"Org ID","Org Name"""",
                    listOf("orgid", "orgname")
                )
            )
        }
    }
}