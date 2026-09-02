package com.instantmechanic.data.mapper

import com.instantmechanic.data.remote.dto.MechanicDto
import com.instantmechanic.data.remote.dto.ServiceRequestDto
import com.instantmechanic.data.remote.dto.ServiceRequestReceiptDto
import com.instantmechanic.domain.model.Mechanic
import com.instantmechanic.domain.model.MechanicDetail
import com.instantmechanic.domain.model.OpeningHours
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceRequestReceipt
import com.instantmechanic.domain.model.ServiceType
import java.time.Clock
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Translates between wire DTOs and domain models.
 *
 * Note that `isOpenNow` is *derived here* from the weekly schedule against an injected [Clock],
 * not read from a server-provided boolean. A cached or slow response can't make the UI claim a
 * garage is open when it closed an hour ago, and unit tests can pin the clock to assert the
 * boundary cases exactly.
 */
class MechanicMapper @Inject constructor(
    private val clock: Clock,
) {

    fun toSummary(dto: MechanicDto): Mechanic = Mechanic(
        id = dto.id,
        name = dto.name,
        imageUrl = dto.imageUrl,
        rating = dto.rating,
        reviewCount = dto.reviewCount,
        distanceKm = dto.distanceKm,
        locality = dto.locality,
        city = dto.city,
        services = dto.services.map(ServiceType::fromApiValue).distinct(),
        isOpenNow = isOpenNow(dto),
        isVerified = dto.isVerified,
    )

    fun toDetail(dto: MechanicDto): MechanicDetail = MechanicDetail(
        id = dto.id,
        name = dto.name,
        imageUrl = dto.imageUrl,
        rating = dto.rating,
        reviewCount = dto.reviewCount,
        distanceKm = dto.distanceKm,
        addressLine = dto.addressLine,
        locality = dto.locality,
        city = dto.city,
        pincode = dto.pincode,
        phone = dto.phone,
        services = dto.services.map(ServiceType::fromApiValue).distinct(),
        weeklyHours = WorkingHoursParser.parse(dto.workingHours),
        isOpenNow = isOpenNow(dto),
        isVerified = dto.isVerified,
        priceRange = dto.priceRange,
        about = dto.about,
    )

    fun toDto(request: ServiceRequest): ServiceRequestDto = ServiceRequestDto(
        mechanicId = request.mechanicId,
        customerName = request.customerName.trim(),
        phone = request.phone.trim(),
        vehicleNumber = request.vehicleNumber.trim().uppercase(),
        serviceType = request.serviceType.apiValue,
        description = request.description.trim(),
    )

    fun toDomain(dto: ServiceRequestReceiptDto): ServiceRequestReceipt = ServiceRequestReceipt(
        requestId = dto.requestId,
        status = dto.status,
        etaMinutes = dto.etaMinutes,
        mechanicName = dto.mechanicName,
    )

    private fun isOpenNow(dto: MechanicDto): Boolean =
        OpeningHours.isOpenAt(WorkingHoursParser.parse(dto.workingHours), LocalDateTime.now(clock))
}
