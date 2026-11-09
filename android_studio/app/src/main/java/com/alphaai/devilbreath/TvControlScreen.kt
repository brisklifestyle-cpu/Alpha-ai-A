package com.alphaai.devilbreath

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors

private data class TvFound(val name: String, val host: String, val port: Int)

@Composable
fun TvControlScreen() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    val devices = remember { mutableStateListOf<TvFound>() }
    var scanning by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("একই Wi-Fi নেটওয়ার্কে TV/Google TV Box রাখুন।") }
    var selected by remember { mutableStateOf<TvFound?>(null) }
    var probe by remember { mutableStateOf("") }
    val executor = remember { Executors.newSingleThreadExecutor() }
    var listener by remember { mutableStateOf<NsdManager.DiscoveryListener?>(null) }

    fun startScan() {
        devices.clear()
        status = "Android TV Remote Service খোঁজা হচ্ছে…"
        scanning = true
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                if (serviceInfo.serviceType.contains("androidtvremote2")) {
                    @Suppress("DEPRECATION")
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            status = "একটি TV পাওয়া গেছে, কিন্তু ঠিকানা resolve করা যায়নি।"
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val ip = info.host?.hostAddress ?: return
                            val item = TvFound(info.serviceName, ip, info.port)
                            if (devices.none { it.host == item.host }) devices.add(item)
                            status = "TV পাওয়া গেছে: ${item.name} (${item.host})"
                        }
                    })
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                devices.removeAll { it.name == serviceInfo.serviceName }
            }
            override fun onDiscoveryStopped(serviceType: String) { scanning = false }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                scanning = false; status = "Scan শুরু করা যায়নি (code $errorCode)। Wi-Fi চালু আছে কি না দেখুন।"
                runCatching { manager.stopServiceDiscovery(this) }
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                status = "Scan বন্ধ করতে সমস্যা হয়েছে (code $errorCode)।"
                runCatching { manager.stopServiceDiscovery(this) }
            }
        }
        listener = l
        runCatching { manager.discoverServices("_androidtvremote2._tcp.", NsdManager.PROTOCOL_DNS_SD, l) }
            .onFailure { scanning = false; status = "Network discovery ব্যর্থ: ${it.localizedMessage}" }
    }

    DisposableEffect(Unit) {
        onDispose {
            listener?.let { runCatching { manager.stopServiceDiscovery(it) } }
            executor.shutdown()
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("TV CONTROL", color = Color(0xFFE32636), style = MaterialTheme.typography.titleLarge)
        Text("Android TV / Google TV • Local Wi-Fi", color = Color.LightGray)
        Text(status, color = Color.LightGray, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { if (scanning) { listener?.let { runCatching { manager.stopServiceDiscovery(it) } }; scanning = false } else startScan() }) {
                Icon(if (scanning) Icons.Default.Stop else Icons.Default.Search, null)
                Spacer(Modifier.width(6.dp)); Text(if (scanning) "Stop Scan" else "Scan TV")
            }
        }
        OutlinedTextField(
            value = manualIp, onValueChange = { manualIp = it },
            modifier = Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("TV IP address (manual)") },
            placeholder = { Text("192.168.1.100") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        Button(enabled = manualIp.trim().isNotEmpty(), onClick = {
            val ip = manualIp.trim()
            val item = TvFound("Manual TV", ip, 6466)
            if (devices.none { it.host == ip }) devices.add(item)
            selected = item
            status = "Manual address যোগ হয়েছে। এখন service port পরীক্ষা করুন।"
        }) { Text("Add TV by IP") }
        if (devices.isNotEmpty()) {
            Text("FOUND DEVICES", color = Color(0xFFE32636))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(devices) { tv ->
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF202020)), onClick = { selected = tv; status = "${tv.name} নির্বাচিত হয়েছে।" }) {
                        Column(Modifier.fillMaxWidth().padding(14.dp)) {
                            Text(tv.name, color = Color.White)
                            Text("${tv.host} • service port ${tv.port}", color = Color.LightGray)
                            if (selected?.host == tv.host) Text("Selected", color = Color(0xFFE32636))
                        }
                    }
                }
            }
        } else {
            Text("কোনো TV পাওয়া যায়নি। Router-এ multicast বন্ধ থাকলে TV-এর IP হাতে লিখে যোগ করুন।", color = Color.LightGray)
            Spacer(Modifier.weight(1f))
        }
        Button(
            enabled = selected != null || manualIp.isNotBlank(),
            onClick = {
                val ip = selected?.host ?: manualIp.trim()
                probe = "Testing 6466/6467…"
                executor.execute {
                    fun test(port: Int): Boolean = runCatching {
                        Socket().use { it.connect(InetSocketAddress(ip, port), 1800); true }
                    }.getOrDefault(false)
                    val remote = test(6466)
                    val pairing = test(6467)
                    val result = "TV $ip — Remote 6466: ${if (remote) "reachable" else "not reachable"}; Pairing 6467: ${if (pairing) "reachable" else "not reachable"}."
                    android.os.Handler(android.os.Looper.getMainLooper()).post { probe = result }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE32636))
        ) { Text("Test TV Connection") }
        if (probe.isNotBlank()) Text(probe, color = Color.White)
        Text(
            "নোট: এই ধাপে TV discovery ও network port test আছে। নিরাপদ PIN pairing এবং আসল remote key command পরের ধাপের protocol integration; এখনো D-pad/Power/Volume বোতাম সক্রিয় নয়।",
            color = Color(0xFFFFCC80), style = MaterialTheme.typography.bodySmall
        )
    }
}
