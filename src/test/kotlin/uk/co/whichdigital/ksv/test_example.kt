package uk.co.whichdigital.ksv

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.time.LocalDate


// definition of expected content (with types) of a csv source
@CsvRow
data class DataRow(
    @CsvValue(name = "RQIA") val id: String,
    @CsvValue(name = "Number of beds") val bedCount: Int?, // types can be nullable
    val addressLine1: String,                              // without annotation it's assumed the the column name is the the field name
    val city: String = "London",                           // without value in the csv file the Kotlin default value is used
    @CsvTimestamp(name = "latest check", format = "yyyy/MM/dd|dd/MM/yyyy")
    val latestCheckDate: LocalDate?,                       // multiple formats can be provided seperated by '|'
    @CsvGeneric(name = "offers Cola, Sprite or Fanta", converterName = "beverageBoolean")
    val refreshments: Boolean?                             // a user-defined converter can be used
)

class TestExample {
    /**
     * this is how a generic converter is configured.
     * here the return type is Boolean
     * but any type - even user-defined ones - can be used!
     */
    @BeforeAll
    fun setup() {
        registerGenericConverter("beverageBoolean") {
            it.toLowerCase() == "indeed"
        }
    }

    @Test
    fun `test the example given in the README file`() {
        // The normal case is to read the InputStream from a file, but any InputStream will do.
        val csvStream: InputStream = """
            city, addressLine1, Number of beds, latest check, RQIA, "offers Cola, Sprite or Fanta"
            if a line doesn't fit the pattern, it will be discarded <- like this line, the next line is fine because city and Number of beds are nullable
                , "2 Marylebone Rd",          ,2020/03/11,   WERS234, nope
            Berlin, "Berkaer Str 41", 1       ,28/08/2012, "NONE123", indeed
            Paris,"Rue Gauge, Maison 1", 4    ,          , "FR92834",
            Atlantis,,25000,,,
            """.trimIndent().byteInputStream()

        // a csv source is mapped (via reflection) to a list of instances of its expected content type
        val dataRows: List<DataRow> = csv2List(
            CsvSourceConfig(
                stream = csvStream
            )
        )

        assertEquals(3, dataRows.size, "number of successfully mapped rows")
        assertEquals(setOf("Paris", "London", "Berlin"), dataRows.map { it.city }.toSet(), "city names (London is added as a default value for a missing one)")
        assertTrue(dataRows.first { it.city=="Berlin" }.refreshments!!, "Berlin offers refreshments")
        assertEquals(dataRows.first { it.city=="Paris" }.addressLine1, "Rue Gauge, Maison 1", "values can contain a comma")
        assertNull(dataRows.first { it.city=="Paris" }.latestCheckDate, "Paris has no latest check date")
        assertEquals(dataRows.first { it.city == "Berlin" }.latestCheckDate, LocalDate.of(2012, 8, 28), "Berlin has a latest check date")
    }
}