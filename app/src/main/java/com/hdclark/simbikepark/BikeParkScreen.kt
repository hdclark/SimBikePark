package com.hdclark.simbikepark

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BikeParkScreen(vm: ParkViewModel) {
    var showSave by remember { mutableStateOf(false) }
    var showLoad by remember { mutableStateOf(false) }
    var elapsed by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(vm.simulationRunning, vm.simulation) {
        if (!vm.simulationRunning) return@LaunchedEffect
        elapsed = 0f
        var last = withFrameNanos { it }
        while (isActive && vm.simulationRunning) {
            val now = withFrameNanos { it }
            elapsed += (now - last) / 1_000_000_000f
            last = now
            if (elapsed > 9.2f) vm.finishSimulation()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Sim Bike Park 🚲", fontWeight = FontWeight.Black)
                        Text("Build it. Send it. Regret it.", style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            ActionRow(
                canUndo = vm.trail.isNotEmpty(),
                canSim = vm.trail.isNotEmpty() && !vm.simulationRunning,
                running = vm.simulationRunning,
                onUndo = vm::undo,
                onNew = vm::clear,
                onSave = { showSave = true },
                onLoad = { showLoad = true },
                onSim = vm::startSimulation
            )

            Box(
                Modifier
                    .padding(horizontal = 10.dp)
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                IsometricPark(
                    pieces = vm.trail,
                    plans = vm.simulation,
                    elapsed = elapsed,
                    running = vm.simulationRunning,
                    modifier = Modifier.fillMaxSize()
                )
                if (vm.trail.isEmpty()) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        shape = RoundedCornerShape(18.dp),
                        tonalElevation = 4.dp
                    ) {
                        Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏗️", style = MaterialTheme.typography.displaySmall)
                            Text("Your mountain is suspiciously safe.", fontWeight = FontWeight.Bold)
                            Text("Tap a feature below to snap the first block into place.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (vm.simulationRunning) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Three riders released!  🧑‍🚀🧑‍🔧🧑‍🎤") },
                        modifier = Modifier.align(Alignment.TopCenter).padding(10.dp)
                    )
                }
            }

            AnimatedVisibility(vm.lastReport != null) {
                Surface(
                    Modifier.padding(10.dp).fillMaxWidth().animateContentSize(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Crash marshal report 💥", fontWeight = FontWeight.Black)
                        Text(vm.lastReport.orEmpty(), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            HeadingRow(vm.nextHeading, onRotate = vm::rotate)
            FeaturePalette(onPick = vm::add)
        }
    }

    if (showSave) SaveDialog(onDismiss = { showSave = false }, onSave = { vm.save(it); showSave = false })
    if (showLoad) LoadDialog(vm.savedNames, onDismiss = { showLoad = false }, onLoad = { vm.load(it); showLoad = false }, onDelete = vm::deleteSave)
}

@Composable
private fun ActionRow(canUndo: Boolean, canSim: Boolean, running: Boolean, onUndo: () -> Unit, onNew: () -> Unit, onSave: () -> Unit, onLoad: () -> Unit, onSim: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(onClick = onUndo, enabled = canUndo, contentPadding = PaddingValues(10.dp)) { Text("↶") }
        FilledTonalButton(onClick = onNew, contentPadding = PaddingValues(10.dp)) { Text("🧹") }
        FilledTonalButton(onClick = onSave, contentPadding = PaddingValues(10.dp)) { Text("💾") }
        FilledTonalButton(onClick = onLoad, contentPadding = PaddingValues(10.dp)) { Text("📂") }
        Button(onClick = onSim, enabled = canSim, modifier = Modifier.weight(1f)) { Text(if (running) "Sending…" else "SEND 3 RIDERS 💥") }
    }
}

@Composable
private fun HeadingRow(heading: Int, onRotate: (Int) -> Unit) {
    val arrow = listOf("↗", "↘", "↙", "↖")[heading.mod(4)]
    Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Next block: $arrow", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        FilledTonalButton(onClick = { onRotate(-1) }) { Text("Turn left ↶") }
        Spacer(Modifier.width(6.dp))
        FilledTonalButton(onClick = { onRotate(1) }) { Text("↷ Right") }
    }
}

@Composable
private fun FeaturePalette(onPick: (FeatureType) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 8.dp, end = 8.dp, bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        FeatureType.entries.forEach { type ->
            ElevatedButton(onClick = { onPick(type) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(type.emoji, style = MaterialTheme.typography.titleLarge)
                    Text(type.title, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SaveDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name this questionable masterpiece") },
        text = { OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text("e.g. Liability Waiver Express") }) },
        confirmButton = { Button(onClick = { onSave(name) }) { Text("Save 💾") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun LoadDialog(names: List<String>, onDismiss: () -> Unit, onLoad: (String) -> Unit, onDelete: (String) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Saved trails 📂") },
        text = {
            if (names.isEmpty()) Text("No saves yet. The insurance company is relieved.")
            else Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                names.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { onLoad(name) }, modifier = Modifier.weight(1f)) { Text(name) }
                        IconButton(onClick = { onDelete(name) }) { Text("🗑️") }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
