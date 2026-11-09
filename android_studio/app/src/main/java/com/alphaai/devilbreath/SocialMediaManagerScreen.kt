package com.alphaai.devilbreath

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private data class SocialApp(val name: String, val packageName: String, val url: String)
private data class SocialDraft(val id: Long, val title: String, val caption: String, val savedAt: Long)

private fun loadSocialDrafts(context: Context): List<SocialDraft> {
    val raw = context.getSharedPreferences("alpha_social_drafts", Context.MODE_PRIVATE).getString("draft_list", "[]") ?: "[]"
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            SocialDraft(item.optLong("id"), item.optString("title"), item.optString("caption"), item.optLong("savedAt"))
        }.sortedByDescending { it.savedAt }
    }.getOrDefault(emptyList())
}

private fun persistSocialDrafts(context: Context, drafts: List<SocialDraft>) {
    val array = JSONArray()
    drafts.forEach { draft ->
        array.put(JSONObject().put("id", draft.id).put("title", draft.title).put("caption", draft.caption).put("savedAt", draft.savedAt))
    }
    context.getSharedPreferences("alpha_social_drafts", Context.MODE_PRIVATE).edit().putString("draft_list", array.toString()).apply()
}

@Composable
fun SocialMediaManagerScreen() {
    val context = LocalContext.current
    val draftPrefs = remember { context.getSharedPreferences("alpha_social_drafts", Context.MODE_PRIVATE) }
    var caption by remember { mutableStateOf(draftPrefs.getString("latest_caption", "") ?: "") }
    val drafts = remember { mutableStateListOf<SocialDraft>().apply { addAll(loadSocialDrafts(context)) } }
    var note by remember { mutableStateOf("খসড়া লিখে সেভ, কপি বা Share করুন।") }
    val apps = listOf(
        SocialApp("Facebook", "com.facebook.katana", "https://www.facebook.com/"),
        SocialApp("Instagram", "com.instagram.android", "https://www.instagram.com/"),
        SocialApp("YouTube", "com.google.android.youtube", "https://www.youtube.com/"),
        SocialApp("TikTok", "com.zhiliaoapp.musically", "https://www.tiktok.com/"),
        SocialApp("X", "com.twitter.android", "https://x.com/"),
        SocialApp("LinkedIn", "com.linkedin.android", "https://www.linkedin.com/")
    )
    fun openAppDetails(packageName: String) {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("SOCIAL MEDIA MANAGER", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("সোশ্যাল অ্যাপ, পোস্টের খসড়া এবং Android app management এক জায়গায়।", color = Color.White)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("POST COMPOSER", color = Color(0xFFE32636), fontWeight = FontWeight.Bold)
                OutlinedTextField(caption, { caption = it }, Modifier.fillMaxWidth(), minLines = 4, placeholder = { Text("আপনার পোস্টের ক্যাপশন লিখুন...") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(enabled = caption.isNotBlank(), onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Social post", caption))
                        note = "ক্যাপশন কপি হয়েছে। পছন্দের অ্যাপে পেস্ট করুন।"
                    }) { Text("কপি") }
                    OutlinedButton(enabled = caption.isNotBlank(), onClick = {
                        val now = System.currentTimeMillis()
                        val draft = SocialDraft(now, "খসড়া ${drafts.size + 1}", caption.trim(), now)
                        drafts.add(0, draft)
                        persistSocialDrafts(context, drafts)
                        draftPrefs.edit().putString("latest_caption", caption).apply()
                        note = "নতুন খসড়া তালিকায় সেভ হয়েছে। মোট ${drafts.size}টি খসড়া।"
                    }) { Text("Save Draft") }
                    OutlinedButton(enabled = caption.isNotBlank(), onClick = {
                        val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, caption) }
                        context.startActivity(Intent.createChooser(send, "কোন অ্যাপে শেয়ার করবেন?"))
                        note = "Android Share Sheet খোলা হয়েছে।"
                    }) { Text("Share") }
                    OutlinedButton(onClick = { caption = ""; draftPrefs.edit().remove("latest_caption").apply(); note = "খসড়া মুছে দেওয়া হয়েছে।" }) { Text("মুছুন") }
                }
                Text(note, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SAVED DRAFTS", color = Color(0xFFE32636), fontWeight = FontWeight.Bold)
                    Text("${drafts.size}টি", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                }
                if (drafts.isEmpty()) {
                    Text("এখনো কোনো খসড়া সেভ করা হয়নি।", color = Color.LightGray)
                } else {
                    drafts.forEach { draft ->
                        HorizontalDivider(color = Color.DarkGray)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(draft.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(draft.caption, color = Color.LightGray, maxLines = 3, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { caption = draft.caption; note = "খসড়া composer-এ লোড হয়েছে।" }) { Text("খুলুন") }
                                OutlinedButton(onClick = {
                                    drafts.removeAll { it.id == draft.id }
                                    persistSocialDrafts(context, drafts)
                                    note = "নির্বাচিত খসড়া মুছে দেওয়া হয়েছে।"
                                }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(
            onClick = {
                runCatching { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Usage access settings • Screen-time control") }
        Text("SOCIAL APPS", color = Color(0xFFE32636), fontWeight = FontWeight.Bold)
        apps.forEach { app ->
            val installed = remember { context.packageManager.getLaunchIntentForPackage(app.packageName) != null }
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(app.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text(if (installed) "ইনস্টল করা আছে" else "ইনস্টল করা নেই", color = if (installed) Color(0xFF81C784) else Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(onClick = {
                            val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                            val intent = launch ?: Intent(Intent.ACTION_VIEW, Uri.parse(app.url))
                            runCatching { context.startActivity(intent) }
                        }) { Text(if (installed) "খুলুন" else "ওয়েব খুলুন") }
                    }
                    if (installed) {
                        OutlinedButton(onClick = { openAppDetails(app.packageName) }) {
                            Text("App info • Permission • Storage • Notifications")
                        }
                    }
                }
            }
        }
        Text(
            "এই স্ক্রিন থেকে অ্যাপ খোলা, পোস্টের লেখা তৈরি/শেয়ার এবং Android-এর App info খোলা যায়। App info-তে প্রতিটি অ্যাপের permission, storage, notification এবং uninstall অপশন আপনি নিজে পরিচালনা করবেন। Usage access settings থেকে screen-time access আলাদাভাবে অনুমোদন করতে পারবেন। Alpha AI গোপনে অ্যাকাউন্টে প্রবেশ, পোস্ট প্রকাশ, ইনবক্স পড়া বা অন্য অ্যাপ uninstall করে না। সরাসরি account publishing/API automation-এর জন্য সংশ্লিষ্ট প্ল্যাটফর্মের অনুমোদিত API ও OAuth access আলাদাভাবে লাগবে।",
            color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall
        )
    }
}
