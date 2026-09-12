package com.subzero.core.domain.testing

import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A [Clock] frozen at noon on [date] in [zone]. */
fun fixedClock(date: LocalDate, zone: ZoneId = ZoneOffset.UTC): Clock =
    Clock.fixed(date.atTime(LocalTime.NOON).atZone(zone).toInstant(), zone)

fun date(iso: String): LocalDate = LocalDate.parse(iso)
