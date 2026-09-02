package com.instantmechanic.data.remote.mock

import com.instantmechanic.data.remote.MechanicApiService
import com.instantmechanic.data.remote.dto.ServiceRequestDto
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Exercises the mock backend through the *real* transport stack.
 *
 * This test builds an actual Retrofit instance over an actual OkHttp client with the interceptor
 * installed, then calls the actual `@GET`/`@POST` methods. So it covers the Retrofit annotations,
 * the query-string construction, the JSON serialization in both directions, the HTTP status codes
 * and the interceptor's own filter/sort/page logic — the entire data path, minus the socket.
 *
 * Latency is set to zero here; only the app's production wiring uses the demo delay.
 */
class MockApiInterceptorTest {

    private val zone = ZoneId.of("Asia/Kolkata")
    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = true
    }

    private var simulateFailure = false

    // ------------------------------------------------------------ list: search

    @Test
    fun `no query returns every garage`() = runBlocking {
        val response = api().getMechanics()

        assertEquals(3, response.totalItems)
        assertEquals(3, response.items.size)
    }

    @Test
    fun `search matches the garage name`() = runBlocking {
        val response = api().getMechanics(q = "sharma")

        assertEquals(1, response.totalItems)
        assertEquals("m-001", response.items.single().id)
    }

    @Test
    fun `search is case insensitive`() = runBlocking {
        assertEquals(1, api().getMechanics(q = "SHARMA").totalItems)
    }

    @Test
    fun `search matches the locality`() = runBlocking {
        val response = api().getMechanics(q = "wakad")

        assertEquals("m-002", response.items.single().id)
    }

    @Test
    fun `search matches a service's human label as well as its api value`() = runBlocking {
        // "Brake repair" is the label for the wire value "brake_repair"; a user typing either
        // should find the garage.
        assertEquals(1, api().getMechanics(q = "brake_repair").totalItems)
        assertEquals(1, api().getMechanics(q = "Brake repair").totalItems)
    }

    @Test
    fun `search with no matches returns an empty page rather than an error`() = runBlocking {
        val response = api().getMechanics(q = "zzzznothing")

        assertEquals(0, response.totalItems)
        assertTrue(response.items.isEmpty())
        // Still one page, so the UI's paging maths doesn't divide by zero.
        assertEquals(1, response.totalPages)
    }

    // ------------------------------------------------------------ list: filters

    @Test
    fun `service filter narrows to garages offering it`() = runBlocking {
        val response = api().getMechanics(service = "towing")

        assertEquals(listOf("m-003"), response.items.map { it.id })
    }

    @Test
    fun `openNow filter applies the garage's real schedule`() = runBlocking {
        // Tuesday 11:00: m-001 and m-002 are open, m-003 runs 21:00 -> 05:00 so it is shut.
        val response = api(at = DayOfWeek.TUESDAY to LocalTime.of(11, 0))
            .getMechanics(openNow = true)

        assertEquals(setOf("m-001", "m-002"), response.items.map { it.id }.toSet())
    }

    @Test
    fun `openNow filter includes an overnight garage in the small hours`() = runBlocking {
        // 02:00 Wednesday is inside Tuesday's 21:00 -> 05:00 shift.
        val response = api(at = DayOfWeek.WEDNESDAY to LocalTime.of(2, 0))
            .getMechanics(openNow = true)

        assertEquals(listOf("m-003"), response.items.map { it.id })
    }

    @Test
    fun `openNow filter excludes a garage on its closed day`() = runBlocking {
        // m-002 is closed on Sunday.
        val response = api(at = DayOfWeek.SUNDAY to LocalTime.of(11, 0))
            .getMechanics(openNow = true)

        assertFalse(response.items.any { it.id == "m-002" })
    }

    @Test
    fun `filters combine`() = runBlocking {
        val response = api(at = DayOfWeek.TUESDAY to LocalTime.of(11, 0))
            .getMechanics(q = "pune", service = "general_service", openNow = true)

        assertEquals(setOf("m-001", "m-002"), response.items.map { it.id }.toSet())
    }

    // ------------------------------------------------------------ list: sorting

    @Test
    fun `default sort is best rated first`() = runBlocking {
        val response = api().getMechanics()

        assertEquals(listOf("m-001", "m-002", "m-003"), response.items.map { it.id })
    }

    @Test
    fun `distance sort is nearest first`() = runBlocking {
        val response = api().getMechanics(sort = "distance")

        assertEquals(listOf("m-002", "m-001", "m-003"), response.items.map { it.id })
    }

    @Test
    fun `an unrecognised sort value falls back to rating instead of failing`() = runBlocking {
        val response = api().getMechanics(sort = "popularity")

        assertEquals(listOf("m-001", "m-002", "m-003"), response.items.map { it.id })
    }

    // ------------------------------------------------------------ list: paging

    @Test
    fun `paging reports the right totals and slices`() = runBlocking {
        val first = api().getMechanics(page = 1, pageSize = 2)

        assertEquals(3, first.totalItems)
        assertEquals(2, first.totalPages)
        assertEquals(listOf("m-001", "m-002"), first.items.map { it.id })

        val second = api().getMechanics(page = 2, pageSize = 2)

        assertEquals(listOf("m-003"), second.items.map { it.id })
    }

    @Test
    fun `a page past the end is empty but still reports the totals`() = runBlocking {
        val response = api().getMechanics(page = 9, pageSize = 2)

        assertTrue(response.items.isEmpty())
        assertEquals(3, response.totalItems)
    }

    // ------------------------------------------------------------ detail

    @Test
    fun `detail returns the full resource`() = runBlocking {
        val dto = api().getMechanic("m-001")

        assertEquals("Sharma Auto Works", dto.name)
        assertEquals("+919876543210", dto.phone)
        assertEquals(7, dto.workingHours.size)
    }

    @Test
    fun `detail for an unknown id is a 404`() = runBlocking {
        val error = runCatching { api().getMechanic("does-not-exist") }.exceptionOrNull()

        assertTrue(error is HttpException)
        assertEquals(404, (error as HttpException).code())
    }

    // ------------------------------------------------------------ create

    @Test
    fun `creating a request returns a receipt with an id and an eta`() = runBlocking {
        val receipt = api().createServiceRequest(requestFor("m-001"))

        assertEquals("CONFIRMED", receipt.status)
        assertTrue("id should look like IM-XXXXXX: ${receipt.requestId}", receipt.requestId.matches(Regex("^IM-[A-Z2-9]{6}$")))
        assertEquals("Sharma Auto Works", receipt.mechanicName)
        // 15 min dispatch + 4 min/km over 1.2 km = 19.8 -> 20.
        assertEquals(20, receipt.etaMinutes)
    }

    @Test
    fun `eta grows with distance`() = runBlocking {
        val near = api().createServiceRequest(requestFor("m-002")).etaMinutes
        val far = api().createServiceRequest(requestFor("m-003")).etaMinutes

        assertTrue("$far should exceed $near", far > near)
    }

    @Test
    fun `creating a request against an unknown garage is a 404`() = runBlocking {
        val error = runCatching {
            api().createServiceRequest(requestFor("m-999"))
        }.exceptionOrNull()

        assertEquals(404, (error as HttpException).code())
    }

    // ------------------------------------------------------------ simulated failure

    @Test
    fun `the failure switch turns every call into a 503`() = runBlocking {
        val api = api()
        simulateFailure = true

        val listError = runCatching { api.getMechanics() }.exceptionOrNull()
        val detailError = runCatching { api.getMechanic("m-001") }.exceptionOrNull()

        assertEquals(503, (listError as HttpException).code())
        assertEquals(503, (detailError as HttpException).code())
    }

    @Test
    fun `turning the failure switch back off restores normal responses`() = runBlocking {
        val api = api()
        simulateFailure = true
        runCatching { api.getMechanics() }

        simulateFailure = false

        assertEquals(3, api.getMechanics().totalItems)
    }

    // ------------------------------------------------------------ wiring

    /**
     * A real Retrofit + OkHttp stack whose only unusual feature is that the interceptor answers
     * instead of the network. `baseUrl` includes a `/v1/` prefix on purpose: route matching keys
     * off the trailing path segments, so a versioned base path must not break it.
     */
    private fun api(at: Pair<DayOfWeek, LocalTime> = DayOfWeek.MONDAY to LocalTime.of(11, 0)): MechanicApiService {
        val (day, time) = at
        val instant = LocalDateTime.of(2024, 1, day.value, time.hour, time.minute)
            .atZone(zone)
            .toInstant()

        val interceptor = MockApiInterceptor(
            json = json,
            clock = Clock.fixed(instant, zone),
            assetReader = { FIXTURE },
            latencyMillis = 0L..0L,
            shouldFail = { simulateFailure },
        )

        return Retrofit.Builder()
            .baseUrl("https://api.instantmechanic.test/v1/")
            .client(OkHttpClient.Builder().addInterceptor(interceptor).build())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(MechanicApiService::class.java)
    }

    private fun requestFor(mechanicId: String) = ServiceRequestDto(
        mechanicId = mechanicId,
        customerName = "Vansh Sharma",
        phone = "9876543210",
        vehicleNumber = "MH12AB1234",
        serviceType = "brake_repair",
        description = "Front brakes squeal badly when stopping.",
    )

    private companion object {
        /**
         * A deliberately tiny fixture rather than the app's real asset: three garages chosen so
         * that rating order and distance order disagree, and one of them runs overnight.
         *
         * - m-001 rating 4.8, 1.2 km, Mon-Sat 09:00-20:00, general + brakes
         * - m-002 rating 4.5, 0.8 km, Mon-Sat 10:00-19:00 (closed Sun), general
         * - m-003 rating 4.1, 6.5 km, every night 21:00-05:00, towing
         */
        val FIXTURE = """
        [
          {
            "id": "m-001",
            "name": "Sharma Auto Works",
            "image_url": "https://example.test/m-001.jpg",
            "rating": 4.8,
            "review_count": 1247,
            "distance_km": 1.2,
            "address_line": "12 Karve Road",
            "locality": "Kothrud",
            "city": "Pune",
            "pincode": "411038",
            "phone": "+919876543210",
            "services": ["general_service", "brake_repair"],
            "working_hours": [
              { "day": "MONDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "TUESDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "WEDNESDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "THURSDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "FRIDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "SATURDAY", "opens_at": "09:00", "closes_at": "20:00" },
              { "day": "SUNDAY", "opens_at": "09:00", "closes_at": "20:00" }
            ],
            "is_verified": true,
            "price_range": "₹₹",
            "about": "Family-run garage."
          },
          {
            "id": "m-002",
            "name": "Wakad Motor Clinic",
            "image_url": "https://example.test/m-002.jpg",
            "rating": 4.5,
            "review_count": 612,
            "distance_km": 0.8,
            "address_line": "44 Datta Mandir Road",
            "locality": "Wakad",
            "city": "Pune",
            "pincode": "411057",
            "phone": "+919812345678",
            "services": ["general_service"],
            "working_hours": [
              { "day": "MONDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "TUESDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "WEDNESDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "THURSDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "FRIDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "SATURDAY", "opens_at": "10:00", "closes_at": "19:00" },
              { "day": "SUNDAY", "closed": true }
            ],
            "is_verified": false,
            "price_range": "₹",
            "about": "Quick turnaround servicing."
          },
          {
            "id": "m-003",
            "name": "Nightline Towing",
            "image_url": "https://example.test/m-003.jpg",
            "rating": 4.1,
            "review_count": 208,
            "distance_km": 6.5,
            "address_line": "9 Mumbai-Bangalore Highway",
            "locality": "Baner",
            "city": "Pune",
            "pincode": "411045",
            "phone": "+919700000000",
            "services": ["towing"],
            "working_hours": [
              { "day": "MONDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "TUESDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "WEDNESDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "THURSDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "FRIDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "SATURDAY", "opens_at": "21:00", "closes_at": "05:00" },
              { "day": "SUNDAY", "opens_at": "21:00", "closes_at": "05:00" }
            ],
            "is_verified": true,
            "price_range": "₹₹₹",
            "about": "24-hour recovery and towing."
          }
        ]
        """.trimIndent()
    }
}
