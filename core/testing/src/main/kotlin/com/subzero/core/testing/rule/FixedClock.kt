package com.subzero.core.testing.rule

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** A [Clock] frozen at a given date, for deterministic billing-date tests. */
fun fixedClock(
    date: LocalDate,
    zone: ZoneId = ZoneOffset.UTC,
    time: LocalTime = LocalTime.NOON,
): Clock = Clock.fixed(date.atTime(time).atZone(zone).toInstant(), zone)

fun fixedClock(instant: Instant, zone: ZoneId = ZoneOffset.UTC): Clock = Clock.fixed(instant, zone)
