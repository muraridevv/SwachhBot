package com.example.swachhbot

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanerSafetyMonitorTest {
    @Test
    fun `reports caution after a persistent medium obstacle`() {
        val monitor = CleanerSafetyMonitor()
        val obstacle = DetectionBox(300, 300) // 9% of a 1000 x 1000 camera frame

        assertEquals(SafetyState.CLEAR, monitor.update(listOf(obstacle), 1000, 1000))
        assertEquals(SafetyState.CAUTION, monitor.update(listOf(obstacle), 1000, 1000))
    }

    @Test
    fun `stops immediately for a close obstacle`() {
        val monitor = CleanerSafetyMonitor()
        val closeObstacle = DetectionBox(600, 600) // 36% coverage

        assertEquals(SafetyState.STOP, monitor.update(listOf(closeObstacle), 1000, 1000))
    }

    @Test
    fun `returns to clear only after empty frames are confirmed`() {
        val monitor = CleanerSafetyMonitor()
        val obstacle = DetectionBox(300, 300)
        monitor.update(listOf(obstacle), 1000, 1000)
        monitor.update(listOf(obstacle), 1000, 1000)

        assertEquals(SafetyState.CAUTION, monitor.update(emptyList(), 1000, 1000))
        assertEquals(SafetyState.CLEAR, monitor.update(emptyList(), 1000, 1000))
    }
}
