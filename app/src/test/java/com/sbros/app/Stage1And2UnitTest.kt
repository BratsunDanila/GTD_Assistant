package com.sbros.app

import com.sbros.app.data.local.Converters
import com.sbros.app.data.local.SeedData
import com.sbros.app.domain.model.EnergyLevel
import com.sbros.app.domain.model.ShelfType
import com.sbros.app.domain.model.ThemePalette
import com.sbros.app.domain.model.toDomain
import com.sbros.app.ui.theme.getSbrosColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage1And2UnitTest {

    @Test
    fun testColorPalettesForAllThemesAndModes() {
        val palettes = ThemePalette.entries.toTypedArray()
        for (palette in palettes) {
            val lightScheme = getSbrosColorScheme(palette, darkTheme = false)
            val darkScheme = getSbrosColorScheme(palette, darkTheme = true)

            assertNotNull(lightScheme.primary)
            assertNotNull(lightScheme.surface)
            assertNotNull(lightScheme.surfaceContainerLow)
            assertNotNull(lightScheme.surfaceContainer)
            assertNotNull(lightScheme.surfaceContainerHigh)

            assertNotNull(darkScheme.primary)
            assertNotNull(darkScheme.surface)
            assertNotNull(darkScheme.surfaceContainerLow)
            assertNotNull(darkScheme.surfaceContainer)
            assertNotNull(darkScheme.surfaceContainerHigh)
        }
    }

    @Test
    fun testConverters() {
        val converters = Converters()

        // List<String>
        val steps = listOf("Шаг 1: Открыть приложение", "Шаг 2: Нажать кнопку")
        val json = converters.fromStringList(steps)
        val decoded = converters.toStringList(json)
        assertEquals(steps, decoded)

        // Empty list fallback
        assertEquals(emptyList<String>(), converters.toStringList(null))
        assertEquals(emptyList<String>(), converters.toStringList(""))

        // ShelfType
        assertEquals(ShelfType.BIG_THINGS, converters.toShelfType("BIG_THINGS"))
        assertEquals(ShelfType.SIMPLE_STEPS, converters.toShelfType("INVALID_SHELF"))

        // EnergyLevel
        assertEquals(EnergyLevel.FLOW, converters.toEnergyLevel("FOCUS"))
        assertEquals(EnergyLevel.MECHANICAL, converters.toEnergyLevel("UNKNOWN_LEVEL"))
    }

    @Test
    fun testTaskItemSimplificationText() {
        val task = SeedData.initialTasks.first { it.id.contains("batteries-buy") }.toDomain()

        // Index 0: исходный заголовок
        assertEquals("Заказать аккумуляторы АА на маркетплейсе", task.copy(currentSimplifyIndex = 0).currentStepText)

        // Index 1: первый уровень упрощения
        assertEquals(
            "Открыть маркетплейс и вбить «Аккумуляторы АА Ni-MH 4 шт»",
            task.copy(currentSimplifyIndex = 1).currentStepText
        )

        // Index 2: микро-старт
        assertEquals(
            "Положить первую подходящую упаковку в корзину",
            task.copy(currentSimplifyIndex = 2).currentStepText
        )

        // Index fallback: если вышли за границы
        assertEquals(
            "Заказать аккумуляторы АА на маркетплейсе",
            task.copy(currentSimplifyIndex = 99).currentStepText
        )
    }

    @Test
    fun testSeedDataIntegrity() {
        val projects = SeedData.initialProjects
        val tasks = SeedData.initialTasks

        assertEquals(2, projects.size)
        assertTrue(tasks.size >= 8)

        // Все задачи, привязанные к проекту, должны иметь существующий projectId
        val projectIds = projects.map { it.id }.toSet()
        tasks.filter { it.projectId != null }.forEach { task ->
            assertTrue(projectIds.contains(task.projectId))
        }

        // Проверяем наличие задач под все 3 уровня энергии
        assertTrue(tasks.any { it.energyLevel == EnergyLevel.MECHANICAL && !it.isCompleted })
        assertTrue(tasks.any { it.energyLevel == EnergyLevel.SINGLE_STEP && !it.isCompleted })
        assertTrue(tasks.any { it.energyLevel == EnergyLevel.FLOW && !it.isCompleted })

        // Проверяем наличие задач под все 4 полки
        assertTrue(tasks.any { it.shelf == ShelfType.SIMPLE_STEPS })
        assertTrue(tasks.any { it.shelf == ShelfType.BIG_THINGS })
        assertTrue(tasks.any { it.shelf == ShelfType.WAITING })
        assertTrue(tasks.any { it.shelf == ShelfType.SOMEDAY })

        // Проверяем, что в курсовой есть 2 шага, и 1-й шаг имеет orderInProject = 1
        val courseworkTasks = tasks.filter { it.projectId == "project-coursework" }
        assertEquals(2, courseworkTasks.size)
        val firstCourseworkStep = courseworkTasks.minByOrNull { it.orderInProject }
        assertNotNull(firstCourseworkStep)
        assertEquals(1, firstCourseworkStep?.orderInProject)
        assertEquals("Написать черновик введения (3 абзаца)", firstCourseworkStep?.title)
    }
}
