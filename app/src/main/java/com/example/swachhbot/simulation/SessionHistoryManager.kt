package com.example.swachhbot.simulation

import android.content.Context
import com.example.swachhbot.model.CleaningSession
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class SessionHistoryManager(context: Context) {
    private val prefs = context.getSharedPreferences("SwachhBotHistory", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    fun saveSession(session: CleaningSession) {
        val history = getSessions().toMutableList()
        history.add(session)
        val json = gson.toJson(history)
        prefs.edit().putString("sessions", json).apply()
    }
    
    fun getSessions(): List<CleaningSession> {
        val json = prefs.getString("sessions", null) ?: return emptyList()
        val type = object : TypeToken<List<CleaningSession>>() {}.type
        return gson.fromJson(json, type)
    }
}