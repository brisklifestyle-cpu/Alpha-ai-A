package com.alphaai.devilbreath

import android.content.Intent
import android.media.AudioManager
import android.provider.ContactsContract
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScreenBg = Color(0xFF080808)
private val ScreenPanel = Color(0xFF171717)
private val ScreenRed = Color(0xFFE32636)
private val ScreenText = Color(0xFFF5F5F5)
private val ScreenMuted = Color(0xFF9A9A9A)

@Composable
private fun ScreenTitle(title: String, subtitle: String) {
    Text(title, color = ScreenRed, style = MaterialTheme.typography.titleLarge)
    Text(subtitle, color = ScreenMuted, fontSize = 12.sp)
}

@Composable
private fun ActionCard(title: String, detail: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = ScreenPanel), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = ScreenText, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = ScreenMuted, fontSize = 12.sp)
            Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = ScreenRed)) { Text(action) }
        }
    }
}

@Composable
private fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
}

@Composable
private fun TvControlScreen() {
    val context = LocalContext.current
    ScreenColumn {
        ScreenTitle("TV & STREAMING CONTROL", "Hisense VIDAA TV • Alif Bedroom TV • ONN Google TV Box")
        ActionCard("Hisense VIDAA TV", "টিভির নিজস্ব নেটওয়ার্ক/রিমোট প্রোটোকল এখানে pair করা হয়নি। Android-এর Wi-Fi settings খুলে একই নেটওয়ার্ক যাচাই করুন।", "Wi-Fi Settings") {
            context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        ActionCard("ONN Google TV Box", "Google TV Remote control-এর জন্য Google TV app বা Android TV Remote Service-এর সমর্থিত pairing প্রয়োজন।", "Google TV App খুলুন") {
            val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.videos")
                ?: context.packageManager.getLaunchIntentForPackage("com.google.android.apps.tv.launcherx")
            if (launch != null) context.startActivity(launch) else context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        }
        ActionCard("Bluetooth Audio Route", "MEGABOOM 3 টিভির সঙ্গে যুক্ত থাকলে ফোনের ভলিউম স্লাইডার টিভি-হোস্টেড অডিও নিয়ন্ত্রণ করে না।", "Bluetooth Settings") {
            context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Text("সংযোগ অবস্থা: এখনো pair/test করা হয়নি। এই স্ক্রিন কোনো ভুয়া Connected status দেখায় না।", color = Color(0xFFFFCC80), fontSize = 12.sp)
    }
}

@Composable
private fun SoundControlScreen() {
    val context = LocalContext.current
    val audio = remember { context.getSystemService(android.content.Context.AUDIO_SERVICE) as AudioManager }
    val prefs = remember { context.getSharedPreferences("alpha_sound_profiles", 0) }
    val profiles = listOf("International Hi-Fi", "Bass", "Vocal", "Boom", "Cinematic", "Broadcast", "Sports", "Night", "Custom EQ")
    var selected by remember { mutableStateOf(prefs.getString("selected_profile", "International Hi-Fi") ?: "International Hi-Fi") }
    var volume by remember { mutableIntStateOf(audio.getStreamVolume(AudioManager.STREAM_MUSIC)) }
    val maxVolume = remember { audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
    ScreenColumn {
        ScreenTitle("SOUND STUDIO", "Alpha AI profile manager • Phone output controls")
        Text("Phone media volume", color = ScreenText)
        Slider(value = volume.toFloat(), onValueChange = { volume = it.toInt() }, valueRange = 0f..maxVolume.toFloat(), onValueChangeFinished = {
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, volume, AudioManager.FLAG_SHOW_UI)
        })
        Text("$volume / $maxVolume", color = ScreenMuted, fontSize = 12.sp)
        Text("Select a sound profile", color = ScreenText, style = MaterialTheme.typography.titleMedium)
        profiles.forEach { profile ->
            Card(onClick = { selected = profile; prefs.edit().putString("selected_profile", profile).apply() },
                colors = CardDefaults.cardColors(containerColor = if (selected == profile) Color(0xFF351116) else ScreenPanel),
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(profile, color = ScreenText)
                    if (selected == profile) Icon(Icons.Default.CheckCircle, null, tint = ScreenRed)
                }
            }
        }
        Text("Selected: $selected", color = ScreenRed)
        Text("সতর্কতা: এই সংস্করণে profile নির্বাচন ফোনে সংরক্ষিত হয়; এটি MEGABOOM 3/TV-তে DSP বা EQ প্রয়োগ করে না। TV-তে speaker যুক্ত থাকলে TV remote দিয়ে volume নিয়ন্ত্রণ করুন।", color = Color(0xFFFFCC80), fontSize = 12.sp)
        OutlinedButton(onClick = { context.startActivity(Intent(android.provider.Settings.ACTION_SOUND_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }) { Text("Android Sound Settings") }
    }
}

@Composable
private fun SocialMediaManagerScreen() {
    val context = LocalContext.current
    val apps = listOf(
        Triple("WhatsApp", "com.whatsapp", "Messages and calls"),
        Triple("Facebook", "com.facebook.katana", "Social network"),
        Triple("Instagram", "com.instagram.android", "Photos and reels"),
        Triple("TikTok", "com.zhiliaoapp.musically", "Short videos"),
        Triple("X", "com.twitter.android", "Posts and updates"),
        Triple("LinkedIn", "com.linkedin.android", "Professional network")
    )
    ScreenColumn {
        ScreenTitle("SOCIAL APP MANAGER", "Installed apps open through their official Android launchers")
        apps.forEach { (name, pkg, detail) ->
            ActionCard(name, detail, "Open $name") {
                val launch = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launch != null) context.startActivity(launch)
                else android.widget.Toast.makeText(context, "$name ইনস্টল করা নেই", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
        Text("অ্যাপ ইনস্টল না থাকলে সেটি খোলা যাবে না। এই স্ক্রিন অন্য অ্যাপের private message পড়া বা নিয়ন্ত্রণ করে না।", color = ScreenMuted, fontSize = 12.sp)
    }
}

@Composable
private fun CallerIntegrationScreen() {
    val context = LocalContext.current
    ScreenColumn {
        ScreenTitle("CALLER ID", "Truecaller and Android dialer shortcuts")
        ActionCard("Truecaller", "Truecaller ইনস্টল থাকলে তার নিজস্ব caller ID ও spam protection ব্যবহার করুন।", "Open Truecaller") {
            val launch = context.packageManager.getLaunchIntentForPackage("com.truecaller")
            if (launch != null) context.startActivity(launch)
            else android.widget.Toast.makeText(context, "Truecaller ইনস্টল করা নেই", android.widget.Toast.LENGTH_SHORT).show()
        }
        ActionCard("Phone Dialer", "Android-এর নিরাপদ dial screen খুলুন।", "Open Dialer") {
            context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Text("Incoming call overlay বা call-log access এই build-এ সক্রিয় নয়; কোনো caller lookup হয়েছে বলে দেখানো হচ্ছে না।", color = Color(0xFFFFCC80), fontSize = 12.sp)
    }
}

@Composable
private fun ContactIntegrationScreen() {
    val context = LocalContext.current
    var picked by remember { mutableStateOf("কোনো contact নির্বাচন করা হয়নি") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uri: Uri? = result.data?.data
        if (uri != null) {
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)
            val name = runCatching {
                context.contentResolver.query(uri, projection, null, null, null)?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
            }.getOrNull()
            picked = name ?: "Contact নির্বাচন হয়েছে"
        }
    }
    ScreenColumn {
        ScreenTitle("PHONE MANAGER • CONTACTS", "Android system contact picker")
        ActionCard("Choose a contact", "Android-এর নিজস্ব contact picker খুলবে; ব্যবহারকারী নিজে contact নির্বাচন করবেন।", "Select Contact") {
            picker.launch(Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI))
        }
        Text(picked, color = ScreenRed)
        ActionCard("Dial a number", "নম্বর ডায়ালারে পাঠান; কল করার আগে আপনি নিজে নিশ্চিত করবেন।", "Open Dialer") {
            context.startActivity(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        Text("Caller ID database বা অন্য অ্যাপের private contacts এই স্ক্রিনে কপি করা হয় না।", color = ScreenMuted, fontSize = 12.sp)
    }
}

@Composable
private fun PhoneCloneScreen() {
    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write("Alpha AI Phone Clone\n")
                    it.write("This is a backup information note only. No contacts, photos, messages, or app-private data were exported by this build.\n")
                }
            }
            android.widget.Toast.makeText(context, "তথ্য ফাইল সংরক্ষিত হয়েছে", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
    ScreenColumn {
        ScreenTitle("PHONE CLONE • BACKUP", "Transparent backup status")
        ActionCard("Create backup note", "এই build শুধু একটি status note export করতে পারে; এটি পূর্ণ ফোন clone বা encrypted device backup নয়।", "Export Status File") {
            exportLauncher.launch("AlphaAI-Backup-Status.txt")
        }
        Text("Implemented: export status note. Not implemented: automatic daily backup, cloud sync, full SMS/call-log backup, app-private data migration, verified full-device restore.", color = Color(0xFFFFCC80), fontSize = 12.sp)
        Text("Android নিরাপত্তার কারণে সাধারণ অ্যাপ অন্য অ্যাপের private data বা সম্পূর্ণ system image কপি করতে পারে না।", color = ScreenMuted, fontSize = 12.sp)
    }
}
