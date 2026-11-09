package com.alphaai.devilbreath

import android.content.Intent
import android.net.Uri
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class SocialApp(val name: String, val packageName: String, val url: String)

@Composable
fun SocialMediaManagerScreen() {
    val context = LocalContext.current
    var caption by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("পোস্টের লেখা এখানে তৈরি/সম্পাদনা করুন, তারপর কপি করে পছন্দের অ্যাপে পোস্ট করুন।") }
    val apps = listOf(
        SocialApp("Facebook", "com.facebook.katana", "https://www.facebook.com/"),
        SocialApp("Instagram", "com.instagram.android", "https://www.instagram.com/"),
        SocialApp("YouTube", "com.google.android.youtube", "https://www.youtube.com/"),
        SocialApp("TikTok", "com.zhiliaoapp.musically", "https://www.tiktok.com/"),
        SocialApp("X", "com.twitter.android", "https://x.com/"),
        SocialApp("LinkedIn", "com.linkedin.android", "https://www.linkedin.com/")
    )
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SOCIAL MEDIA MANAGER", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("সোশ্যাল অ্যাপ খুলুন, পোস্টের খসড়া লিখুন এবং কপি করে শেয়ার করুন।", color = Color.White)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("POST COMPOSER", color = Color(0xFFE32636), fontWeight = FontWeight.Bold)
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    placeholder = { Text("আপনার পোস্টের ক্যাপশন লিখুন...") }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Social post", caption))
                        note = if (caption.isBlank()) "আগে ক্যাপশন লিখুন।" else "ক্যাপশন কপি হয়েছে। এখন সোশ্যাল অ্যাপে পেস্ট করুন।"
                    }) { Text("কপি করুন") }
                    OutlinedButton(onClick = { caption = ""; note = "খসড়া মুছে দেওয়া হয়েছে।" }) { Text("মুছুন") }
                }
                Text(note, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall)
            }
        }
        Text("CONNECTED APPS • OPEN", color = Color(0xFFE32636), fontWeight = FontWeight.Bold)
        apps.forEach { app ->
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("অ্যাপ থাকলে খুলবে, না থাকলে ওয়েবসাইট", color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = {
                        val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                        val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse(app.url))
                        runCatching { context.startActivity(intent) }
                    }) { Text("খুলুন") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "গুরুত্বপূর্ণ: এই সংস্করণে অ্যাপ খোলা ও ক্যাপশন কপি করা যায়। Facebook/Instagram/TikTok/YouTube-এ সরাসরি পোস্ট, ইনবক্স পড়া বা অ্যাকাউন্টের ডেটা নিয়ন্ত্রণ এখনো যুক্ত নয়। সেগুলোর জন্য প্রতিটি প্ল্যাটফর্মের অনুমোদিত API, Developer App এবং আপনার OAuth অনুমতি প্রয়োজন।",
            color = Color(0xFFAAAAAA),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
