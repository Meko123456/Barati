package io.github.meko123456.barati.shared

import io.github.meko123456.barati.shared.domain.StudyDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone

class StudyDayTest {

    // 23:30 UTC is already the next morning in Tbilisi and still the evening in New York.
    private val lateUtc = Instant.parse("2026-10-23T23:30:00Z")

    @Test
    fun theDayIsTheDateOnThePhoneNotInUtc() {
        assertEquals(LocalDate(2026, 10, 24).toEpochDays(), StudyDay.dayAt(lateUtc, TimeZone.of("Asia/Tbilisi")))
        assertEquals(LocalDate(2026, 10, 23).toEpochDays(), StudyDay.dayAt(lateUtc, TimeZone.of("America/New_York")))
    }

    @Test
    fun theDayTurnsAtLocalMidnight() {
        val zone = TimeZone.of("America/New_York")
        // Midnight in New York is 04:00 UTC in October (EDT, UTC-4).
        assertEquals(
            LocalDate(2026, 10, 22).toEpochDays(),
            StudyDay.dayAt(Instant.parse("2026-10-23T03:59:59Z"), zone),
        )
        assertEquals(
            LocalDate(2026, 10, 23).toEpochDays(),
            StudyDay.dayAt(Instant.parse("2026-10-23T04:00:00Z"), zone),
        )
    }

    @Test
    fun aDayThatGainsAnHourIsStillOneDay() {
        // Berlin's clocks go back at 03:00 on 25 October 2026, so that day lasts 25 hours.
        val zone = TimeZone.of("Europe/Berlin")
        val sunday = LocalDate(2026, 10, 25).toEpochDays()
        assertEquals(sunday, StudyDay.dayAt(Instant.parse("2026-10-24T22:00:00Z"), zone)) // 00:00 CEST
        assertEquals(sunday, StudyDay.dayAt(Instant.parse("2026-10-25T22:59:59Z"), zone)) // 23:59:59 CET
        assertEquals(sunday + 1, StudyDay.dayAt(Instant.parse("2026-10-25T23:00:00Z"), zone))
    }
}
