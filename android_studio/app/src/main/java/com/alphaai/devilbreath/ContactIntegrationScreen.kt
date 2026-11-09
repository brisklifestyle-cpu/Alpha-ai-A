package com.alphaai.devilbreath

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

private data class ContactPhone(val name: String, val number: String)

@Composable
fun ContactIntegrationScreen() {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var query by remember { mutableStateOf("") }
    var contacts by remember { mutableStateOf<List<ContactPhone>>(emptyList()) }
    var message by remember { mutableStateOf("অনুমতি দিলে ফোনের Contacts থেকে নাম/নম্বর খুঁজতে পারবেন।") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        message = if (granted) "Contacts access অনুমোদিত।" else "অনুমতি দেওয়া হয়নি; Contact search চালু করা যাবে না।"
    }

    fun searchContacts(term: String) {
        if (!hasPermission) return
        val found = mutableListOf<ContactPhone>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = if (term.isBlank()) null else
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
        val args = if (term.isBlank()) null else arrayOf("%$term%", "%$term%")
        runCatching {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection, selection, args,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && found.size < 100) {
                    val name = cursor.getString(nameIndex).orEmpty()
                    val number = cursor.getString(numberIndex).orEmpty()
                    if (number.isNotBlank()) found.add(ContactPhone(name, number))
                }
            }
        }.onFailure { message = "Contacts পড়া যায়নি: ${it.localizedMessage ?: "সমস্যা"}" }
        contacts = found.distinctBy { it.number }
        message = if (found.isEmpty()) "কোনো মিল পাওয়া যায়নি।" else "${contacts.size}টি নম্বর পাওয়া গেছে।"
    }

    LaunchedEffect(hasPermission) { if (hasPermission) searchContacts(query) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("CONTACTS & CALL", color = Color(0xFF00A86B), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Contacts থেকে নম্বর খুঁজে Phone Dialer-এ কল প্রস্তুত করুন।", color = Color.LightGray)
        if (!hasPermission) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) }) {
                Text("Contacts Permission দিন")
            }
        } else {
            OutlinedTextField(
                value = query, onValueChange = { query = it; searchContacts(it) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("নাম বা ফোন নম্বর খুঁজুন") }
            )
            Text(message, color = Color(0xFFAAAAAA), style = MaterialTheme.typography.bodySmall)
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(contacts) { contact ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF171717))) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(contact.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(contact.number, color = Color.LightGray)
                            }
                            OutlinedButton(onClick = {
                                runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(contact.number)}"))) }
                            }) { Text("Dial") }
                        }
                    }
                }
            }
        }
        Text(
            "গোপনীয়তা: Contacts permission শুধু আপনার অনুরোধে চাওয়া হয়। Alpha AI কোনো call log পড়ে না, কল রেকর্ড করে না বা নিজে থেকে কল করে না; Dial চাপলে Android Phone app খোলে এবং কলটি আপনি নিশ্চিত করবেন।",
            color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall
        )
    }
}
