package io.github.meko123456.barati.shared.domain

import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * The day reviews are counted in: the date on the phone's own calendar, as days since 1970-01-01.
 *
 * Both apps ask here so they can't disagree. iOS used to count whole days of UTC, so its day
 * turned at 04:00 in Tbilisi and at 20:00 the evening before in New York: cards came due hours
 * early or late, and a card graded in the evening was stamped with tomorrow.
 */
object StudyDay {

    fun today(): Long = dayAt(Clock.System.now(), TimeZone.currentSystemDefault())

    internal fun dayAt(instant: Instant, zone: TimeZone): Long =
        instant.toLocalDateTime(zone).date.toEpochDays()
}
