package com.alphaai.devilbreath

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

private data class SoundPreset(
    val name: String,
    val bass: Float,
    val vocal: Float,
    val clarity: Float,
    val dynamics: Float,
    val description: String
)

private val soundPresets = listOf(
    SoundPreset("International Hi-Fi", 50f, 50f, 75f, 65f, "Balanced, detailed everyday listening"),
    SoundPreset("Bass", 90f, 45f, 55f, 80f, "Deep low-frequency emphasis"),
    SoundPreset("Vocal", 35f, 90f, 75f, 50f, "Clear dialogue and vocals"),
    SoundPreset("Boom", 100f, 40f, 60f, 95f, "High-impact bass character"),
    SoundPreset("Cinematic", 75f, 65f, 85f, 90f, "Wide, dramatic movie character"),
    SoundPreset("Broadcast", 30f, 85f, 90f, 55f, "Speech-first presentation"),
    SoundPreset("Sports", 55f, 70f, 85f, 75f, "Commentary and crowd detail"),
    SoundPreset("Night", 35f, 70f, 65f, 25f, "Reduced dynamic intensity"),
    SoundPreset("Custom EQ", 50f, 50f, 50f, 50f, "Your own saved profile")
)

@Composable
fun SoundControlScreen() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val prefs = remember { context.getSharedPreferences("alpha_sound_profiles", Context.MODE_PRIVATE) }
    val stream = AudioManager.STREAM_MUSIC
    val maxVolume = remember { audio.getStreamMaxVolume(stream).coerceAtLeast(1) }
    var currentVolume by remember { mutableIntStateOf(audio.getStreamVolume(stream)) }
    var lastAudibleVolume by remember { mutableIntStateOf(currentVolume.coerceAtLeast(1)) }
    var selectedPreset by remember { mutableStateOf(prefs.getString("selected_preset", "International Hi-Fi") ?: "International Hi-Fi") }
    var bass by remember { mutableFloatStateOf(prefs.getFloat("bass", 50f)) }
    var vocal by remember { mutableFloatStateOf(prefs.getFloat("vocal", 50f)) }
    var clarity by remember { mutableFloatStateOf(prefs.getFloat("clarity", 50f)) }
    var dynamics by remember { mutableFloatStateOf(prefs.getFloat("dynamics", 50f)) }
    val muted = currentVolume == 0
    val percent = (currentVolume * 100f / maxVolume).roundToInt()

    fun saveProfile() {
        prefs.edit()
            .putString("selected_preset", selectedPreset)
            .putFloat("bass", bass).putFloat("vocal", vocal)
            .putFloat("clarity", clarity).putFloat("dynamics", dynamics).apply()
    }
    fun selectPreset(preset: SoundPreset) {
        selectedPreset = preset.name
        bass = preset.bass
        vocal = preset.vocal
        clarity = preset.clarity
        dynamics = preset.dynamics
        saveProfile()
    }
    fun setVolume(value: Int) {
        val safe = value.coerceIn(0, maxVolume)
        audio.setStreamVolume(stream, safe, AudioManager.FLAG_SHOW_UI)
        currentVolume = safe
        if (safe > 0) lastAudibleVolume = safe
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("ALPHA INTERNATIONAL SOUND ENGINE", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge)
        Text("আপনার সাউন্ড প্রোফাইল Alpha AI-এর ভেতরেই তৈরি ও সংরক্ষণ করুন।", color = Color.LightGray)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("SOUND PROFILES", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                soundPresets.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            FilterChip(
                                selected = selectedPreset == preset.name,
                                onClick = { selectPreset(preset) },
                                label = { Text(preset.name, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text(soundPresets.firstOrNull { it.name == selectedPreset }?.description ?: "Custom sound profile", color = Color.LightGray)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CUSTOM TUNING", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                SoundParameter("Bass", bass) { bass = it; selectedPreset = "Custom EQ" }
                SoundParameter("Vocal", vocal) { vocal = it; selectedPreset = "Custom EQ" }
                SoundParameter("Clarity", clarity) { clarity = it; selectedPreset = "Custom EQ" }
                SoundParameter("Dynamic Range", dynamics) { dynamics = it; selectedPreset = "Custom EQ" }
                Button(onClick = { saveProfile() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))) {
                    Text("Save My Custom Profile")
                }
                Text("প্রোফাইলটি Alpha AI-তে সংরক্ষিত। এটি এখনো MEGABOOM 3 বা TV-তে DSP/EQ প্রয়োগ করছে না।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("PHONE MEDIA VOLUME", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                Icon(if (muted) Icons.Default.VolumeMute else if (percent < 50) Icons.Default.VolumeDown else Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFE32636), modifier = Modifier.size(42.dp))
                Text("$percent%", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Slider(value = currentVolume.toFloat(), onValueChange = { setVolume(it.roundToInt()) }, valueRange = 0f..maxVolume.toFloat(), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { setVolume(currentVolume - 1) }, modifier = Modifier.weight(1f)) { Text("− কমান") }
                    Button(onClick = { setVolume(if (muted) lastAudibleVolume else 0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))) { Text(if (muted) "Unmute" else "Mute") }
                    OutlinedButton(onClick = { setVolume(currentVolume + 1) }, modifier = Modifier.weight(1f)) { Text("+ বাড়ান") }
                }
                TextButton(onClick = { currentVolume = audio.getStreamVolume(stream) }) { Text("ভলিউম রিফ্রেশ") }
            }
        }
        Text("ডিভাইস সীমাবদ্ধতা: এই ভলিউম Android ফোনের Media stream নিয়ন্ত্রণ করে। MEGABOOM 3 TV-তে Bluetooth দিয়ে যুক্ত থাকলে ফোনের এই slider TV-এর volume বদলায় না। ফোনে যুক্ত থাকলেও প্রকৃত EQ প্রয়োগ আলাদা audio-processing integration ছাড়া নিশ্চিত নয়।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SoundParameter(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White)
            Text("${value.roundToInt()}%", color = Color.LightGray)
        }
        Slider(value = value, onValueChange = onValueChange, valueRange = 0f..100f)
    }
}
