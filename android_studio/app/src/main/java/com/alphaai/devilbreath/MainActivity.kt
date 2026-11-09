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
                listOf("Home" to Icons.Default.Home, "AI Chat" to Icons.Default.Chat, "Social" to Icons.Default.Share, "Voice" to Icons.Default.Mic, "Sound" to Icons.Default.VolumeUp, "Clone" to Icons.Default.Security, "Caller" to Icons.Default.Call, "Contacts" to Icons.Default.Contacts, "TV" to Icons.Default.Tv, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
                    NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Icon(icon, null) }, label = { Text(name, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Red, selectedTextColor = Red, indicatorColor = Color(0xFF351116), unselectedIconColor = Muted, unselectedTextColor = Muted))
                }
            }
        }) { padding ->
            when (page) {
                "Home" -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("ALPHA AI", color = White, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("DEVIL BREATH", color = Red, fontSize = 12.sp, letterSpacing = 2.sp)
                    Surface(color = Color(0xFF1A0B0D), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("HELLO, DEVIL", color = Red, fontWeight = FontWeight.Bold)
                            Text("আপনার ব্যক্তিগত AI কমান্ড সেন্টার", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(if (apiKey.isBlank()) "Offline Helper সক্রিয় • Gemini Key যোগ করা হয়নি" else "Gemini API Key সংরক্ষিত", color = Muted)
                            Button(onClick = { page = "AI Chat" }, colors = ButtonDefaults.buttonColors(containerColor = Red)) { Icon(Icons.Default.Chat, null); Spacer(Modifier.width(8.dp)); Text("AI CHAT শুরু করুন") }
                        }
                    }
                    TruecallerConnectCard()
                    Text("PHASE 1", color = Red, fontWeight = FontWeight.Bold)
                    listOf("Gemini AI Chat" to "ফ্রি tier quota সাপেক্ষ", "Voice input & spoken reply" to "ডিভাইসের speech service নির্ভর", "Offline Helper" to "সীমিত স্থানীয় সহায়তা").forEach {
                        Surface(color = Panel, shape = RoundedCornerShape(14.dp)) { Column(Modifier.padding(16.dp)) { Text(it.first, color = White, fontWeight = FontWeight.SemiBold); Text(it.second, color = Muted, fontSize = 12.sp) } }
                    }
                }
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
