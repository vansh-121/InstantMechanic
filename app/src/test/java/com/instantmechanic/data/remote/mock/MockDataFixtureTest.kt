package com.instantmechanic.data.remote.mock

import com.instantmechanic.data.mapper.MechanicMapper
import com.instantmechanic.data.mapper.WorkingHoursParser
import com.instantmechanic.data.remote.dto.MechanicDto
import com.instantmechanic.domain.model.OpeningHours
import com.instantmechanic.domain.model.ServiceType
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Guards the bundled mock payload — the one file in this project that no compiler checks.
 *
 * `mechanics.json` is hand-written data, not code, so a stray comma or a misspelled
 * `breake_repair` would sail past every other test and only show up as an empty screen or a
 * silently mislabelled chip on a reviewer's emulator. These assertions are the type system the
 * asset doesn't have.
 */
class MockDataFixtureTest {

    private val json = Json {
        // Mirrors NetworkModule.provideJson so the test parses the asset exactly as the app does.
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    private val mechanics: List<MechanicDto> by lazy {
        json.decodeFromString(ListSerializer(MechanicDto.serializer()), assetFile().readText())
    }

    @Test
    fun `the shipped asset parses`() {
        assertTrue("the mock backend has nothing to serve", mechanics.isNotEmpty())
    }

    @Test
    fun `ids are present and unique`() {
        // LazyColumn keys on the id; duplicates would corrupt scroll state and item animations.
        assertTrue(mechanics.none { it.id.isBlank() })
        assertEquals(mechanics.size, mechanics.map { it.id }.distinct().size)
    }

    @Test
    fun `every field the UI renders is populated`() {
        mechanics.forEach { dto ->
            assertTrue("${dto.id}: blank name", dto.name.isNotBlank())
            // "6.9 km · , Pune" — a blank locality shows up as a stray comma on the card.
            assertTrue("${dto.id}: blank locality", dto.locality.isNotBlank())
            assertTrue("${dto.id}: blank city", dto.city.isNotBlank())
            assertTrue("${dto.id}: blank address", dto.addressLine.isNotBlank())
            assertTrue("${dto.id}: blank about", dto.about.isNotBlank())
            assertTrue("${dto.id}: blank price range", dto.priceRange.isNotBlank())
            assertTrue("${dto.id}: rating out of range: ${dto.rating}", dto.rating in 1.0..5.0)
            assertTrue("${dto.id}: no reviews", dto.reviewCount > 0)
            assertTrue("${dto.id}: no distance", dto.distanceKm > 0.0)
        }
    }

    @Test
    fun `ratings survive one-decimal rendering`() {
        // The card formats with "%.1f"; a rating of 4.85 would display as 4.9 and read as a typo.
        mechanics.forEach { dto ->
            assertEquals(
                "${dto.id}: rating ${dto.rating} has more than one decimal",
                dto.rating,
                (dto.rating * 10).roundToInt() / 10.0,
                0.0,
            )
        }
    }

    @Test
    fun `phone numbers are dialable`() {
        // The Detail screen hands these straight to ACTION_DIAL.
        val dialable = Regex("^\\+91[6-9]\\d{9}$")
        mechanics.forEach { dto ->
            assertTrue("${dto.id}: '${dto.phone}' is not a dialable Indian mobile", dialable.matches(dto.phone))
        }
    }

    @Test
    fun `every service maps to a known type`() {
        // ServiceType.fromApiValue is deliberately lenient — an unknown value degrades to OTHER
        // rather than crashing. That leniency is for a *server* we don't control; in our own
        // fixture an OTHER means a typo, and the filter menu hides OTHER, so the garage would be
        // unreachable through the service filter.
        mechanics.forEach { dto ->
            assertTrue("${dto.id}: has no services", dto.services.isNotEmpty())
            dto.services.forEach { raw ->
                assertEquals(
                    "${dto.id}: unknown service '$raw'",
                    raw,
                    ServiceType.fromApiValue(raw).apiValue,
                )
            }
            assertEquals("${dto.id}: duplicate services", dto.services.size, dto.services.distinct().size)
        }
    }

    @Test
    fun `every garage has a full week of hours`() {
        // The Detail screen draws a seven-row table and highlights today; a missing Thursday would
        // leave a gap, and a duplicate would render the day twice.
        mechanics.forEach { dto ->
            val parsed = WorkingHoursParser.parse(dto.workingHours)
            assertEquals(
                "${dto.id}: ${dto.workingHours.size} raw entries parsed to ${parsed.size} — an unparseable day was dropped",
                dto.workingHours.size,
                parsed.size,
            )
            assertEquals(
                "${dto.id}: does not cover all seven days exactly once",
                DayOfWeek.entries.toSet(),
                parsed.map { it.day }.toSet(),
            )
            assertEquals("${dto.id}: a day is listed twice", 7, parsed.size)
            parsed.filterNot { it.isClosed }.forEach { day ->
                assertTrue("${dto.id}: ${day.day} is open but has no times", day.opensAt != null && day.closesAt != null)
            }
        }
    }

    @Test
    fun `the fixture exercises both sides of the open-closed pill`() {
        // A demo where every garage is open (or every one closed) would hide the whole
        // opening-hours feature, and the "Open now" filter would look broken either way.
        val openThen = mechanics.count { isOpenAt(it, WEDNESDAY_MORNING) }

        assertTrue("no garage is open on a weekday mid-morning", openThen > 0)
        assertTrue("every single garage is open at once", openThen < mechanics.size)
    }

    @Test
    fun `at least one garage is open in the middle of the night`() {
        // Roadside rescue and towing are the reason the overnight (21:00 -> 05:00) case exists in
        // OpeningHours at all; if the fixture lost it, that code path would go undemonstrated.
        val threeAm = LocalDateTime.of(2026, 9, 2, 3, 0)
        assertTrue("nothing is open at 03:00", mechanics.any { isOpenAt(it, threeAm) })
    }

    @Test
    fun `the payload carries no image urls`() {
        // Deliberate: photos are the one thing the app cannot serve from assets, so the mock
        // payload omits them and MechanicImage draws its initials tile instead. That keeps
        // "clone and run" true with no network at all. See MechanicImage's KDoc.
        mechanics.forEach { dto ->
            assertTrue("${dto.id}: image_url would make the demo need the network", dto.imageUrl.isBlank())
        }
    }

    @Test
    fun `the mapper converts every record`() {
        // A fixed clock, so "summary and detail agree" can't flake by straddling an opening time.
        val mapper = MechanicMapper(Clock.fixed(WEDNESDAY_MORNING.atZone(ZONE).toInstant(), ZONE))
        mechanics.forEach { dto ->
            val summary = mapper.toSummary(dto)
            val detail = mapper.toDetail(dto)
            assertEquals(dto.id, summary.id)
            assertEquals(dto.id, detail.id)
            assertFalse("${dto.id}: mapped to no services", detail.services.isEmpty())
            assertEquals("${dto.id}: summary and detail disagree on open/closed", summary.isOpenNow, detail.isOpenNow)
        }
    }

    private fun isOpenAt(dto: MechanicDto, at: LocalDateTime): Boolean =
        OpeningHours.isOpenAt(WorkingHoursParser.parse(dto.workingHours), at)

    /**
     * Gradle runs unit tests with the module directory as the working directory; the fallback keeps
     * the test working when it is launched from the repository root instead (as some IDE run
     * configurations do).
     */
    private fun assetFile(): File {
        val candidates = listOf(
            File("src/main/assets/$ASSET_PATH"),
            File("app/src/main/assets/$ASSET_PATH"),
        )
        return candidates.firstOrNull { it.isFile }
            ?: error("Could not find $ASSET_PATH; looked in ${candidates.map { it.absolutePath }}")
    }

    private companion object {
        const val ASSET_PATH = "mock/mechanics.json"

        /** The fixture's opening times are wall-clock IST, so assertions about them need the zone. */
        val ZONE: ZoneId = ZoneId.of("Asia/Kolkata")

        /** An ordinary trading hour: a Wednesday, half past eleven. */
        val WEDNESDAY_MORNING: LocalDateTime = LocalDateTime.of(2026, 9, 2, 11, 30)
    }
}
