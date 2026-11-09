package com.alphaai.devilbreath

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TruecallerConnectCard() {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("TRUECALLER", color = Color(0xFF00A86B), fontWeight = FontWeight.Bold)
            Text(
                "আপনার ফোনে Truecaller খুলুন। Alpha AI থেকে এখন Truecaller অ্যাপ চালু করা যাবে।",
                color = Color(0xFFF5F5F5),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val launch = context.packageManager.getLaunchIntentForPackage("com.truecaller")
                        if (launch != null) {
                            context.startActivity(launch)
                        } else {
                            val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.truecaller"))
                            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.truecaller"))
                            runCatching { context.startActivity(market) }
                                .onFailure { runCatching { context.startActivity(web) } }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00A86B))
                ) {
                    Text("Truecaller খুলুন")
                }
                OutlinedButton(
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://developer.truecaller.com/sign-up"))
                        )
                    }
                ) {
                    Text("Developer setup")
                }
            }
            Text(
                "নোট: এটি Truecaller অ্যাপ খোলার সংযোগ। Truecaller OAuth/নম্বর যাচাই SDK চালু করতে Developer Console-এর Client ID এবং এই অ্যাপের SHA-1 fingerprint লাগবে। Truecaller-এর সম্পূর্ণ caller-ID ডেটাবেসে সরাসরি প্রবেশাধিকার এই সংযোগ দেয় না।",
                color = Color(0xFFB0B0B0),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
