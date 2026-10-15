package com.sbros.app.domain.usecase

import com.sbros.app.domain.repository.SbrosRepository
import javax.inject.Inject

class SkipTaskUseCase @Inject constructor(
    private val repository: SbrosRepository
) {
    suspend operator fun invoke(taskId: String) {
        repository.skipTask(taskId)
    }
}
