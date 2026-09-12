package com.subzero.core.data.database.converter

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

/** LocalDate as ISO-8601 text (lexicographically sortable); Instant as epoch milliseconds. */
class TimeConverters {

    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
