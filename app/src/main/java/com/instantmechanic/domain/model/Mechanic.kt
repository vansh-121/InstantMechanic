package com.instantmechanic.domain.model

import java.util.Locale

/**
 * Summary of a garage, as shown in the Home list.
 *
 * [isOpenNow] is *computed* by the data-layer mapper from the weekly schedule against an
 * injected Clock rather than trusted from the server payload, so it stays correct no matter
 * how stale the response is — and stays deterministic in unit tests.
 */
data class Mechanic(
    val id: String,
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val locality: String,
    val city: String,
    val services: List<ServiceType>,
    val isOpenNow: Boolean,
    val isVerified: Boolean,
) {
    val ratingDisplay: String get() = "%.1f".format(Locale.US, rating)
    val distanceDisplay: String get() = "%.1f km".format(Locale.US, distanceKm)
    val areaDisplay: String get() = "$locality, $city"
}

/** Full garage profile shown on the Detail screen. */
data class MechanicDetail(
    val id: String,
    val name: String,
    val imageUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val distanceKm: Double,
    val addressLine: String,
    val locality: String,
    val city: String,
    val pincode: String,
    val phone: String,
    val services: List<ServiceType>,
    val weeklyHours: List<DayHours>,
    val isOpenNow: Boolean,
    val isVerified: Boolean,
    val priceRange: String,
    val about: String,
) {
    val ratingDisplay: String get() = "%.1f".format(Locale.US, rating)
    val distanceDisplay: String get() = "%.1f km".format(Locale.US, distanceKm)
    val fullAddress: String get() = "$addressLine, $locality, $city $pincode"
}
