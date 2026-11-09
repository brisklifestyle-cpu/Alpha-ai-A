package com.alphaai.devilbreath

import android.content.Context
import android.content.Intent
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
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
    val name: String, val bass: Float, val mid: Float, val treble: Float,
    val clarity: Float, val dynamics: Float, val description: String
)

private val soundPresets = listOf(
    SoundPreset("International Hi-Fi", 50f, 55f, 65f, 75f, 65f, "Balanced detail, natural vocals and controlled bass"),
    SoundPreset("Bass", 92f, 48f, 52f, 55f, 82f, "Deep low-end character"),
    SoundPreset("Vocal", 35f, 88f, 72f, 82f, 52f, "Forward, clear voices and dialogue"),
    SoundPreset("Boom", 100f, 42f, 58f, 58f, 95f, "Powerful, punchy listening character"),
    SoundPreset("Cinematic", 76f, 62f, 84f, 86f, 90f, "Dramatic movie profile"),
    SoundPreset("Broadcast", 30f, 86f, 90f, 92f, 55f, "Speech-first clarity"),
    SoundPreset("Sports", 55f, 74f, 86f, 84f, 76f, "Commentary and crowd detail"),
    SoundPreset("Night", 32f, 72f, 62f, 68f, 25f, "Gentler dynamics for late listening"),
    SoundPreset("Custom EQ", 50f, 50f, 50f, 50f, 50f, "Your saved personal profile")
)

private data class SpeakerRoute(val name: String, val isMegaboom: Boolean)

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
    var mid by remember { mutableFloatStateOf(prefs.getFloat("mid", 50f)) }
    var treble by remember { mutableFloatStateOf(prefs.getFloat("treble", 50f)) }
    var clarity by remember { mutableFloatStateOf(prefs.getFloat("clarity", 50f)) }
    var dynamics by remember { mutableFloatStateOf(prefs.getFloat("dynamics", 50f)) }
    var route by remember { mutableStateOf<SpeakerRoute?>(null) }
    var routeChecked by remember { mutableStateOf(false) }
    var eqStatus by remember { mutableStateOf("ফোনের audio engine পরীক্ষা হচ্ছে…") }
    val systemEq = remember { runCatching { Equalizer(0, 0).apply { enabled = true } }.getOrNull() }
    val muted = currentVolume == 0
    val percent = (currentVolume * 100f / maxVolume).roundToInt()

    fun refreshRoute() {
        val outputs = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val bluetooth = outputs.firstOrNull {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
        route = bluetooth?.let {
            val label = runCatching { it.productName.toString() }.getOrDefault("Bluetooth audio device")
            SpeakerRoute(label.ifBlank { "Bluetooth audio device" },
                label.contains("megaboom", true) || label.contains("ue", true) || label.contains("ultimate ears", true))
        }
        routeChecked = true
        currentVolume = audio.getStreamVolume(stream)
    }

    fun saveProfile() {
        prefs.edit().putString("selected_preset", selectedPreset)
            .putFloat("bass", bass).putFloat("mid", mid).putFloat("treble", treble)
            .putFloat("clarity", clarity).putFloat("dynamics", dynamics).apply()
    }
    fun selectPreset(preset: SoundPreset) {
        selectedPreset = preset.name
        bass = preset.bass; mid = preset.mid; treble = preset.treble
        clarity = preset.clarity; dynamics = preset.dynamics
        saveProfile()
    }
    fun setVolume(value: Int) {
        val safe = value.coerceIn(0, maxVolume)
        audio.setStreamVolume(stream, safe, AudioManager.FLAG_SHOW_UI)
        currentVolume = safe
        if (safe > 0) lastAudibleVolume = safe
    }

    LaunchedEffect(Unit) { refreshRoute() }
    LaunchedEffect(bass, mid, treble, systemEq) {
        val eq = systemEq
        if (eq == null) {
            eqStatus = "এই ফোনের Android audio engine global EQ অনুমোদন করেনি।"
        } else {
            val result = runCatching {
                val count = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val maxBoost = range[1].toInt()
                val minCut = range[0].toInt()
                val values = listOf(bass, mid, treble, treble, treble)
                for (i in 0 until count) {
                    val value = values[(i * values.size / count).coerceIn(0, values.lastIndex)]
                    val level = (minCut + (maxBoost - minCut) * (value / 100f)).roundToInt()
                    eq.setBandLevel(i.toShort(), level.coerceIn(minCut, maxBoost).toShort())
                }
                eq.enabled = true
            }
            eqStatus = if (result.isSuccess) "ফোনের system audio EQ চালু আছে; Bluetooth output-এ প্রভাব পড়তে পারে।" else "ফোনের audio EQ প্রয়োগ করা যায়নি: ${result.exceptionOrNull()?.localizedMessage ?: "unsupported"}"
        }
    }
    DisposableEffect(systemEq) {
        onDispose { runCatching { systemEq?.release() } }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("ALPHA INTERNATIONAL SOUND EXPERIENCE", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge)
        Text("একটি আলাদা listening studio—এক ট্যাপে সাউন্ড মোড, নিজের EQ প্রোফাইল এবং স্পিকার সংযোগের অবস্থা।", color = Color.LightGray)

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null, tint = Color(0xFFE32636))
                    Text("MEGABOOM 3 • DEVICE SETUP", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                }
                if (!routeChecked) Text("Bluetooth audio route পরীক্ষা হচ্ছে…", color = Color.LightGray)
                else if (route == null) {
                    Text("ফোনে কোনো Bluetooth audio output route শনাক্ত হয়নি।", color = Color.LightGray)
                    Text("MEGABOOM 3 যদি Hisense TV-তে যুক্ত থাকে, ফোনে এটি connected দেখাবে না—এটি স্বাভাবিক।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
                } else {
                    Text(if (route!!.isMegaboom) "Bluetooth speaker route পাওয়া গেছে: ${route!!.name}" else "Bluetooth audio route: ${route!!.name}", color = Color.White)
                    Text(if (route!!.isMegaboom) "MEGABOOM 3 নামের সঙ্গে মিল পাওয়া গেছে। ফোনের media volume এই Bluetooth route-এ পাঠানো হবে।" else "Bluetooth audio সংযুক্ত। MEGABOOM 3 হিসেবে নিশ্চিত করা যায়নি।", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { refreshRoute() }, modifier = Modifier.weight(1f)) { Text("Refresh status") }
                    Button(onClick = { context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS)) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))) { Text("Pair / Connect") }
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ONE-TOUCH SOUND EXPERIENCES", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                soundPresets.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { preset ->
                            FilterChip(selected = selectedPreset == preset.name, onClick = { selectPreset(preset) },
                                label = { Text(preset.name, maxLines = 1) }, modifier = Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                Text(soundPresets.firstOrNull { it.name == selectedPreset }?.description ?: "Custom listening profile", color = Color.LightGray)
                Text("Selected profile: $selectedPreset", color = Color.White, style = MaterialTheme.typography.labelLarge)
                Text(eqStatus, color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("PERSONAL EQ PROFILE", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                SoundParameter("Bass", bass) { bass = it; selectedPreset = "Custom EQ" }
                SoundParameter("Mid / Vocal body", mid) { mid = it; selectedPreset = "Custom EQ" }
                SoundParameter("Treble", treble) { treble = it; selectedPreset = "Custom EQ" }
                SoundParameter("Clarity", clarity) { clarity = it; selectedPreset = "Custom EQ" }
                SoundParameter("Dynamic Range", dynamics) { dynamics = it; selectedPreset = "Custom EQ" }
                Button(onClick = { saveProfile() }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))) { Text("Save My Sound Profile") }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (route != null) "PHONE → BLUETOOTH VOLUME" else "PHONE MEDIA VOLUME", color = Color(0xFFE32636), style = MaterialTheme.typography.titleMedium)
                Icon(if (muted) Icons.Default.VolumeMute else if (percent < 50) Icons.Default.VolumeDown else Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFFE32636), modifier = Modifier.size(42.dp))
                Text("$percent%", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Slider(value = currentVolume.toFloat(), onValueChange = { setVolume(it.roundToInt()) }, valueRange = 0f..maxVolume.toFloat(), modifier = Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { setVolume(currentVolume - 1) }, modifier = Modifier.weight(1f)) { Text("− কমান") }
                    Button(onClick = { setVolume(if (muted) lastAudibleVolume else 0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))) { Text(if (muted) "Unmute" else "Mute") }
                    OutlinedButton(onClick = { setVolume(currentVolume + 1) }, modifier = Modifier.weight(1f)) { Text("+ বাড়ান") }
                }
                TextButton(onClick = { refreshRoute() }) { Text("ভলিউম ও সংযোগ Refresh") }
            }
        }
        Text("গুরুত্বপূর্ণ: Android global EQ সমর্থন করলে ফোন থেকে বের হওয়া অডিওতে প্রভাব ফেলতে পারে; এটি MEGABOOM 3-এর নিজস্ব DSP পরিবর্তন নয় এবং TV-এর নিজস্ব অ্যাপ/অন্য HDMI উৎসের অডিওতে কাজ নাও করতে পারে। TV-তে যুক্ত অবস্থায় volume নিয়ন্ত্রণের জন্য Hisense TV remote protocol এখনো আলাদা করে যুক্ত করতে হবে।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
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
