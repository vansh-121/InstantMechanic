package com.instantmechanic.data.mapper

import com.instantmechanic.data.remote.dto.MechanicDto
import com.instantmechanic.data.remote.dto.ServiceRequestReceiptDto
import com.instantmechanic.data.remote.dto.WorkingHoursDto
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * The mapper is where wire data becomes something the UI can trust, so the tests focus on the two
 * things it *decides* rather than merely copies: the derived `isOpenNow` flag, and the lenient
 * handling of service values the client doesn't recognise.
 *
 * The clock is injected, which is the whole reason these assertions can be exact instead of
 * "whatever is true when CI happens to run".
 */
class MechanicMapperTest {

    private val zone = ZoneId.of("Asia/Kolkata")

    // ------------------------------------------------------------ derived open/closed

    @Test
    fun `isOpenNow is true inside the garage's window`() {
        val mapper = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0))

        assertTrue(mapper.toSummary(dto()).isOpenNow)
    }

    @Test
    fun `isOpenNow is false outside the garage's window`() {
        val mapper = mapperAt(DayOfWeek.MONDAY, LocalTime.of(21, 0))

        assertFalse(mapper.toSummary(dto()).isOpenNow)
    }

    @Test
    fun `isOpenNow is false on a closed day`() {
        val mapper = mapperAt(DayOfWeek.SUNDAY, LocalTime.of(11, 0))

        assertFalse(mapper.toSummary(dto()).isOpenNow)
    }

    @Test
    fun `isOpenNow ignores any server-supplied opinion`() {
        // The DTO has no `is_open` field by design. This test documents that intent: the same
        // payload flips purely with the clock, so a stale cached response cannot mislead the user.
        val payload = dto()

        assertTrue(mapperAt(DayOfWeek.MONDAY, LocalTime.of(10, 0)).toSummary(payload).isOpenNow)
        assertFalse(mapperAt(DayOfWeek.MONDAY, LocalTime.of(23, 0)).toSummary(payload).isOpenNow)
    }

    @Test
    fun `summary and detail agree about open state`() {
        val mapper = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0))
        val payload = dto()

        assertEquals(mapper.toSummary(payload).isOpenNow, mapper.toDetail(payload).isOpenNow)
    }

    // ------------------------------------------------------------ services

    @Test
    fun `known service values map to their enum`() {
        val mapped = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0))
            .toSummary(dto(services = listOf("brake_repair", "towing")))

        assertEquals(listOf(ServiceType.BRAKE_REPAIR, ServiceType.TOWING), mapped.services)
    }

    @Test
    fun `an unknown service value degrades to OTHER instead of throwing`() {
        // Forward compatibility: a backend adding "ceramic_coating" must not crash a shipped app.
        val mapped = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0))
            .toSummary(dto(services = listOf("ceramic_coating")))

        assertEquals(listOf(ServiceType.OTHER), mapped.services)
    }

    @Test
    fun `duplicate services are collapsed`() {
        // Two unknown values would otherwise both become OTHER and render as a repeated chip.
        val mapped = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0))
            .toSummary(dto(services = listOf("towing", "towing", "hyperdrive", "warp_core")))

        assertEquals(listOf(ServiceType.TOWING, ServiceType.OTHER), mapped.services)
    }

    // ------------------------------------------------------------ straight field mapping

    @Test
    fun `detail carries the full address parts and phone`() {
        val detail = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0)).toDetail(dto())

        assertEquals("12 Karve Road", detail.addressLine)
        assertEquals("Kothrud", detail.locality)
        assertEquals("Pune", detail.city)
        assertEquals("411038", detail.pincode)
        assertEquals("+919876543210", detail.phone)
        assertEquals("12 Karve Road, Kothrud, Pune 411038", detail.fullAddress)
    }

    @Test
    fun `detail parses the whole week of hours`() {
        val detail = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0)).toDetail(dto())

        assertEquals(7, detail.weeklyHours.size)
        assertEquals(DayOfWeek.MONDAY, detail.weeklyHours.first().day)
        assertEquals("9:00 AM - 8:00 PM", detail.weeklyHours.first().displayRange)
        assertEquals("Closed", detail.weeklyHours.last().displayRange)
    }

    @Test
    fun `rating renders with one decimal regardless of locale`() {
        val summary = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0)).toSummary(dto())

        assertEquals("4.8", summary.ratingDisplay)
        assertEquals("1.2 km", summary.distanceDisplay)
    }

    // ------------------------------------------------------------ outbound direction

    @Test
    fun `outbound request is trimmed and the plate uppercased`() {
        val dto = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0)).toDto(
            ServiceRequest(
                mechanicId = "m-001",
                customerName = "  Vansh Sharma  ",
                phone = " 9876543210 ",
                vehicleNumber = " mh12ab1234 ",
                serviceType = ServiceType.BRAKE_REPAIR,
                description = "  Brakes squeal.  ",
            ),
        )

        assertEquals("Vansh Sharma", dto.customerName)
        assertEquals("9876543210", dto.phone)
        assertEquals("MH12AB1234", dto.vehicleNumber)
        assertEquals("brake_repair", dto.serviceType)
        assertEquals("Brakes squeal.", dto.description)
    }

    @Test
    fun `receipt maps to the domain model`() {
        val receipt = mapperAt(DayOfWeek.MONDAY, LocalTime.of(11, 0)).toDomain(
            ServiceRequestReceiptDto(
                requestId = "IM-7KQ4TB",
                status = "CONFIRMED",
                etaMinutes = 75,
                createdAt = "2024-01-01T05:30:00Z",
                mechanicName = "Sharma Auto Works",
            ),
        )

        assertEquals("IM-7KQ4TB", receipt.requestId)
        assertEquals(75, receipt.etaMinutes)
        assertEquals("1 hr 15 min", receipt.etaDisplay)
    }

    // ------------------------------------------------------------ helpers

    private fun mapperAt(day: DayOfWeek, time: LocalTime): MechanicMapper {
        // 1 Jan 2024 was a Monday, so day.value maps straight onto the date.
        val instant: Instant = LocalDateTime.of(2024, 1, day.value, time.hour, time.minute)
            .atZone(zone)
            .toInstant()
        return MechanicMapper(Clock.fixed(instant, zone))
    }

    /** Mon-Sat 09:00-20:00, closed Sunday. */
    private fun dto(services: List<String> = listOf("general_service")) = MechanicDto(
        id = "m-001",
        name = "Sharma Auto Works",
        imageUrl = "https://example.test/m-001.jpg",
        rating = 4.8,
        reviewCount = 1247,
        distanceKm = 1.2,
        addressLine = "12 Karve Road",
        locality = "Kothrud",
        city = "Pune",
        pincode = "411038",
        phone = "+919876543210",
        services = services,
        workingHours = DayOfWeek.entries.map { day ->
            if (day == DayOfWeek.SUNDAY) {
                WorkingHoursDto(day = day.name, closed = true)
            } else {
                WorkingHoursDto(day = day.name, opensAt = "09:00", closesAt = "20:00")
            }
        },
        isVerified = true,
        priceRange = "₹₹",
        about = "Family-run garage.",
    )
}
