package com.instantmechanic.data.repository

import com.instantmechanic.core.result.AppResult
import com.instantmechanic.core.result.DataError
import com.instantmechanic.data.mapper.MechanicMapper
import com.instantmechanic.data.remote.MechanicApiService
import com.instantmechanic.di.IoDispatcher
import com.instantmechanic.domain.model.MechanicDetail
import com.instantmechanic.domain.model.MechanicPage
import com.instantmechanic.domain.model.MechanicQuery
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceRequestReceipt
import com.instantmechanic.domain.repository.MechanicRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The only place in the app that knows Retrofit exists.
 *
 * Its second job is just as important as fetching: it is the boundary where thrown exceptions
 * stop. Everything above it receives an [AppResult] with a typed [DataError], so a ViewModel can
 * decide what to show without catching `HttpException` or knowing what a 503 is.
 */
@Singleton
class MechanicRepositoryImpl @Inject constructor(
    private val api: MechanicApiService,
    private val mapper: MechanicMapper,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MechanicRepository {

    override suspend fun getMechanics(query: MechanicQuery): AppResult<MechanicPage> =
        safeCall {
            val response = api.getMechanics(
                q = query.search.trim().takeIf { it.isNotEmpty() },
                service = query.service?.apiValue,
                openNow = query.openNowOnly.takeIf { it },
                sort = query.sort.apiValue,
                page = query.page,
                pageSize = query.pageSize,
            )
            MechanicPage(
                items = response.items.map(mapper::toSummary),
                page = response.page,
                totalPages = response.totalPages,
                totalItems = response.totalItems,
            )
        }

    private val detailCache = java.util.concurrent.ConcurrentHashMap<String, MechanicDetail>()

    override suspend fun getMechanic(id: String): AppResult<MechanicDetail> {
        val result = safeCall { mapper.toDetail(api.getMechanic(id)) }
        return when (result) {
            is AppResult.Success -> {
                detailCache[id] = result.data
                result
            }
            is AppResult.Failure -> {
                val cached = detailCache[id]
                if (cached != null) AppResult.Success(cached) else result
            }
        }
    }

    override suspend fun submitServiceRequest(request: ServiceRequest): AppResult<ServiceRequestReceipt> =
        safeCall { mapper.toDomain(api.createServiceRequest(mapper.toDto(request))) }

    /**
     * Runs [block] off the main thread and converts anything it throws into a [DataError].
     *
     * `CancellationException` is a `IllegalStateException`-free control-flow signal in coroutines
     * and must be allowed to propagate, otherwise a cancelled screen would render an error.
     */
    private suspend fun <T> safeCall(block: suspend () -> T): AppResult<T> =
        withContext(ioDispatcher) {
            try {
                AppResult.Success(block())
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: HttpException) {
                AppResult.Failure(
                    when (e.code()) {
                        404 -> DataError.NOT_FOUND
                        in 500..599 -> DataError.SERVER
                        else -> DataError.UNKNOWN
                    },
                )
            } catch (e: SocketTimeoutException) {
                AppResult.Failure(DataError.TIMEOUT)
            } catch (e: IOException) {
                AppResult.Failure(DataError.NETWORK)
            } catch (e: SerializationException) {
                AppResult.Failure(DataError.PARSE)
            } catch (e: Exception) {
                AppResult.Failure(DataError.UNKNOWN)
            }
        }
}
