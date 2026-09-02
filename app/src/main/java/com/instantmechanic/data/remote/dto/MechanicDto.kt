package com.instantmechanic.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire representation of a garage.
 *
 * DTOs are kept separate from the domain models on purpose: the JSON shape is the backend's to
 * change, and keeping it at the edge means a rename on the server costs one line in the mapper
 * instead of rippling through the UI.
 */
@Serializable
data class MechanicDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("image_url") val imageUrl: String = "",
    @SerialName("rating") val rating: Double = 0.0,
    @SerialName("review_count") val reviewCount: Int = 0,
    @SerialName("distance_km") val distanceKm: Double = 0.0,
    @SerialName("address_line") val addressLine: String = "",
    @SerialName("locality") val locality: String = "",
    @SerialName("city") val city: String = "",
    @SerialName("pincode") val pincode: String = "",
    @SerialName("phone") val phone: String = "",
    @SerialName("services") val services: List<String> = emptyList(),
    @SerialName("working_hours") val workingHours: List<WorkingHoursDto> = emptyList(),
    @SerialName("is_verified") val isVerified: Boolean = false,
    @SerialName("price_range") val priceRange: String = "",
    @SerialName("about") val about: String = "",
)

/** One weekday's opening window. `opens_at`/`closes_at` are "HH:mm" and null when closed. */
@Serializable
data class WorkingHoursDto(
    @SerialName("day") val day: String,
    @SerialName("opens_at") val opensAt: String? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("closed") val closed: Boolean = false,
)

/** Paged envelope returned by `GET /mechanics`. */
@Serializable
data class MechanicListResponseDto(
    @SerialName("items") val items: List<MechanicDto> = emptyList(),
    @SerialName("page") val page: Int = 1,
    @SerialName("page_size") val pageSize: Int = 20,
    @SerialName("total_items") val totalItems: Int = 0,
    @SerialName("total_pages") val totalPages: Int = 1,
)

/** Request body for `POST /service-requests`. */
@Serializable
data class ServiceRequestDto(
    @SerialName("mechanic_id") val mechanicId: String,
    @SerialName("customer_name") val customerName: String,
    @SerialName("phone") val phone: String,
    @SerialName("vehicle_number") val vehicleNumber: String,
    @SerialName("service_type") val serviceType: String,
    @SerialName("description") val description: String,
)

/** Response body for `POST /service-requests`. */
@Serializable
data class ServiceRequestReceiptDto(
    @SerialName("request_id") val requestId: String,
    @SerialName("status") val status: String,
    @SerialName("eta_minutes") val etaMinutes: Int,
    @SerialName("created_at") val createdAt: String,
    @SerialName("mechanic_name") val mechanicName: String = "",
)

/** Error envelope the backend returns for 4xx/5xx. */
@Serializable
data class ApiErrorDto(
    @SerialName("code") val code: String = "",
    @SerialName("message") val message: String = "",
)
