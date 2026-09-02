package com.instantmechanic.util

import com.instantmechanic.core.result.AppResult
import com.instantmechanic.core.result.DataError
import com.instantmechanic.domain.model.DayHours
import com.instantmechanic.domain.model.Mechanic
import com.instantmechanic.domain.model.MechanicDetail
import com.instantmechanic.domain.model.MechanicPage
import com.instantmechanic.domain.model.MechanicQuery
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceRequestReceipt
import com.instantmechanic.domain.model.ServiceType
import com.instantmechanic.domain.repository.MechanicRepository
import kotlinx.coroutines.delay
import java.time.DayOfWeek
import java.time.LocalTime

/**
 * Hand-written stand-in for [MechanicRepository].
 *
 * No mocking framework: the interface has three methods, so a fake is shorter than the setup a mock
 * would need, it reads like ordinary Kotlin in the test, and it can *record* what the ViewModel
 * asked for. That last part matters here — several tests assert that filters travel to the backend
 * as query parameters rather than being applied to a local list, and the only way to see that is to
 * look at the [queries] this fake received.
 *
 * The responders are `suspend` lambdas and each method honours a configurable delay, so a test can
 * hold a call in flight and inspect the intermediate loading state instead of only the outcome.
 */
class FakeMechanicRepository : MechanicRepository {

    /** Every query the ViewModel issued, in order. */
    val queries = mutableListOf<MechanicQuery>()

    /** Every request the ViewModel submitted, in order. */
    val submitted = mutableListOf<ServiceRequest>()

    /** Virtual milliseconds each call takes. Zero means it resolves within the same dispatch. */
    var listDelayMillis: Long = 0
    var detailDelayMillis: Long = 0
    var submitDelayMillis: Long = 0

    var listResponder: suspend (MechanicQuery) -> AppResult<MechanicPage> = { query ->
        AppResult.Success(pageOf(TestData.mechanics, query))
    }

    var detailResponder: suspend (String) -> AppResult<MechanicDetail> = { id ->
        AppResult.Success(TestData.detail(id = id))
    }

    var submitResponder: suspend (ServiceRequest) -> AppResult<ServiceRequestReceipt> = {
        AppResult.Success(TestData.receipt)
    }

    override suspend fun getMechanics(query: MechanicQuery): AppResult<MechanicPage> {
        queries += query
        if (listDelayMillis > 0) delay(listDelayMillis)
        return listResponder(query)
    }

    override suspend fun getMechanic(id: String): AppResult<MechanicDetail> {
        if (detailDelayMillis > 0) delay(detailDelayMillis)
        return detailResponder(id)
    }

    override suspend fun submitServiceRequest(request: ServiceRequest): AppResult<ServiceRequestReceipt> {
        submitted += request
        if (submitDelayMillis > 0) delay(submitDelayMillis)
        return submitResponder(request)
    }

    // ------------------------------------------------------------ conveniences

    fun failList(error: DataError = DataError.NETWORK) {
        listResponder = { AppResult.Failure(error) }
    }

    fun returnList(items: List<Mechanic>) {
        listResponder = { query -> AppResult.Success(pageOf(items, query)) }
    }

    /** Succeeds only from the [attempt]-th call onwards; earlier calls fail. Used to test Retry. */
    fun failListUntil(attempt: Int, error: DataError = DataError.SERVER) {
        var calls = 0
        listResponder = { query ->
            calls++
            if (calls < attempt) {
                AppResult.Failure(error)
            } else {
                AppResult.Success(pageOf(TestData.mechanics, query))
            }
        }
    }

    fun failDetail(error: DataError = DataError.NOT_FOUND) {
        detailResponder = { AppResult.Failure(error) }
    }

    fun failSubmit(error: DataError = DataError.SERVER) {
        submitResponder = { AppResult.Failure(error) }
    }

    private fun pageOf(items: List<Mechanic>, query: MechanicQuery) = MechanicPage(
        items = items,
        page = query.page,
        totalPages = 1,
        totalItems = items.size,
    )
}

/** Domain fixtures shared by the ViewModel tests. */
object TestData {

    fun mechanic(
        id: String = "m-001",
        name: String = "Sharma Auto Works",
        rating: Double = 4.8,
        distanceKm: Double = 1.2,
        services: List<ServiceType> = listOf(ServiceType.GENERAL_SERVICE, ServiceType.BRAKE_REPAIR),
        isOpenNow: Boolean = true,
    ) = Mechanic(
        id = id,
        name = name,
        imageUrl = "https://example.test/$id.jpg",
        rating = rating,
        reviewCount = 1247,
        distanceKm = distanceKm,
        locality = "Kothrud",
        city = "Pune",
        services = services,
        isOpenNow = isOpenNow,
        isVerified = true,
    )

    val mechanics = listOf(
        mechanic(),
        mechanic(id = "m-002", name = "Wakad Motor Clinic", rating = 4.5, distanceKm = 0.8),
    )

    fun detail(
        id: String = "m-001",
        name: String = "Sharma Auto Works",
        services: List<ServiceType> = listOf(ServiceType.GENERAL_SERVICE, ServiceType.BRAKE_REPAIR),
    ) = MechanicDetail(
        id = id,
        name = name,
        imageUrl = "https://example.test/$id.jpg",
        rating = 4.8,
        reviewCount = 1247,
        distanceKm = 1.2,
        addressLine = "12 Karve Road",
        locality = "Kothrud",
        city = "Pune",
        pincode = "411038",
        phone = "+919876543210",
        services = services,
        weeklyHours = DayOfWeek.entries.map { day ->
            val closed = day == DayOfWeek.SUNDAY
            DayHours(
                day = day,
                opensAt = if (closed) null else LocalTime.of(9, 0),
                closesAt = if (closed) null else LocalTime.of(20, 0),
                isClosed = closed,
            )
        },
        isOpenNow = true,
        isVerified = true,
        priceRange = "₹₹",
        about = "Family-run garage.",
    )

    val receipt = ServiceRequestReceipt(
        requestId = "IM-7KQ4TB",
        status = "CONFIRMED",
        etaMinutes = 20,
        mechanicName = "Sharma Auto Works",
    )
}
