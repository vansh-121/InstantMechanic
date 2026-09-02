package com.instantmechanic.domain.repository

import com.instantmechanic.core.result.AppResult
import com.instantmechanic.domain.model.MechanicDetail
import com.instantmechanic.domain.model.MechanicPage
import com.instantmechanic.domain.model.MechanicQuery
import com.instantmechanic.domain.model.ServiceRequest
import com.instantmechanic.domain.model.ServiceRequestReceipt

/**
 * The app's single gateway to mechanic data.
 *
 * The interface is declared in `domain` and implemented in `data`, so the dependency arrow points
 * inwards: ViewModels compile against this abstraction and know nothing about Retrofit. That is
 * what lets the unit tests substitute a fake in one line, with no HTTP stack in sight.
 *
 * Every method returns [AppResult] rather than throwing — failure is part of the contract.
 */
interface MechanicRepository {

    /** Search / filter / sort / page the mechanic list. All of it happens server-side. */
    suspend fun getMechanics(query: MechanicQuery): AppResult<MechanicPage>

    /** Full profile for one garage. */
    suspend fun getMechanic(id: String): AppResult<MechanicDetail>

    /** Book a service. Returns the backend's receipt on success. */
    suspend fun submitServiceRequest(request: ServiceRequest): AppResult<ServiceRequestReceipt>
}
