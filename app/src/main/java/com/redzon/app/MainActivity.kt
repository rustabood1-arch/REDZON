package com.redzon.app

import android.app.ActivityManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale

private val Ink = Color(0xFF09111D)
private val Panel = Color(0xFF111E2C)
private val Cyan = Color(0xFF2DD4BF)
private val Amber = Color(0xFFF4B860)
private val Muted = Color(0xFF91A5B8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { RedzonApp(applicationContext) }
    }
}

@androidx.compose.runtime.Composable
private fun RedzonApp(context: Context) {
    var rootStatus by remember { mutableStateOf("جار طلب صلاحية ROOT...") }
    var rootReady by remember { mutableStateOf(false) }
    var cpu by remember { mutableStateOf(0f) }
    var ram by remember { mutableStateOf(0f) }
    var fpsLocked by remember { mutableStateOf(false) }
    var actionStatus by remember { mutableStateOf("المراقبة تعمل مباشرة") }
    var pendingRootCommand by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val rootResult = withContext(Dispatchers.IO) { RootShell.run("id") }
        rootReady = rootResult?.contains("uid=0") == true
        rootStatus = if (rootReady) "ROOT متصل" else "ROOT غير متاح"
    }

    LaunchedEffect(Unit) {
        var previous = ProcStats.readCpu()
        while (true) {
            delay(1000)
            val current = ProcStats.readCpu()
            cpu = ProcStats.usage(previous, current)
            previous = current
            ram = ProcStats.readRam(context)
        }
    }

    LaunchedEffect(pendingRootCommand) {
        pendingRootCommand?.let { command ->
            val succeeded = withContext(Dispatchers.IO) { RootShell.run(command) != null }
            actionStatus = if (succeeded) "تم تطبيق الإعداد بنجاح" else "تعذر تنفيذ أمر ROOT"
            pendingRootCommand = null
        }
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = Ink) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("REDZON", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("تحكم ذكي بأداء جهازك", color = Muted, fontSize = 14.sp)
                    }
                    StatusPill(rootStatus, rootReady)
                }

                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("المراقبة المباشرة", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MetricCard("CPU", String.format(Locale.US, "%.0f%%", cpu), Cyan, Modifier.weight(1f))
                            MetricCard("RAM", String.format(Locale.US, "%.0f%%", ram), Amber, Modifier.weight(1f))
                        }
                        Text(actionStatus, color = Muted, fontSize = 12.sp)
                    }
                }

                Text("أدوات الأداء", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = Cyan, modifier = Modifier.size(28.dp))
                            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                                Text("قفل FPS", color = Color.White, fontWeight = FontWeight.Bold)
                                Text("تثبيت معدل التحديث على 120Hz عبر ROOT", color = Muted, fontSize = 12.sp)
                            }
                        }
                        Button(
                            onClick = {
                                actionStatus = "جار تطبيق قفل FPS..."
                                fpsLocked = true
                                pendingRootCommand = "settings put system peak_refresh_rate 120.0; settings put system min_refresh_rate 120.0"
                            },
                            enabled = rootReady && !fpsLocked,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null)
                            Text("  تطبيق ROOT FPS LOCK", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                fpsLocked = false
                                actionStatus = "تمت استعادة الإعدادات الافتراضية"
                                pendingRootCommand = "settings delete system peak_refresh_rate; settings delete system min_refresh_rate"
                            },
                            enabled = rootReady,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Text("  إيقاف التعديل والعودة للافتراضي")
                        }
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("REDZON PERFORMANCE CONTROL  •  ${if (rootReady) "ROOT ENABLED" else "LIMITED MODE"}", color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun StatusPill(status: String, ready: Boolean) {
    Box(modifier = Modifier.background(if (ready) Cyan.copy(alpha = .16f) else Amber.copy(alpha = .16f), RoundedCornerShape(50))) {
        Text(status, color = if (ready) Cyan else Amber, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

@androidx.compose.runtime.Composable
private fun MetricCard(label: String, value: String, accent: Color, modifier: Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = .1f)), shape = RoundedCornerShape(14.dp)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(if (label == "CPU") Icons.Default.Memory else Icons.Default.Memory, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text(label, color = accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

private object RootShell {
    fun run(command: String): String? = try {
        val process = ProcessBuilder("su", "-c", command).redirectErrorStream(true).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }
        if (process.waitFor() == 0) output else null
    } catch (_: Exception) { null }
}

private object ProcStats {
    data class Cpu(val idle: Long, val total: Long)

    fun readCpu(): Cpu {
        val fields = java.io.File("/proc/stat").useLines { lines -> lines.first().trim().split(Regex("\\s+")) }
        val values = fields.drop(1).map { it.toLongOrNull() ?: 0L }
        return Cpu(values.getOrElse(3) { 0L }, values.sum())
    }

    fun usage(previous: Cpu, current: Cpu): Float {
        val totalDelta = current.total - previous.total
        val idleDelta = current.idle - previous.idle
        return if (totalDelta > 0) ((totalDelta - idleDelta) * 100f / totalDelta).coerceIn(0f, 100f) else 0f
    }

    fun readRam(context: Context): Float {
        val info = ActivityManager.MemoryInfo()
        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(info)
        return ((info.totalMem - info.availMem) * 100f / info.totalMem).coerceIn(0f, 100f)
    }
}
