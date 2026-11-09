package com.alphaai.devilbreath

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors

private val Bg = Color(0xFF080808)
private val Panel = Color(0xFF171717)
private val Red = Color(0xFFE32636)
private val White = Color(0xFFF5F5F5)
private val Muted = Color(0xFF9A9A9A)

class MainActivity : ComponentActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale("bn", "BD") }
        setContent { AlphaApp(
            readKey = { securePrefs().getString("gemini_key", "") ?: "" },
            saveKey = { securePrefs().edit().putString("gemini_key", it.trim()).apply() },
            askAI = { key, prompt, done -> executor.execute { val answer = GeminiClient.ask(key, prompt); runOnUiThread { done(answer) } } },
            speak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "alpha_reply") }
        ) }
    }
    private fun securePrefs() = EncryptedSharedPreferences.create(
        this, "alpha_secure_prefs",
        MasterKey.Builder(this).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    override fun onDestroy() { tts?.stop(); tts?.shutdown(); executor.shutdown(); super.onDestroy() }
}

private object GeminiClient {
    fun ask(apiKey: String, prompt: String): String {
        if (apiKey.isBlank()) return "API Key দেওয়া নেই। Settings-এ Gemini API Key যোগ করুন, অথবা Offline Helper ব্যবহার করুন।"
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            val body = JSONObject().put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val raw = stream.bufferedReader().use { it.readText() }
            if (code !in 200..299) {
                val message = runCatching { JSONObject(raw).getJSONObject("error").getString("message") }.getOrDefault("HTTP $code")
                "Gemini API সমস্যা ($code): $message"
            } else {
                val obj = JSONObject(raw)
                obj.getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
            }
        } catch (e: Exception) {
            "সংযোগ করা যায়নি: ${e.localizedMessage ?: "অজানা সমস্যা"}"
        } finally { connection?.disconnect() }
    }
}

private data class ChatLine(val fromUser: Boolean, val text: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlphaApp(readKey: () -> String, saveKey: (String) -> Unit, askAI: (String, String, (String) -> Unit) -> Unit, speak: (String) -> Unit) {
    var page by remember { mutableStateOf("Home") }
    var apiKey by remember { mutableStateOf(readKey()) }
    var draftKey by remember { mutableStateOf(apiKey) }
    var prompt by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var online by remember { mutableStateOf(true) }
    val chat = remember { mutableStateListOf(ChatLine(false, "স্বাগতম! আমি Alpha AI। Gemini API Key দিলে অনলাইন AI ব্যবহার করতে পারবেন; না দিলে Offline Helper চালু থাকবে।")) }
    val context = LocalContext.current
    var voiceStatus by remember { mutableStateOf("মাইক্রোফোন চাপুন, তারপর “Hey Devil” বা “হেই ডেভিল” বলে নির্দেশ দিন।") }
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.trim()
        if (!spoken.isNullOrBlank()) {
            val wakePattern = Regex("(?i)^\\s*(hey\\s+devil|হেই\\s+ডেভিল)[,\\s।!]*")
            val command = spoken.replaceFirst(wakePattern, "").trim()
            if (command.isBlank() && wakePattern.containsMatchIn(spoken)) {
                prompt = ""
                voiceStatus = "হ্যাঁ, বলুন… এবার আপনার নির্দেশটি বলুন।"
            } else {
                prompt = command.ifBlank { spoken }
                voiceStatus = if (command != spoken) "Wake phrase শনাক্ত হয়েছে। নির্দেশটি পাঠানোর জন্য Send চাপুন।" else "কথা শনাক্ত হয়েছে। পাঠানোর জন্য Send চাপুন।"
            }
        } else {
            voiceStatus = "কোনো কথা শনাক্ত হয়নি। আবার মাইক্রোফোন চাপুন।"
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(primary = Red, background = Bg, surface = Panel, onSurface = White)) {
        Scaffold(containerColor = Bg, bottomBar = {
            NavigationBar(containerColor = Color(0xFF101010)) {
                listOf(
                    Triple("Home", Icons.Default.Home, "Home"),
                    Triple("Devices", Icons.Default.Tv, "Devices"),
                    Triple("Tools", Icons.Default.Build, "Sound"),
                    Triple("More", Icons.Default.MoreHoriz, "Settings")
                ).forEach { (label, icon, destination) ->
                    NavigationBarItem(
                        selected = (if (page == "AI Chat" || page == "Voice") "Assistant" else page) == label,
                        onClick = { page = destination },
                        icon = { Icon(icon, null) },
                        label = { Text(label, fontSize = 11.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Red, selectedTextColor = Red,
                            indicatorColor = Color(0xFF351116),
                            unselectedIconColor = Muted, unselectedTextColor = Muted
                        )
                    )
                }
            }
        }) { padding ->
            when (page) {
                "Home" -> DashboardHome(apiKey.isNotBlank()) { page = it }
                "Devices" -> DashboardDevices { page = it }
                "TV" -> TvControlScreen()
                "Sound" -> SoundControlScreen()
                "Social" -> SocialMediaManagerScreen()
                "Caller" -> CallerIntegrationScreen()
                "Contacts" -> ContactIntegrationScreen()
                "Clone" -> PhoneCloneScreen()
                "AI Chat", "Voice" -> Column(Modifier.fillMaxSize().padding(padding).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(if (page == "Voice") "HEY DEVIL • VOICE" else "ALPHA AI CHAT", color = Red, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    if (page == "Voice") {
                        Text(voiceStatus, color = Muted, fontSize = 12.sp)
                        Text("অফলাইন লোকাল কমান্ড: ফোন/ফোনে যুক্ত অডিওর মিডিয়া ভলিউম। TV-তে যুক্ত স্পিকারের ভলিউম TV নিয়ন্ত্রণের ওপর নির্ভর করে।", color = Muted, fontSize = 11.sp)
                    }
                    LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(chat) { line ->
                            Surface(color = if (line.fromUser) Color(0xFF351116) else Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                                Text(line.text, color = White, modifier = Modifier.padding(14.dp))
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(prompt, { prompt = it }, modifier = Modifier.weight(1f), placeholder = { Text("বাংলায় লিখুন...") }, maxLines = 3)
                        IconButton(onClick = {
                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
                                putExtra(RecognizerIntent.EXTRA_PROMPT, "হেই ডেভিল বলুন, তারপর আপনার নির্দেশ দিন...")
                            }
                            runCatching { voiceLauncher.launch(intent) }
                        }) { Icon(Icons.Default.Mic, "Voice", tint = Red) }
                        IconButton(enabled = prompt.isNotBlank() && !busy, onClick = {
                            val q = prompt.trim(); prompt = ""; chat.add(ChatLine(true, q)); busy = true
                            val localAnswer = performLocalCommand(context, q)
                            if (localAnswer != null) {
                                chat.add(ChatLine(false, localAnswer)); busy = false; speak(localAnswer)
                            } else if (online && apiKey.isNotBlank()) {
                                askAI(apiKey, q) { answer -> chat.add(ChatLine(false, answer)); busy = false; speak(answer) }
                            } else {
                                val answer = offlineAnswer(q); chat.add(ChatLine(false, answer)); busy = false; speak(answer)
                            }
                        }) { Icon(Icons.Default.Send, "Send", tint = if (busy) Muted else Red) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("AI Mode", color = Muted)
                        Spacer(Modifier.width(8.dp))
                        FilterChip(selected = online, onClick = { online = true }, label = { Text("Gemini Online") })
                        Spacer(Modifier.width(6.dp))
                        FilterChip(selected = !online, onClick = { online = false }, label = { Text("Offline Helper") })
                    }
                    if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Red)
                }
                else -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("SETTINGS", color = Red, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Gemini API Key", color = White, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(draftKey, { draftKey = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), placeholder = { Text("Google AI Studio API Key") })
                    Button(onClick = { apiKey = draftKey.trim(); saveKey(apiKey) }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Text("নিরাপদে সংরক্ষণ করুন") }
                    Text(if (apiKey.isBlank()) "এখনো Key সংরক্ষণ করা হয়নি" else "Key সংরক্ষিত আছে (মান দেখানো হচ্ছে না)", color = Muted)
                    Text("API Key এই GitHub repository-তে রাখা হয় না। ব্যক্তিগত পরীক্ষার APK-তে ব্যবহার করুন; বহু-ব্যবহারকারীর অ্যাপের জন্য backend proxy উত্তম।", color = Muted, fontSize = 12.sp)
                    Text("Gemini free tier-এর সীমা Google নির্ধারণ করে।", color = Muted, fontSize = 12.sp)
                }
            }
        }
    }
}

private fun performLocalCommand(context: Context, q: String): String? {
    val s = q.lowercase()
    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val stream = AudioManager.STREAM_MUSIC
    fun openIntent(intent: Intent, success: String): String {
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            success
        } catch (_: Exception) {
            "এই কাজটি করার উপযুক্ত অ্যাপ বা Android screen পাওয়া যায়নি।"
        }
    }
    return when {
        listOf("ইউটিউব খোলো", "ইউটিউব চালু", "open youtube", "launch youtube").any { it in s } -> {
            val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (launch != null) openIntent(launch, "YouTube খোলা হচ্ছে।")
            else openIntent(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.youtube.com")), "YouTube খোলা হচ্ছে।")
        }
        listOf("ক্যামেরা খোলো", "ক্যামেরা চালু", "open camera", "launch camera").any { it in s } ->
            openIntent(Intent("android.media.action.IMAGE_CAPTURE"), "ক্যামেরা খোলা হচ্ছে।")
        listOf("ফোন খোলো", "কল করার অ্যাপ", "open phone", "open dialer").any { it in s } ->
            openIntent(Intent(Intent.ACTION_DIAL), "Phone dialer খোলা হচ্ছে।")
        listOf("ওয়াইফাই সেটিংস", "ওয়াইফাই সেটিংস", "wifi settings", "open wifi").any { it in s } ->
            openIntent(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS), "Wi-Fi settings খোলা হচ্ছে।")
        listOf("ব্লুটুথ সেটিংস", "bluetooth settings", "open bluetooth").any { it in s } ->
            openIntent(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS), "Bluetooth settings খোলা হচ্ছে।")
        listOf("সাউন্ড সেটিংস", "শব্দের সেটিংস", "sound settings").any { it in s } ->
            openIntent(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS), "Sound settings খোলা হচ্ছে।")
        listOf("সেটিংস খোলো", "সেটিংস চালু", "open settings", "launch settings").any { it in s } ->
            openIntent(Intent(android.provider.Settings.ACTION_SETTINGS), "Android Settings খোলা হচ্ছে।")
        listOf("ভলিউম বাড়াও", "ভলিউম বাড়াও", "শব্দ বাড়াও", "শব্দ বাড়াও", "volume up", "increase volume", "বাড়িয়ে দাও", "বাড়িয়ে দাও").any { it in s } -> {
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "ফোনের মিডিয়া ভলিউম বাড়ানো হয়েছে।"
        }
        listOf("ভলিউম কমাও", "শব্দ কমাও", "volume down", "lower volume", "কমানো").any { it in s } -> {
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "ফোনের মিডিয়া ভলিউম কমানো হয়েছে।"
        }
        listOf("মিউট", "নিঃশব্দ", "mute").any { it in s } -> {
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
            "ফোনের মিডিয়া অডিও মিউট করা হয়েছে।"
        }
        listOf("আনমিউট", "unmute", "শব্দ চালু").any { it in s } -> {
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
            "ফোনের মিডিয়া অডিও আবার চালু করা হয়েছে।"
        }
        else -> null
    }
}

private fun offlineAnswer(q: String): String {
    val s = q.lowercase()
    return when {
        "সময়" in s || "time" in s -> "আমি অফলাইন মোডে বর্তমান সময় নির্ভুলভাবে পড়তে পারছি না।"
        "হ্যালো" in s || "hello" in s || "হাই" in s -> "হ্যালো! আমি Alpha AI Offline Helper। কীভাবে সাহায্য করতে পারি?"
        "সাহায্য" in s || "help" in s -> "অফলাইন মোডে সাধারণ শুভেচ্ছা ও অ্যাপ-সংক্রান্ত সীমিত সহায়তা দিতে পারি। পূর্ণ AI উত্তরের জন্য Gemini Online চালু করুন।"
        else -> "এটি Offline Helper-এর সীমিত সংস্করণ। ইন্টারনেট চালু করে Gemini Online নির্বাচন করুন, অথবা Settings-এ API Key যোগ করুন।"
    }
}


private data class DashboardTile(val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val destination: String)

@Composable
private fun DashboardHome(hasApiKey: Boolean, onNavigate: (String) -> Unit) {
    val tiles = listOf(
        DashboardTile("AI Assistant", "Devil Breath", Icons.Default.SmartToy, "AI Chat"),
        DashboardTile("Sound Studio", "International EQ", Icons.Default.Equalizer, "Sound"),
        DashboardTile("TV Control", "Hisense • ONN", Icons.Default.Tv, "TV"),
        DashboardTile("MEGABOOM 3", "Bluetooth Audio", Icons.Default.Speaker, "Sound"),
        DashboardTile("Phone Manager", "Apps & Contacts", Icons.Default.PhoneAndroid, "Contacts"),
        DashboardTile("Backup & Clone", "Backup & Restore", Icons.Default.Security, "Clone"),
        DashboardTile("Social Manager", "Social Apps", Icons.Default.Forum, "Social"),
        DashboardTile("Connect Center", "Devices & Services", Icons.Default.Link, "Devices")
    )
    val neon = listOf(
        Color(0xFF16C8FF), Color(0xFFE52BFF), Color(0xFFFF3030), Color(0xFF9BFF32),
        Color(0xFFB45CFF), Color(0xFF00D8C8), Color(0xFFFFB52E), Color(0xFF26C6DA)
    )
    Box(Modifier.fillMaxSize()) {
        EarthSpaceBackdrop()
        LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.ChangeHistory, null, tint = Red, modifier = Modifier.size(62.dp))
                    Text("Alpha AI", color = White, fontSize = 29.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Devil Breath", color = Red, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("YOUR SMART AI CONTROL CENTER", color = White.copy(alpha = 0.78f), fontSize = 9.sp, letterSpacing = 1.3.sp)
                }
            }
            item {
                Surface(
                    onClick = { onNavigate("Voice") },
                    color = Color(0xD9090508),
                    shape = RoundedCornerShape(28.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF9E1725)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.GraphicEq, null, tint = Red, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Hey Devil", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("Tap to Speak", color = White.copy(alpha = 0.72f), fontSize = 11.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(if (hasApiKey) "ONLINE" else "READY", color = Color(0xFF00E676), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            item { Text("SMART CONTROL MODULES", color = White, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.1.sp) }
            items(tiles.chunked(4)) { group ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    group.forEach { tile ->
                        val tint = neon[tiles.indexOf(tile).coerceIn(0, neon.lastIndex)]
                        Surface(
                            onClick = { onNavigate(tile.destination) },
                            modifier = Modifier.weight(1f).height(104.dp),
                            color = Color(0xD9090A10),
                            shape = RoundedCornerShape(15.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, tint.copy(alpha = 0.62f))
                        ) {
                            Column(Modifier.fillMaxSize().padding(horizontal = 3.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Icon(tile.icon, null, tint = tint, modifier = Modifier.size(29.dp))
                                Text(tile.title, color = White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, lineHeight = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EarthSpaceBackdrop() {
    Canvas(Modifier.fillMaxSize()) {
        drawRect(Color.Black)
        val center = Offset(size.width * 0.5f, size.height * 1.18f)
        val radius = size.width * 0.82f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF281017), Color(0xFF10090D), Color(0xFF030305)),
                center = Offset(center.x, center.y - radius * 0.68f),
                radius = radius * 1.65f
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color(0xFF6F101D).copy(alpha = 0.55f),
            radius = radius,
            center = center,
            style = Stroke(width = 7f)
        )
        drawCircle(
            color = Color(0xFFFF2638).copy(alpha = 0.75f),
            radius = radius * 0.985f,
            center = center,
            style = Stroke(width = 2.5f)
        )
        drawCircle(
            color = Color(0xFFFF2638).copy(alpha = 0.16f),
            radius = radius * 1.035f,
            center = center,
            style = Stroke(width = 13f)
        )
        val stars = listOf(
            Offset(size.width*.10f,size.height*.14f), Offset(size.width*.82f,size.height*.11f),
            Offset(size.width*.24f,size.height*.42f), Offset(size.width*.91f,size.height*.49f),
            Offset(size.width*.72f,size.height*.32f), Offset(size.width*.06f,size.height*.63f),
            Offset(size.width*.91f,size.height*.72f), Offset(size.width*.48f,size.height*.20f)
        )
        stars.forEach { drawCircle(Color(0xFFFF6673).copy(alpha=.55f), radius=1.5f, center=it) }
    }
}
@Composable
private fun DashboardDevices(onNavigate: (String) -> Unit) {
    val devices = listOf(
        DashboardTile("Hisense VIDAA TV", "Alif Bedroom TV • 43A6Q", Icons.Default.Tv, "TV"),
        DashboardTile("ONN Google TV Box", "HDMI-connected streaming box", Icons.Default.Cast, "TV"),
        DashboardTile("MEGABOOM 3", "Bluetooth audio device", Icons.Default.Speaker, "Sound"),
        DashboardTile("Sound Studio", "Phone volume & saved sound profiles", Icons.Default.Equalizer, "Sound")
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("DEVICE CENTER", color = Red, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, fontSize = 18.sp)
        Text("আপনার ডিভাইসগুলো আলাদা করে নির্বাচন করুন", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        devices.forEach { device ->
            Surface(onClick = { onNavigate(device.destination) }, color = Panel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(device.icon, null, tint = Red, modifier = Modifier.size(28.dp))
                    Column(Modifier.weight(1f)) {
                        Text(device.title, color = White, fontWeight = FontWeight.SemiBold)
                        Text(device.subtitle, color = Muted, fontSize = 11.sp)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = Muted)
                }
            }
        }
        Text("TV remote pairing এবং MEGABOOM 3-এর TV-hosted volume control এখনো protocol integration-এর অপেক্ষায়। এখানে কোনো সংযোগ সফল হয়েছে বলে দেখানো হচ্ছে না।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
    }
}
