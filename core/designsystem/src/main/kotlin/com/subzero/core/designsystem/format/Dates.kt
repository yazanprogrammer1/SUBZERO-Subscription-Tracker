package com.subzero.core.designsystem.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.subzero.core.designsystem.R
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val RELATIVE_WINDOW_DAYS = 7L

/** The locale from the current configuration, for date formatting. */
@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/**
 * "Today", "Tomorrow", "In 3 days" inside a one-week window, else a medium date such as
 * "Sep 16, 2026" (year omitted when it is the current year).
 */
@Composable
fun relativeDate(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days == 0L -> stringResource(R.string.core_designsystem_date_today)
        days == 1L -> stringResource(R.string.core_designsystem_date_tomorrow)
        days == -1L -> stringResource(R.string.core_designsystem_date_yesterday)
        days in 2..RELATIVE_WINDOW_DAYS -> pluralStringResource(R.plurals.core_designsystem_date_in_days, days.toInt(), days.toInt())
        days in -RELATIVE_WINDOW_DAYS..-2 -> pluralStringResource(R.plurals.core_designsystem_date_days_ago, (-days).toInt(), (-days).toInt())
        else -> shortDate(date, today)
    }
}

/** "Sep 16" in the current year, "Sep 16, 2027" otherwise. */
@Composable
fun shortDate(date: LocalDate, today: LocalDate): String {
    val locale = currentLocale()
    val formatter = remember(locale, date.year == today.year) {
        if (date.year == today.year) {
            DateTimeFormatter.ofPattern(bestPattern(locale, "MMMd"), locale)
        } else {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        }
    }
    return formatter.format(date)
}

/** "September 16, 2026". */
@Composable
fun longDate(date: LocalDate): String {
    val locale = currentLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale) }
    return formatter.format(date)
}

/** "September 2026". */
@Composable
fun monthYear(month: YearMonth): String {
    val locale = currentLocale()
    val formatter = remember(locale) { DateTimeFormatter.ofPattern(bestPattern(locale, "MMMMy"), locale) }
    return formatter.format(month)
}

/** "Sep", for chart axes. */
@Composable
fun monthAbbreviation(month: YearMonth): String {
    val locale = currentLocale()
    return month.month.getDisplayName(TextStyle.SHORT, locale)
}

private fun bestPattern(locale: Locale, skeleton: String): String =
    android.text.format.DateFormat.getBestDateTimePattern(locale, skeleton)
