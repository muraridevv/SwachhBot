package com.example.swachhbot

/**
 * Converts camera detections into conservative movement advice for a future cleaner.
 *
 * This is deliberately independent of robot hardware: an integration can map [SafetyState]
 * to stop, slow, or resume commands without changing the camera pipeline.
 */
class CleanerSafetyMonitor {
    private var candidate = SafetyState.CLEAR
    private var candidateFrames = 0
    private var reported = SafetyState.CLEAR

    fun update(boxes: List<DetectionBox>, imageWidth: Int, imageHeight: Int): SafetyState {
        val imageArea = (imageWidth.toLong() * imageHeight.toLong()).coerceAtLeast(1L)
        val largestCoverage = boxes.maxOfOrNull { box ->
            (box.width.coerceAtLeast(0).toLong() * box.height.coerceAtLeast(0)) / imageArea.toDouble()
        } ?: 0.0
        val next = when {
            largestCoverage >= EMERGENCY_STOP_COVERAGE -> SafetyState.STOP
            largestCoverage >= STOP_COVERAGE -> SafetyState.STOP
            largestCoverage >= CAUTION_COVERAGE -> SafetyState.CAUTION
            else -> SafetyState.CLEAR
        }

        if (next == candidate) {
            candidateFrames++
        } else {
            candidate = next
            candidateFrames = 1
        }
        // A very close obstacle must not wait for frame confirmation.
        if (largestCoverage >= EMERGENCY_STOP_COVERAGE || candidateFrames >= CONFIRMATION_FRAMES) {
            reported = candidate
        }
        return reported
    }

    companion object {
        private const val CAUTION_COVERAGE = 0.05
        private const val STOP_COVERAGE = 0.18
        private const val EMERGENCY_STOP_COVERAGE = 0.34
        private const val CONFIRMATION_FRAMES = 2
    }
}

/** Geometry-only representation so the safety policy can be tested without Android framework classes. */
data class DetectionBox(val width: Int, val height: Int)

enum class SafetyState(val message: String) {
    CLEAR("PATH CLEAR • SAFE TO MOVE"),
    CAUTION("CAUTION • SLOW FOR OBSTACLE"),
    STOP("STOP • OBSTACLE AHEAD")
}
