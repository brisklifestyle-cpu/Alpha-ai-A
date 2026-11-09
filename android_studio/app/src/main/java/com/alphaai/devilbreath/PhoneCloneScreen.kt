package com.alphaai.devilbreath

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.DocumentsContract
import android.provider.OpenableColumns
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val CloneRed = Color(0xFFE32636)
private val ClonePanel = Color(0xFF171717)

@Composable
fun PhoneCloneScreen() {
    val context = LocalContext.current
    val selectedFiles = remember { mutableStateListOf<Uri>() }
    var passphrase by remember { mutableStateOf("") }
    var includeContacts by remember { mutableStateOf(false) }
    var hasContactsPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
    }
    var status by remember { mutableStateOf("ফাইল বেছে নিন, একটি শক্তিশালী পাসফ্রেজ দিন, তারপর encrypted backup তৈরি করুন।") }
    var restoreSource by remember { mutableStateOf<Uri?>(null) }
    var busy by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        selectedFiles.clear()
        selectedFiles.addAll(uris)
        status = "${uris.size}টি ফাইল নির্বাচন করা হয়েছে।"
    }
    val contactsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasContactsPermission = granted
        if (!granted) includeContacts = false
        status = if (granted) "Contacts permission পাওয়া গেছে।" else "Contacts permission দেওয়া হয়নি।"
    }
    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { target ->
        if (target != null) {
            busy = true
            status = "Encrypted backup তৈরি হচ্ছে…"
            Thread {
                val result = runCatching { writeEncryptedBackup(context, target, selectedFiles.toList(), passphrase, includeContacts) }
                Handler(Looper.getMainLooper()).post {
                    busy = false
                    status = result.fold({ "Backup সফল হয়েছে। ফাইলটি নির্বাচিত লোকাল বা Cloud location-এ সেভ হয়েছে।" }, { "Backup ব্যর্থ: ${it.localizedMessage ?: "অজানা সমস্যা"}" })
                }
            }.start()
        }
    }
    val archivePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { source ->
        if (source != null) {
            restoreSource = source
            status = "Backup archive নির্বাচন করা হয়েছে। Restore destination folder বেছে নিন।"
        }
    }
    val destinationPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { folder ->
        val source = restoreSource
        if (folder != null && source != null) {
            busy = true
            status = "Backup যাচাই ও restore হচ্ছে…"
            Thread {
                val result = runCatching { restoreEncryptedBackup(context, source, folder, passphrase) }
                Handler(Looper.getMainLooper()).post {
                    busy = false
                    status = result.fold({ "Restore সম্পন্ন: ${it}টি ফাইল destination folder-এ লেখা হয়েছে। Contacts JSON থাকলে সেটিও সেখানে থাকবে; Contacts app-এ import আলাদাভাবে করতে হবে।" }, { "Restore ব্যর্থ: পাসফ্রেজ ভুল, archive নষ্ট, অথবা destination অনুমতি নেই। ${it.localizedMessage ?: ""}" })
                    restoreSource = null
                }
            }.start()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("FULL PHONE CLONE", color = CloneRed, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("অনুমোদিত ডেটার encrypted backup ও restore", color = Color.LightGray)
        Card(colors = CardDefaults.cardColors(containerColor = ClonePanel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("১ • Backup data নির্বাচন", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("Photos, videos, documents বা অন্য ফাইল Android picker থেকে বেছে নিন।", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { filePicker.launch(arrayOf("*/*")) }, enabled = !busy) { Text("ফাইল নির্বাচন করুন") }
                Text("নির্বাচিত ফাইল: ${selectedFiles.size}টি", color = Color.LightGray)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = includeContacts, onCheckedChange = { checked ->
                        if (checked && !hasContactsPermission) contactsPermission.launch(Manifest.permission.READ_CONTACTS)
                        else includeContacts = checked
                    }, enabled = !busy)
                    Text("Contacts-ও backup করুন", color = Color.White)
                }
                Text("SMS, Call Log, Calendar এবং অন্য অ্যাপের private data এই প্রথম সংস্করণে পড়া/কপি করা হয় না।", color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall)
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = ClonePanel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("২ • Encryption", color = Color.White, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = passphrase, onValueChange = { passphrase = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Backup passphrase (কমপক্ষে ৮ অক্ষর)") },
                    visualTransformation = PasswordVisualTransformation()
                )
                Text("AES-256-GCM encryption ও PBKDF2 key derivation ব্যবহার করা হয়। Restore-এর সময় একই passphrase লাগবে। এটি ভুলে গেলে backup খোলা যাবে না।", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                Button(
                    onClick = { createBackup.launch("AlphaAI-PhoneClone.aiclon") },
                    enabled = !busy && passphrase.length >= 8 && (selectedFiles.isNotEmpty() || includeContacts),
                    colors = ButtonDefaults.buttonColors(containerColor = CloneRed)
                ) { Text("Encrypted Backup তৈরি করুন") }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = ClonePanel)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("৩ • Restore", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("আগের .aiclon archive বেছে নিয়ে destination folder নির্বাচন করুন।", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { archivePicker.launch(arrayOf("*/*")) }, enabled = !busy && passphrase.length >= 8) { Text("Backup Restore করুন") }
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = CloneRed)
        Text(status, color = Color(0xFFBDBDBD), style = MaterialTheme.typography.bodySmall)
        Text("Cloud: Create Document picker-এ Google Drive বা অন্য সংযুক্ত provider দেখা গেলে সেখানে backup সেভ করা যাবে। এই সংস্করণে auto cloud sync নেই।", color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
    }
}

private fun deriveCloneKey(passphrase: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(passphrase.toCharArray(), salt, 210_000, 256)
    return try {
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        SecretKeySpec(bytes, "AES")
    } finally {
        spec.clearPassword()
    }
}

private fun writeEncryptedBackup(context: Context, target: Uri, files: List<Uri>, passphrase: String, includeContacts: Boolean) {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
        init(Cipher.ENCRYPT_MODE, deriveCloneKey(passphrase, salt), GCMParameterSpec(128, iv))
    }
    val raw = context.contentResolver.openOutputStream(target, "wt") ?: error("Backup destination খোলা যায়নি")
    raw.use { output ->
        output.write("ALPHAIC1".toByteArray(Charsets.US_ASCII))
        output.write(salt)
        output.write(iv)
        val encrypted = CipherOutputStream(BufferedOutputStream(output), cipher)
        ZipOutputStream(encrypted).use { zip ->
                files.forEachIndexed { index, uri ->
                    val name = queryDisplayName(context, uri).replace(Regex("[/\\\\\\\\:*?\"<>|]"), "_").ifBlank { "file_$index" }
                    zip.putNextEntry(ZipEntry("files/${index}_$name"))
                    context.contentResolver.openInputStream(uri)?.use { input -> input.copyTo(zip) } ?: error("ফাইল পড়া যায়নি: $name")
                    zip.closeEntry()
                }
                if (includeContacts) {
                    val contacts = JSONArray()
                    val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
                    context.contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, null, null, null)?.use { cursor ->
                        val ni = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val pi = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        while (cursor.moveToNext()) contacts.put(JSONObject().put("name", cursor.getString(ni).orEmpty()).put("number", cursor.getString(pi).orEmpty()))
                    }
                    zip.putNextEntry(ZipEntry("contacts/contacts.json"))
                    zip.write(contacts.toString().toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }
                zip.finish()
        }
    }
}

private fun restoreEncryptedBackup(context: Context, source: Uri, destinationTree: Uri, passphrase: String): Int {
    val input = BufferedInputStream(context.contentResolver.openInputStream(source) ?: error("Archive পড়া যায়নি"))
    input.use { raw ->
        val magic = ByteArray(8)
        if (raw.read(magic) != 8 || String(magic, Charsets.US_ASCII) != "ALPHAIC1") error("এটি Alpha AI backup archive নয়")
        val salt = ByteArray(16)
        val iv = ByteArray(12)
        if (raw.read(salt) != 16 || raw.read(iv) != 12) error("Backup header অসম্পূর্ণ")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, deriveCloneKey(passphrase, salt), GCMParameterSpec(128, iv))
        }
        var restored = 0
        val decrypted = CipherInputStream(raw, cipher)
        ZipInputStream(decrypted).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (!entry.isDirectory) {
                        val safeName = entry.name.substringAfterLast('/').replace(Regex("[/\\\\\\\\:*?\"<>|]"), "_")
                        val mime = if (safeName.endsWith(".json", true)) "application/json" else "application/octet-stream"
                        val target = DocumentsContract.createDocument(context.contentResolver, destinationTree, mime, safeName)
                            ?: error("Destination-এ ফাইল তৈরি করা যায়নি")
                        context.contentResolver.openOutputStream(target, "w")?.use { output -> zip.copyTo(output) }
                            ?: error("Restore ফাইল লেখা যায়নি")
                        restored++
                    }
                    zip.closeEntry()
                }
        }
        return restored
    }
}

private fun queryDisplayName(context: Context, uri: Uri): String {
    var name = "selected_file"
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) name = cursor.getString(0) ?: name
    }
    return name
}
