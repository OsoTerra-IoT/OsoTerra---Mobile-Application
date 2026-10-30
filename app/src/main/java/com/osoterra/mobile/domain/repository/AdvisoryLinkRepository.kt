package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.AdvisoryRequest

/** Vínculos asesor–productor (IAM `advisory-links`) vistos desde el productor. */
interface AdvisoryLinkRepository {
    suspend fun getPendingRequests(): DataResult<List<AdvisoryRequest>>
    suspend fun accept(requestId: String): DataResult<Unit>
    suspend fun decline(requestId: String): DataResult<Unit>
}
