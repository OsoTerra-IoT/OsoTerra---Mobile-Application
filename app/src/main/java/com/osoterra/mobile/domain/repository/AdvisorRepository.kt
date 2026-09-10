package com.osoterra.mobile.domain.repository

import com.osoterra.mobile.core.util.DataResult
import com.osoterra.mobile.domain.model.Reading
import com.osoterra.mobile.domain.model.SupervisedPlot

interface AdvisorRepository {
    suspend fun getSupervisedPlots(): DataResult<List<SupervisedPlot>>
    suspend fun getSeries(plotId: String): DataResult<List<Reading>>
}
