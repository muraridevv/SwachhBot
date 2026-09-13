package com.example.swachhbot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.swachhbot.model.MoveIntent
import com.example.swachhbot.model.RobotState
import com.example.swachhbot.model.TurnIntent
import com.example.swachhbot.simulation.RobotSimulator
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SimulatorViewModel : ViewModel() {
    private val simulator = RobotSimulator()
    
    val robotState: StateFlow<RobotState> = simulator.robotState

    init {
        // Start the physics simulation loop in the ViewModel scope
        viewModelScope.launch {
            simulator.runSimulationLoop()
        }
    }

    fun updateRoomSize(width: Float, height: Float) {
        simulator.updateRoomSize(width, height)
    }

    // Setters for continuous intents instead of discrete one-off movements
    fun setMoveIntent(intent: MoveIntent) {
        simulator.moveIntent = intent
    }

    fun setTurnIntent(intent: TurnIntent) {
        simulator.turnIntent = intent
    }

    fun stop() {
        simulator.moveIntent = MoveIntent.NONE
        simulator.turnIntent = TurnIntent.NONE
    }
}
