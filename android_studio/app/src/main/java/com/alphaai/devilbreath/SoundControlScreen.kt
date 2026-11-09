package com.alphaai.devilbreath

import android.content.Context
import android.media.AudioManager
import androidx.compose.foundation.layout.*
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

@Composable
fun SoundControlScreen() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val stream = AudioManager.STREAM_MUSIC
    val maxVolume = remember { audio.getStreamMaxVolume(stream).coerceAtLeast(1) }
    var currentVolume by remember { mutableIntStateOf(audio.getStreamVolume(stream)) }
    var lastAudibleVolume by remember { mutableIntStateOf(currentVolume.coerceAtLeast(1)) }
    val muted = currentVolume == 0
    val percent = (currentVolume * 100f / maxVolume).roundToInt()

    fun refresh() {
        currentVolume = audio.getStreamVolume(stream).coerceIn(0, maxVolume)
        if (currentVolume > 0) lastAudibleVolume = currentVolume
    }
    fun setVolume(value: Int) {
        val safe = value.coerceIn(0, maxVolume)
        audio.setStreamVolume(stream, safe, AudioManager.FLAG_SHOW_UI)
        currentVolume = safe
        if (safe > 0) lastAudibleVolume = safe
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text("SOUND CONTROL", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge)
        Text("ফোনের Media volume নিয়ন্ত্রণ", color = Color.LightGray)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                Modifier.fillMaxWidth().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    if (muted) Icons.Default.VolumeMute else if (percent < 50) Icons.Default.VolumeDown else Icons.Default.VolumeUp,
                    contentDescription = "Media volume",
                    tint = Color(0xFFE32636),
                    modifier = Modifier.size(52.dp)
                )
                Text("$percent%", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                Slider(
                    value = currentVolume.toFloat(),
                    onValueChange = { setVolume(it.roundToInt()) },
                    valueRange = 0f..maxVolume.toFloat(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("0%", color = Color.LightGray)
                    Text("100%", color = Color.LightGray)
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { setVolume(currentVolume - 1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("− কমান") }
                    Button(
                        onClick = { setVolume(if (muted) lastAudibleVolume else 0) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))
                    ) { Text(if (muted) "Unmute" else "Mute") }
                    OutlinedButton(
                        onClick = { setVolume(currentVolume + 1) },
                        modifier = Modifier.weight(1f)
                    ) { Text("+ বাড়ান") }
                }
                TextButton(onClick = { refresh() }) { Text("বর্তমান ভলিউম রিফ্রেশ") }
            }
        }
        Text(
            "গুরুত্বপূর্ণ: এটি ফোনের Media stream নিয়ন্ত্রণ করে। Bluetooth স্পিকার ফোনে যুক্ত থাকলে সাধারণত সেটির ভলিউমও বদলাতে পারে। স্পিকার TV-তে যুক্ত থাকলে এই নিয়ন্ত্রণ TV-এর ভলিউম বদলাবে না।",
            color = Color(0xFFFFCC80),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
