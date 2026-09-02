package com.instantmechanic.domain.model

/**
 * A service a garage can perform.
 *
 * Modelled as an enum with a stable [apiValue] so the wire format stays decoupled from the
 * display label, and an unknown value coming from the backend degrades to [OTHER] instead of
 * throwing during parsing.
 */
enum class ServiceType(val apiValue: String, val label: String) {
    GENERAL_SERVICE("general_service", "General Service"),
    ENGINE_REPAIR("engine_repair", "Engine Repair"),
    OIL_CHANGE("oil_change", "Oil Change"),
    BRAKE_REPAIR("brake_repair", "Brake Repair"),
    TYRE_REPLACEMENT("tyre_replacement", "Tyre Replacement"),
    BATTERY_JUMPSTART("battery_jumpstart", "Battery / Jumpstart"),
    AC_SERVICE("ac_service", "AC Service"),
    DENTING_PAINTING("denting_painting", "Denting & Painting"),
    CAR_WASH("car_wash", "Car Wash"),
    TOWING("towing", "Towing"),
    OTHER("other", "Other");

    companion object {
        private val byApiValue = entries.associateBy(ServiceType::apiValue)

        /** Lenient lookup — unrecognised server values become [OTHER] rather than crashing. */
        fun fromApiValue(value: String): ServiceType =
            byApiValue[value.lowercase().trim()] ?: OTHER
    }
}
