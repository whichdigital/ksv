# KSV - parsing comma separated values in/for Kotlin

You basically only have to annotate a data class with `@CsvRow` and it’s fields with either
`@CscValue` for basic types (`String`, `Int`, `Double`, `Boolean`),
`@CsvTimestamp` for `LocalDate`/`LocalDateTime`
or `@CsvGeneric` for generic mappings for user-defined conversions.

```kotlin
val dataRows: List<DataRow> = csv2List(
  CsvSourceConfig(
    stream = resourceLoader.getResource("testData/directory.csv").inputStream 
  )    
)

@CsvRow data class DataRow(
    @CsvValue(name = "RQIA") val id: String,
    @CsvValue(name = "Number of beds") val bedCount: Int?, // types can be nullable
    val addressLine1: String,                              // without annotation it's assumed the the column name is the the field name
    val city: String = "London",                           // without value in the csv file the Kotlin default value is used
    @CsvTimestamp(name = "latest check", format = "yyyy/MM/dd|dd/MM/yyyy")  
    val lastInspectionDate: LocalDate?,                    // multiple formats can be provided seperated by '|'
    @CsvGeneric(name = "Refreshments", converterName = "facilityBoolean")
    val refreshments: Boolean?                             // a user-defined converter can be used
)

// this is how a generic converter is configured.
// here the return type is Boolean
// but any type - even user-defined ones - can be used!
registerGenericConverter("facilityBoolean") {
    it.toLowerCase().startsWith("yes")
}
```

Boolean conversion

all string values whose lowercase version is one of the values
`"true"`, `"yes"`, `"y"`, `"1"` is mapped to `true`, to `false` otherwise.
