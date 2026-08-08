package com.hdclark.simbikepark

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import org.json.JSONArray
import org.json.JSONObject

class ParkViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("bike_park_saves", 0)
    val trail = mutableStateListOf<TrailPiece>()
    var nextHeading by mutableIntStateOf(0); private set
    var simulation by mutableStateOf<List<SimulationPlan>>(emptyList()); private set
    var simulationRunning by mutableStateOf(false); private set
    var lastReport by mutableStateOf<String?>(null); private set
    var savedNames by mutableStateOf(loadNames()); private set

    fun add(type: FeatureType) { trail += TrailPiece(type, nextHeading); lastReport = null }
    fun undo() { if (trail.isNotEmpty()) trail.removeAt(trail.lastIndex) }
    fun rotate(delta: Int) { nextHeading = (nextHeading + delta).mod(4) }
    fun clear() { trail.clear(); simulation = emptyList(); simulationRunning = false; lastReport = null; nextHeading = 0 }

    fun startSimulation() {
        if (trail.isEmpty()) return
        simulation = TrackSimulator.plan(trail.toList(), (System.nanoTime() xor trail.hashCode().toLong()).toInt())
        simulationRunning = true
        lastReport = null
    }

    fun finishSimulation() {
        simulationRunning = false
        val crashed = simulation.filter { it.crashPiece != null }
        lastReport = if (crashed.isEmpty()) {
            "Miraculous clean run. The trail crew suspects witchcraft. ✨"
        } else {
            crashed.joinToString("\n") { "${it.rider.emoji} ${it.rider.name} ${it.crashFlavor}." }
        }
    }

    fun save(nameRaw: String) {
        val name = nameRaw.trim().ifEmpty { "Mystery Meat Trail" }
        val arr = JSONArray()
        trail.forEach { arr.put(JSONObject().put("type", it.type.name).put("heading", it.heading)) }
        prefs.edit().putString("trail:$name", arr.toString()).apply()
        savedNames = loadNames()
    }

    fun load(name: String) {
        val raw = prefs.getString("trail:$name", null) ?: return
        val arr = JSONArray(raw)
        trail.clear()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            runCatching { FeatureType.valueOf(obj.getString("type")) }.getOrNull()?.let {
                trail += TrailPiece(it, obj.optInt("heading", 0))
            }
        }
        nextHeading = trail.lastOrNull()?.heading ?: 0
        simulation = emptyList(); simulationRunning = false; lastReport = null
    }

    fun deleteSave(name: String) { prefs.edit().remove("trail:$name").apply(); savedNames = loadNames() }

    private fun loadNames(): List<String> = prefs.all.keys.filter { it.startsWith("trail:") }.map { it.removePrefix("trail:") }.sorted()
}
