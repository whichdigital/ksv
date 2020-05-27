# KSV - reflective mapping of comma separated values to user-defined ones for Kotlin

You only have to annotate a data class with `@CsvRow` and it’s properties with either 
`@CsvValue` (for Strings, Ints, Doubles and Booleans), `CsvTimestamp` (for LocalDate and LocalDateTime) or `@CsvGeneric` (for user-defined mappings).
Because this library is written in Kotlin, you can define the **nullability** of properties.
(A blank value in the csv results in a null value of the property,
 at least as the property doesn't have a default value.)

```kotlin
@CsvRow data class DataRow(
    @CsvValue(name = "RQIA") val id: String,
    @CsvValue(name = "Number of beds") val bedCount: Int?, // types can be nullable
    val addressLine1: String,                              // without annotation it's assumed the the column name is the the property name
    val city: String = "London",                           // without value in the csv file the Kotlin default value is used
    @CsvTimestamp(name = "latest check", format = "yyyy/MM/dd|dd/MM/yyyy")  
    val latestCheckDate: LocalDate?,                       // multiple formats can be provided seperated by '|'
    @CsvGeneric(name = "offers Cola, Sprite or Fanta", converterName = "beverageBoolean")
    val refreshments: Boolean?                             // a user-defined converter can be used
)

// register a user-defined converter
registerGenericConverter("beverageBoolean") {
    it.toLowerCase()=="indeed"
}

val csvStream: InputStream = """
  city, addressLine1, Number of beds, latest check, RQIA, "offers Cola, Sprite or Fanta"
  if a line doesn't fit the pattern, it will be discarded <- like this line, the next line is fine because city and Number of beds are nullable
      , "2 Marylebone Rd",          ,2020/03/11,   WERS234, nope
  Berlin, "Berkaer Str 41", 1       ,28/08/2012, "NONE123", indeed
  Paris,"Rue Gauge, Maison 1", 4    ,          , "FR92834",
  Atlantis,,25000,,,
  """.trimIndent().byteInputStream()

val dataRows: List<DataRow> = csv2List(
  CsvSourceConfig(
    stream = csvStream 
  )    
)
```
This code is actually executed in the testclass [TestExample](https://github.com/whichdigital/ksv/blob/master/src/test/kotlin/uk/co/whichdigital/ksv/test_example.kt).

## Annotations

### class annotation(s)

#### @CsvRow
Is a marker annotation on a data class marking it as 
Boolean conversion


### property annotation(s)

All values are trimmed and stripped of surrounding quotes (default quote is double quote).

#### @CscValue
for mapping values to **String**, **Int**, **Double** or **Boolean**.
 
Booleans are mapped from a String value by comparing the lowercase version to
"true", "yes", "y" and "1", which are mapped to true, otherwise false.

annotation parameter:
* name (optional): the name of the column this property is instanciated from. If no name is provided, the name of the annotated property is used.

#### @CsvTimestamp
for **LocalDate** and **LocalDateTime**.

annotation parameter:
* name (optional): the name of the column this property is instanciated from. If no name is provided, the name of the annotated property is used.
* format: format is either a single timestamp pattern (e.g. "yyyy/MM/dd" ) or multiple patterns separated by '|' (e.g. "yyyy/MM/dd|dd-MM-yyyy" )

#### @CsvGeneric
for user-defined mappings to any type. It just has to be assured that the user-defined converter
is registered before the annotation is used. This is done by invoking the global `registerConverter` function.
```kotlin
fun <T: Any> registerGenericConverter(
  converterName: String,
  converter: (String) -> T
)
```
where `T` is the type of the property.

annotation parameter:
* name (optional): the name of the column this property is instanciated from. If no name is provided, the name of the annotated property is used.
* converterName: has to match the name of a registered converter. The return type of the converter has to match the type of the annotated property.

## Code

#### csv2List

Is the global function that converts a csv source (an InputStream plus optional more configuration parameters)
 to a list of the user-defined row type. This is where the (reflective) magic happens.
```kotlin
val dataRows: List<DataRow> = csv2List(
  CsvSourceConfig(
    stream = csvStream 
  )    
)
```
Invoking `csv2List` will close the InputStream.

`csv2List` has actually a bunch of optional parameters - apart from the main one that takes in a `CsvSourceConfig` -
that provide statistics about how many rows where discarded/parsed. (As the naming suggest the main idea here is to allow for logging.)

* logInvalidLine: `(line: String, msg: String)->Unit`: the line and reason of why a certain row/line was dropped from the csv (, mostly because the number of commas didn't fit).
* logRejectedRecord: `(record: String)->Unit`: the String representation of a CsvRecord (slightly process row/line) that was rejected by the `keepCsvRecord`-Predicate.
* logConversionError: `(record: String, msg: String)->Unit`: a record and why its conversion to the expected (row)type failed (, e.g. because of unfulfilled nullability constraints).
* logSummary: `(invalidLineCount: Int, rejectedRecordCount: Int, conversionErrorCount: Int, itemsCreated: Int)->Unit`: after all lines have been considered, here a summary of the complete process can be logged. 

e.g.
```kotlin
val csvFilePath: String = "data/someFile.csv"
val dataRows: List<DataRow> = csv2List(
  CsvSourceConfig(
    stream = classLoader.getResourceAsStream(csvFilePath),
    logSummary = {invalidLineCount: Int, rejectedRecordCount: Int, conversionErrorCount: Int, itemsCreated: Int ->
      logger.info("""
        Finished importing file $csvFilePath
          items imported: $itemsCreated
          lines with invalid format: $invalidLineCount (probably wrong amount of commas)
          rejected csvRecord: $rejectedRecordCount (optional filter provided)
          csvRecord which couldn't be converted to item: $conversionErrorCount
        """.trimIndent())
    }
  )    
)
```

#### CsvSourceConfig
Assuming the InputStream uses UTF8 the instanciation of a `CsvSourceConfig` only needs said InputStream.
But there are more configuration options:
* stream: InputStream: the source of the csv
* charset: Charset: the default is UTF8
* commaChar: Char: the default is a normal comma (',') but csv files are known to sometimes use other characters (e.g. a semicolon) as a delimiter
* quoteChar: Char: the default is a double quote, but char (e.g. single quote) can be used
* fixLine: (String)->String: this function is used on every line of the csv file. The idea is to remove e.g. illegal characters. The default removes invisible BOM characters (`\uFEFF` and `\u200B`) from the start of the line.
* keepCsvRecord: (CsvRecord) -> Boolean: The csv input stream can be extremely large. Sometimes we want to filter out rows from the csv before an object is instanciated. (in our exemple this would be of type `DataRow`)
* normalizeColumnName: (String)->String: if we don't control the source of the csv data (e.g. because the files come from an external source),
 it often happens the column names change slightly between different versions. the `normalizeColumnName`-parameter is supposed to make
 a configuration more robust against such changes. The default version removes all spaces from the column names an maps them to their lower case version.
 If you have different requirements (or the default version leads to collisions of normalized column names) provide your own function.

Here an example of how to define a predicate for the optional `keepCsvRecord`-parameter: (it allows only lines where the number of beds is bigger than 2)
```kotlin
val dataRows: List<DataRow> = csv2List(
  CsvSourceConfig(
    stream = csvStream,
    keepCsvRecord = ::onlyRowsWithAtLeastTwoBeds 
  )    
)

fun onlyRowsWithAtLeastTwoBeds(header: CsvHeader, record: CsvRecord): Boolean {
  val (nrOfBedsIndex: Int) = header.getIndexesOf("Number of beds") // more than one index can be queried at the same time which is why the result has to be destructed
  val nrOfBeds: Int = record.getAsNonBlankStringOrNull(nrOfBedsIndex)?.toIntOrNull() ?: 0
  return nrOfBeds>=2
}
```
Of course this filter operation could also be implemented on  `dataRows` after the complete csv source has been parsed:
```kotlin
val dataRows: List<DataRow> = csv2List(CsvSourceConfig(csvStream)).filter {row ->
  row.bedCount?.let{nrOfBeds->nrOfBeds>=2} ?: false
}
```
But assuming a case where the csv source is truely big (like gigabytes big),
and a lot of instances get discarded because of this filter,
it can be a reasonable idea to filter those rows out before computation time and memory is wasted on them.
