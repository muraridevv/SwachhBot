package com.example.swachhbot.simulation

import com.example.swachhbot.model.MoveIntent
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.TurnIntent

/**
 * Interface for navigation algorithms. This can later be implemented by A*,
 * coverage path planning, or an AI-assisted module.
 */
interface CleaningPlanner {
    fun getNextIntents(
        robot: RobotState,
        collisionAt: (Float, Float) -> Boolean
    ): Pair<MoveIntent, TurnIntent>
    
    fun reset()
}

/**
 * A basic Boustophedon (Zig-Zag) coverage planner.
 * Sweeps East, drops South, Sweeps West, drops South, repeats.
 */
class ZigZagPlanner : CleaningPlanner {
    enum class State {
        SWEEP_EAST, TURN_SOUTH_1, MOVE_SOUTH_1, TURN_WEST,
        SWEEP_WEST, TURN_SOUTH_2, MOVE_SOUTH_2, TURN_EAST, DONE
    }

    private var state = State.SWEEP_EAST
    private var targetY = 0f

    override fun reset() {
        state = State.SWEEP_EAST
    }

    override fun getNextIntents(robot: RobotState, collisionAt: (Float, Float) -> Boolean): Pair<MoveIntent, TurnIntent> {
        // Look slightly ahead of the robot (35 units, knowing radius is 30)
        val rad = Math.toRadians((robot.rotationDegrees - 90).toDouble())
        val lookAheadX = robot.x + Math.cos(rad).toFloat() * 35f
        val lookAheadY = robot.y + Math.sin(rad).toFloat() * 35f
        val obstacleAhead = collisionAt(lookAheadX, lookAheadY)

        return when (state) {
            State.SWEEP_EAST -> {
                val turn = turnTowards(robot.rotationDegrees, 90f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                if (obstacleAhead) {
                    targetY = robot.y + 60f // Shift down by 2x radius
                    state = State.TURN_SOUTH_1
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                Pair(MoveIntent.FORWARD, TurnIntent.NONE)
            }
            State.TURN_SOUTH_1 -> {
                val turn = turnTowards(robot.rotationDegrees, 180f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                state = State.MOVE_SOUTH_1
                Pair(MoveIntent.NONE, TurnIntent.NONE)
            }
            State.MOVE_SOUTH_1 -> {
                if (robot.y >= targetY) {
                    state = State.TURN_WEST
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                if (obstacleAhead) {
                    // Cornered while shifting, abort or end room
                    state = State.DONE
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                Pair(MoveIntent.FORWARD, TurnIntent.NONE)
            }
            State.TURN_WEST -> {
                val turn = turnTowards(robot.rotationDegrees, 270f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                state = State.SWEEP_WEST
                Pair(MoveIntent.NONE, TurnIntent.NONE)
            }
            State.SWEEP_WEST -> {
                val turn = turnTowards(robot.rotationDegrees, 270f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                if (obstacleAhead) {
                    targetY = robot.y + 60f
                    state = State.TURN_SOUTH_2
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                Pair(MoveIntent.FORWARD, TurnIntent.NONE)
            }
            State.TURN_SOUTH_2 -> {
                val turn = turnTowards(robot.rotationDegrees, 180f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                state = State.MOVE_SOUTH_2
                Pair(MoveIntent.NONE, TurnIntent.NONE)
            }
            State.MOVE_SOUTH_2 -> {
                if (robot.y >= targetY) {
                    state = State.TURN_EAST
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                if (obstacleAhead) {
                    state = State.DONE
                    return Pair(MoveIntent.NONE, TurnIntent.NONE)
                }
                Pair(MoveIntent.FORWARD, TurnIntent.NONE)
            }
            State.TURN_EAST -> {
                val turn = turnTowards(robot.rotationDegrees, 90f)
                if (turn != TurnIntent.NONE) return Pair(MoveIntent.NONE, turn)
                state = State.SWEEP_EAST
                Pair(MoveIntent.NONE, TurnIntent.NONE)
            }
            State.DONE -> {
                Pair(MoveIntent.NONE, TurnIntent.NONE)
            }
        }
    }

    private fun turnTowards(current: Float, target: Float): TurnIntent {
        val diff = (target - current + 360) % 360
        if (diff < 5f || diff > 355f) return TurnIntent.NONE
        return if (diff < 180f) TurnIntent.RIGHT else TurnIntent.LEFT
    }
}
