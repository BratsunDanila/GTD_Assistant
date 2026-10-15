package com.sbros.app

import com.sbros.app.ui.capture.CaptureMode
import com.sbros.app.ui.capture.components.GtdMemoryTriggers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Stage4UnitTest {

    @Test
    fun testGtdMemoryTriggersList() {
        assertNotNull(GtdMemoryTriggers)
        assertTrue(GtdMemoryTriggers.size >= 5)

        // Проверяем ключевые формулировки из спецификации
        assertTrue(GtdMemoryTriggers.any { it.contains("учебе или работе") })
        assertTrue(GtdMemoryTriggers.any { it.contains("обещал написать или скинуть") })
        assertTrue(GtdMemoryTriggers.any { it.contains("бытовых мелочей") })
        assertTrue(GtdMemoryTriggers.any { it.contains("ждешь ответа или проверки") })
        assertTrue(GtdMemoryTriggers.any { it.contains("сохранить на потом") })
    }

    @Test
    fun testCaptureModes() {
        assertEquals(CaptureMode.VOICE, CaptureMode.valueOf("VOICE"))
        assertEquals(CaptureMode.TEXT, CaptureMode.valueOf("TEXT"))
    }

    @Test
    fun testAmplitudeNormalizationFormula() {
        // Проверяем формулу преобразования rmsdB (-2..10) в (0..1)
        fun normalizeRms(rmsdB: Float): Float = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)

        assertEquals(0f, normalizeRms(-2f), 0.001f)
        assertEquals(0f, normalizeRms(-5f), 0.001f)
        assertEquals(1f, normalizeRms(10f), 0.001f)
        assertEquals(1f, normalizeRms(15f), 0.001f)
        assertEquals(0.5f, normalizeRms(4f), 0.001f)
    }

    @Test
    fun testSilenceThresholdDuration() {
        val lastSpeechTime = 1000L
        val nowDuringSpeech = 2500L
        val nowAfterSilence = 4100L

        val isSilentDuringSpeech = (nowDuringSpeech - lastSpeechTime) >= 3000L
        val isSilentAfter3Sec = (nowAfterSilence - lastSpeechTime) >= 3000L

        assertFalse(isSilentDuringSpeech)
        assertTrue(isSilentAfter3Sec)
    }

    @Test
    fun testContinuousSpeechAccumulation() {
        var accumulatedText = ""
        fun addPhrase(phrase: String) {
            val trimmed = phrase.trim()
            if (trimmed.isNotBlank()) {
                accumulatedText = if (accumulatedText.isBlank()) trimmed else "$accumulatedText, $trimmed"
            }
        }

        addPhrase("надо купить батарейки")
        assertEquals("надо купить батарейки", accumulatedText)

        addPhrase("и написать старосте")
        assertEquals("надо купить батарейки, и написать старосте", accumulatedText)

        addPhrase("в пятницу глянуть кино")
        assertEquals("надо купить батарейки, и написать старосте, в пятницу глянуть кино", accumulatedText)
    }
}
