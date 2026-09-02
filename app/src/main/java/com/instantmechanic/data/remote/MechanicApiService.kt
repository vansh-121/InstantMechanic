package com.instantmechanic.data.remote

import com.instantmechanic.data.remote.dto.MechanicDto
import com.instantmechanic.data.remote.dto.MechanicListResponseDto
import com.instantmechanic.data.remote.dto.ServiceRequestDto
import com.instantmechanic.data.remote.dto.ServiceRequestReceiptDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * The REST contract.
 *
 * This is a genuine Retrofit interface over a genuine OkHttp stack — real URLs, real query
 * strings, real JSON parsing. In the default build the responses happen to be produced by
 * `MockApiInterceptor` instead of a server; nothing below this interface is aware of that, and
 * pointing the app at a live backend is a `gradle.properties` change rather than a code change.
 */
interface MechanicApiService {

    /**
     * @param q free-text search across name, locality and city
     * @param service filter by [com.instantmechanic.domain.model.ServiceType.apiValue]
     * @param openNow when true, only garages currently open
     * @param sort `rating` (desc) or `distance` (asc)
     */
    @GET("mechanics")
    suspend fun getMechanics(
        @Query("q") q: String? = null,
        @Query("service") service: String? = null,
        @Query("openNow") openNow: Boolean? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): MechanicListResponseDto

    @GET("mechanics/{id}")
    suspend fun getMechanic(@Path("id") id: String): MechanicDto

    @POST("service-requests")
    suspend fun createServiceRequest(@Body body: ServiceRequestDto): ServiceRequestReceiptDto
}
