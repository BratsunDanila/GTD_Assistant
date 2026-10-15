package com.sbros.app.domain.usecase

import com.sbros.app.domain.model.TaskItem
import com.sbros.app.domain.repository.SbrosRepository
import javax.inject.Inject

class SimplifyTaskUseCase @Inject constructor(
    private val repository: SbrosRepository
) {
    suspend operator fun invoke(task: TaskItem): Int {
        val totalLevels = 1 + task.simplifiedSteps.size
        val nextIndex = (task.currentSimplifyIndex + 1) % totalLevels
        repository.simplifyTask(task.id, nextIndex)
        return nextIndex
    }
}
