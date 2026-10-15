package com.sbros.app.domain.usecase

import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.repository.SbrosRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetFocusTaskUseCase @Inject constructor(
    private val repository: SbrosRepository
) {
    operator fun invoke(energyLevel: EnergyLevel): Flow<List<TaskItem>> {
        return repository.getFocusTasksByEnergy(energyLevel)
    }
}
