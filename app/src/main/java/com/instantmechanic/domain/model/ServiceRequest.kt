package com.instantmechanic.domain.model

/** A validated service booking, ready to be sent to the backend. */
data class ServiceRequest(
    val mechanicId: String,
    val customerName: String,
    val phone: String,
    val vehicleNumber: String,
    val serviceType: ServiceType,
    val description: String,
)

/** The backend's acknowledgement of a [ServiceRequest]. */
data class ServiceRequestReceipt(
    val requestId: String,
    val status: String,
    val etaMinutes: Int,
    val mechanicName: String,
) {
    /** "45 min" / "1 hr 15 min" — used on the confirmation screen. */
    val etaDisplay: String
        get() = when {
            etaMinutes < 60 -> "$etaMinutes min"
            etaMinutes % 60 == 0 -> "${etaMinutes / 60} hr"
            else -> "${etaMinutes / 60} hr ${etaMinutes % 60} min"
        }
}
