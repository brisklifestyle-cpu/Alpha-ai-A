package com.alphaai.devilbreath

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CallerIntegrationScreen() {
    val context = LocalContext.current
    val packageName = "com.truecaller"
    val installed = context.packageManager.getLaunchIntentForPackage(packageName) != null
    fun openTruecaller() {
        val launch = context.packageManager.getLaunchIntentForPackage(packageName)
        if (launch != null) {
            runCatching { context.startActivity(launch) }
        } else {
            val store = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.truecaller"))
            runCatching { context.startActivity(store) }
        }
    }
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("CALLER INTEGRATION", color = Color(0xFF00A86B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717)), shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TRUECALLER", color = Color(0xFF00A86B), fontWeight = FontWeight.Bold)
                Text(if (installed) "Truecaller এই ফোনে ইনস্টল করা আছে।" else "Truecaller এই ফোনে পাওয়া যায়নি।", color = Color.White)
                Button(onClick = { openTruecaller() }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))) {
                    Text(if (installed) "Truecaller খুলুন" else "Truecaller ইনস্টল করুন")
                }
                if (installed) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
                        }
                    }) { Text("Truecaller App Info") }
                }
            }
        }
        Text("Caller ID ও Spam Protection চালু করার ধাপ", color = Color.White, fontWeight = FontWeight.SemiBold)
        Text("১. Truecaller খুলুন এবং নিজের অ্যাকাউন্ট সেটআপ সম্পন্ন করুন।\n২. Truecaller-এর ভেতরে Caller ID ও Spam Protection অপশন খুঁজে চালু করুন।\n৩. Android চাইলে প্রয়োজনীয় default caller ID / spam app বা সংশ্লিষ্ট অনুমতি নিজে নিশ্চিত করুন।", color = Color.LightGray)
        Text(
            "Alpha AI এখানে Truecaller খোলার ও সেটআপে যাওয়ার shortcut দেয়। Caller ID database, live incoming-call overlay বা spam blocking Alpha AI নিজে করে না; সেগুলো Truecaller-এর নিজস্ব সেবা ও Android-এর অনুমতির ওপর নির্ভরশীল। কোনো call log, contact বা phone permission এই ধাপে নেওয়া হচ্ছে না।",
            color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall
        )
    }
}
