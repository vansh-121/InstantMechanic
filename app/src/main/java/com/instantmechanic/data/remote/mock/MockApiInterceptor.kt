package com.instantmechanic.data.remote.mock

import com.instantmechanic.data.mapper.WorkingHoursParser
import com.instantmechanic.data.remote.dto.ApiErrorDto
import com.instantmechanic.data.remote.dto.MechanicDto
import com.instantmechanic.data.remote.dto.MechanicListResponseDto
import com.instantmechanic.data.remote.dto.ServiceRequestDto
import com.instantmechanic.data.remote.dto.ServiceRequestReceiptDto
import com.instantmechanic.domain.model.OpeningHours
import com.instantmechanic.domain.model.ServiceType
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Serves the REST API from a bundled JSON asset.
 *
 * ### Why an interceptor rather than a fake repository
 *
 * The brief asks for REST integration and JSON parsing, but no backend was supplied. Faking at
 * the *repository* level would have deleted exactly the layers under test — Retrofit, OkHttp,
 * kotlinx.serialization, error mapping. Faking at the *transport* level keeps all of them: real
 * `@GET`/`@POST` calls with real query strings go out, real JSON comes back and is really parsed,
 * and HTTP status codes drive the error handling. The only thing replaced is the socket.
 *
 * The consequence is that `USE_MOCK_API=false` swaps in a live server with no code change, and
 * that a reviewer can clone the repo and run it with nothing to configure.
 *
 * Filtering, searching, sorting and paging are applied *here*, from the query parameters, exactly
 * as a server would — the ViewModel never filters a local list.
 *
 * Every collaborator is constructor-injected so the whole thing runs in a plain JVM unit test.
 */
class MockApiInterceptor(
    private val json: kotlinx.serialization.json.Json,
    private val clock: Clock,
    private val assetReader: () -> String,
    private val latencyMillis: LongRange = DEFAULT_LATENCY,
    private val shouldFail: () -> Boolean = { false },
) : Interceptor {

    /** Parsed once and reused; the asset never changes at runtime. */
    private val mechanics: List<MechanicDto> by lazy {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(MechanicDto.serializer()), assetReader())
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        simulateLatency()

        if (shouldFail()) {
            return errorResponse(
                request,
                code = 503,
                message = "Service Unavailable",
                body = ApiErrorDto("service_unavailable", "The service is temporarily unavailable."),
            )
        }

        val segments = request.url.pathSegments.filter { it.isNotBlank() }
        val last = segments.lastOrNull().orEmpty()
        val secondLast = segments.getOrNull(segments.size - 2).orEmpty()

        return when {
            request.method == "GET" && last == PATH_MECHANICS -> listMechanics(request)
            request.method == "GET" && secondLast == PATH_MECHANICS -> mechanicDetail(request, last)
            request.method == "POST" && last == PATH_SERVICE_REQUESTS -> createServiceRequest(request)
            else -> errorResponse(
                request,
                code = 404,
                message = "Not Found",
                body = ApiErrorDto("not_found", "No route matches ${request.method} ${request.url.encodedPath}"),
            )
        }
    }

    // ---------------------------------------------------------------- routes

    private fun listMechanics(request: Request): Response {
        val url = request.url
        val search = url.queryParameter("q").orEmpty().trim()
        val service = url.queryParameter("service")?.takeIf { it.isNotBlank() }
        val openNow = url.queryParameter("openNow")?.toBooleanStrictOrNull() ?: false
        val sort = url.queryParameter("sort").orEmpty()
        val page = url.queryParameter("page")?.toIntOrNull()?.coerceAtLeast(1) ?: 1
        val pageSize = url.queryParameter("pageSize")?.toIntOrNull()?.coerceIn(1, 100) ?: 20

        val now = LocalDateTime.now(clock)

        val filtered = mechanics
            .filter { matchesSearch(it, search) }
            .filter { service == null || it.services.any { s -> s.equals(service, ignoreCase = true) } }
            .filter { !openNow || isOpenNow(it, now) }
            .sortedWith(comparatorFor(sort))

        val totalItems = filtered.size
        val totalPages = if (totalItems == 0) 1 else ceil(totalItems / pageSize.toDouble()).toInt()
        val items = filtered.drop((page - 1) * pageSize).take(pageSize)

        val payload = MechanicListResponseDto(
            items = items,
            page = page,
            pageSize = pageSize,
            totalItems = totalItems,
            totalPages = totalPages,
        )
        return jsonResponse(request, 200, "OK", json.encodeToString(MechanicListResponseDto.serializer(), payload))
    }

    private fun mechanicDetail(request: Request, id: String): Response {
        val match = mechanics.firstOrNull { it.id.equals(id, ignoreCase = true) }
            ?: return errorResponse(
                request,
                code = 404,
                message = "Not Found",
                body = ApiErrorDto("mechanic_not_found", "No mechanic with id '$id'"),
            )
        return jsonResponse(request, 200, "OK", json.encodeToString(MechanicDto.serializer(), match))
    }

    private fun createServiceRequest(request: Request): Response {
        val raw = request.readBodyAsString()
        val submitted = runCatching {
            json.decodeFromString(ServiceRequestDto.serializer(), raw)
        }.getOrElse {
            return errorResponse(
                request,
                code = 400,
                message = "Bad Request",
                body = ApiErrorDto("invalid_body", "Request body is not a valid service request"),
            )
        }

        val mechanic = mechanics.firstOrNull { it.id == submitted.mechanicId }
            ?: return errorResponse(
                request,
                code = 404,
                message = "Not Found",
                body = ApiErrorDto("mechanic_not_found", "No mechanic with id '${submitted.mechanicId}'"),
            )

        // A plausible ETA: travel time scaled by distance, plus a fixed dispatch overhead.
        val eta = (DISPATCH_OVERHEAD_MINUTES + mechanic.distanceKm * MINUTES_PER_KM).roundToInt()

        val receipt = ServiceRequestReceiptDto(
            requestId = generateRequestId(),
            status = "CONFIRMED",
            etaMinutes = eta,
            createdAt = DateTimeFormatter.ISO_INSTANT.format(clock.instant()),
            mechanicName = mechanic.name,
        )
        return jsonResponse(
            request,
            code = 201,
            message = "Created",
            body = json.encodeToString(ServiceRequestReceiptDto.serializer(), receipt),
        )
    }

    // ---------------------------------------------------------------- filtering

    private fun matchesSearch(dto: MechanicDto, search: String): Boolean {
        if (search.isBlank()) return true
        val needle = search.lowercase(Locale.US)
        if (dto.name.lowercase(Locale.US).contains(needle)) return true
        if (dto.locality.lowercase(Locale.US).contains(needle)) return true
        if (dto.city.lowercase(Locale.US).contains(needle)) return true
        // Match the human label too, so searching "brake" finds `brake_repair`.
        return dto.services.any { raw ->
            val type = ServiceType.fromApiValue(raw)
            raw.lowercase(Locale.US).contains(needle) ||
                type.label.lowercase(Locale.US).contains(needle)
        }
    }

    private fun isOpenNow(dto: MechanicDto, now: LocalDateTime): Boolean =
        OpeningHours.isOpenAt(WorkingHoursParser.parse(dto.workingHours), now)

    private fun comparatorFor(sort: String): Comparator<MechanicDto> = when (sort.lowercase(Locale.US)) {
        SORT_DISTANCE -> compareBy<MechanicDto> { it.distanceKm }.thenByDescending { it.rating }
        // Default: best rated first, breaking ties with the more-reviewed garage.
        else -> compareByDescending<MechanicDto> { it.rating }.thenByDescending { it.reviewCount }
    }

    // ---------------------------------------------------------------- plumbing

    private fun simulateLatency() {
        val floor = latencyMillis.first.coerceAtLeast(0)
        val ceiling = latencyMillis.last.coerceAtLeast(floor)
        if (ceiling == 0L) return
        val delay = if (ceiling == floor) floor else Random.nextLong(floor, ceiling + 1)
        if (delay > 0) runCatching { Thread.sleep(delay) }
    }

    private fun generateRequestId(): String {
        val suffix = (1..REQUEST_ID_LENGTH)
            .map { REQUEST_ID_ALPHABET.random() }
            .joinToString("")
        return "IM-$suffix"
    }

    private fun Request.readBodyAsString(): String {
        val body = this.body ?: return ""
        val buffer = Buffer()
        body.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun jsonResponse(request: Request, code: Int, message: String, body: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(message)
            .addHeader("content-type", CONTENT_TYPE)
            .body(body.toResponseBody(CONTENT_TYPE.toMediaType()))
            .build()

    private fun errorResponse(request: Request, code: Int, message: String, body: ApiErrorDto): Response =
        jsonResponse(request, code, message, json.encodeToString(ApiErrorDto.serializer(), body))

    companion object {
        private const val PATH_MECHANICS = "mechanics"
        private const val PATH_SERVICE_REQUESTS = "service-requests"
        private const val SORT_DISTANCE = "distance"
        private const val CONTENT_TYPE = "application/json"

        private const val DISPATCH_OVERHEAD_MINUTES = 15.0
        private const val MINUTES_PER_KM = 4.0

        private const val REQUEST_ID_LENGTH = 6
        private const val REQUEST_ID_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"

        /** Enough delay that loading states are actually visible when demoing the app. */
        val DEFAULT_LATENCY = 500L..1100L
    }
}
