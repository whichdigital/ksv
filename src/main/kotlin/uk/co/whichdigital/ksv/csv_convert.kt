package uk.co.whichdigital.ksv

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf


fun convert(token: String?, csvRowParam: CsvRowParam): Any? {
    if (token.isNullOrBlank()) {
        return null
    }

    val trimmed = token.trim()

    return when (csvRowParam) {
        is CsvRowParam.ByNoAnnotation, is CsvRowParam.ByCsvValue ->
            when (val expectedValueClass = csvRowParam.paramType.classifier) {
                String::class -> trimmed
                Int::class -> trimmed.toInt()
                Double::class -> trimmed.toDouble()
                Boolean::class -> trimmed.isTruthy()
                else -> throw IllegalStateException("unsupported parameter type: $expectedValueClass")
            }
        is CsvRowParam.ByCsvTimestamp ->
            when (val expectedValueClass = csvRowParam.paramType.classifier) {
                LocalDate::class -> trimmed.toLocalDate(csvRowParam)
                LocalDateTime::class -> trimmed.toLocalDateTime(csvRowParam)
                else -> throw IllegalStateException("unsupported parameter type: $expectedValueClass for @CsvTimestamp")
            }
        is CsvRowParam.ByCsvGeneric -> GenericConverterRegistry[csvRowParam.converterName].convert(trimmed)
    }
}

private val truthyValues: Set<String> = setOf("true", "yes", "y", "1")
private fun String.isTruthy(): Boolean = truthyValues.contains(this.toLowerCase())

private val dateTimeFormatterByFormat: MutableMap<String, DateTimeFormatter> = mutableMapOf()

private fun getTimeFormatter(format: String): DateTimeFormatter {
    return dateTimeFormatterByFormat[format] ?: DateTimeFormatter.ofPattern(format).also {
        dateTimeFormatterByFormat[format] = it
    }
}

private fun String.toLocalDate(csvRowParam: CsvRowParam.ByCsvTimestamp): LocalDate? =
    parseTimestamp(this, csvRowParam.format) { token, dateTimeFormatter ->
        LocalDate.parse(token, dateTimeFormatter)
    }

private fun String.toLocalDateTime(csvRowParam: CsvRowParam.ByCsvTimestamp): LocalDateTime? =
    parseTimestamp(this, csvRowParam.format) { token, dateTimeFormatter ->
        LocalDateTime.parse(token, dateTimeFormatter)
    }

private fun <T> parseTimestamp(
    timestampToken: String,
    formats: String,
    convertToTimestamp: (String, DateTimeFormatter) -> T
): T? {
    if (timestampToken.length <= 1 || timestampToken.toCharArray().none(Char::isDigit)) return null
    var lastException: DateTimeParseException? = null
    for (format in formats.split('|')) {
        val dateTimeFormatter = getTimeFormatter(format)
        try {
            return convertToTimestamp(timestampToken, dateTimeFormatter)
        } catch (dtpe: DateTimeParseException) {
            lastException = dtpe
        }
    }
    if (lastException != null) {
        throw lastException
    } else {
        throw IllegalArgumentException("no date format provided")
    }
}

inline fun <reified T : Any> registerGenericConverter(
    name: String,
    noinline convert: (String) -> T
) {
    GenericConverterRegistry.register(
        GenericConverter(
            T::class,
            name,
            convert
        )
    )
}

class GenericConverter<T : Any>(
    private val generatedClass: KClass<T>,
    val name: String,
    val convert: (String) -> T
) {
    fun isAssignableTo(otherClass: KClass<*>): Boolean = otherClass.isSuperclassOf(generatedClass)
}

object GenericConverterRegistry {
    private val converterByName = mutableMapOf<String, GenericConverter<*>>()

    fun register(converter: GenericConverter<*>) {
        if (converterByName.containsKey(converter.name)) {
            throw IllegalArgumentException("There is already a converter with name ${converter.name} registered!")
        }
        converterByName[converter.name] = converter
    }

    operator fun get(name: String): GenericConverter<*> =
        converterByName[name] ?: throw IllegalArgumentException("no converter registered for name: $name")
}
