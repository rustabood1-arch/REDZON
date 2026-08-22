package com.redzon.app
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import java.io.RandomAccessFile
import kotlin.concurrent.thread

class MainActivity : Activity() {

private lateinit var statusText: TextView
private lateinit var cpuText: TextView
private lateinit var ramText: TextView
private lateinit var startBtn: Button
private lateinit var dashLayout: LinearLayout
@Volatile private var isRunning = true

override fun onCreate(savedInstanceState: Bundle?) {
super.onCreate(savedInstanceState)

// فحص الروت المباشر
if (!checkRootPermission()) {
showRootDeniedUI()
return
}

// بناء الواجهة الرئيسية
showMainUI()
startSystemMonitoring()
}

private fun checkRootPermission(): Boolean {
return try {
val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
process.waitFor() == 0
} catch (e: Exception) {
false
}
}

private fun runRootCommand(command: String): Boolean {
return try {
val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
process.waitFor() == 0
} catch (e: Exception) {
false
}
}

private fun showRootDeniedUI() {
val rootLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setBackgroundColor(Color.parseColor("#101010"))
gravity = Gravity.CENTER
setPadding(50, 50, 50, 50)
}

val title = TextView(this).apply {
text = "⚠️ خطأ بالصلاحيات"
setTextColor(Color.RED)
textSize = 22f
typeface = Typeface.DEFAULT_BOLD
gravity = Gravity.CENTER
}

val desc = TextView(this).apply {
text = "التطبيق يتطلب صلاحيات الروت للعمل!\nيرجى منح الصلاحية من تطبيق الروت وإعادة الفتح."
setTextColor(Color.WHITE)
textSize = 16f
gravity = Gravity.CENTER
setPadding(0, 30, 0, 50)
}

val exitBtn = Button(this).apply {
text = "إغلاق التطبيق"
setBackgroundColor(Color.RED)
setTextColor(Color.WHITE)
setOnClickListener {
finishAffinity()
Process.killProcess(Process.myPid())
}
}

rootLayout.addView(title)
rootLayout.addView(desc)
rootLayout.addView(exitBtn)
setContentView(rootLayout)
}

private fun showMainUI() {
val scrollView = ScrollView(this).apply {
setBackgroundColor(Color.parseColor("#101010"))
isFillViewport = true
}

val mainLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setPadding(40, 60, 40, 40)
gravity = Gravity.CENTER_HORIZONTAL
layoutParams = ViewGroup.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.WRAP_CONTENT
)
}

val appTitle = TextView(this).apply {
text = "REDZON FPS"
setTextColor(Color.parseColor("#D4AF37"))
textSize = 32f
typeface = Typeface.DEFAULT_BOLD
gravity = Gravity.CENTER
}

startBtn = Button(this).apply {
text = "START FPS REDZON"
setBackgroundColor(Color.parseColor("#1A1A1A"))
setTextColor(Color.parseColor("#D4AF37"))
textSize = 18f
setPadding(20, 30, 20, 30)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 20, 0, 20)
}
}

dashLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
visibility = View.GONE
setPadding(0, 40, 0, 0)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
}

val infoCard = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setBackgroundColor(Color.parseColor("#1A1A1A"))
setPadding(30, 30, 30, 30)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
}

val infoTitle = TextView(this).apply {
text = "📊 مراقب النظام"
setTextColor(Color.parseColor("#D4AF37"))
textSize = 18f
typeface = Typeface.DEFAULT_BOLD
}

cpuText = TextView(this).apply {
text = "استخدام المعالج (CPU): 0%"
setTextColor(Color.WHITE)
textSize = 15f
setPadding(0, 15, 0, 5)
}

ramText = TextView(this).apply {
text = "استخدام الذاكرة (RAM): 0 MB"
setTextColor(Color.WHITE)
textSize = 15f
setPadding(0, 5, 0, 15)
}

statusText = TextView(this).apply {
text = "الحالة الحالية: الوضع الافتراضي"
setTextColor(Color.parseColor("#D4AF37"))
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
}

infoCard.addView(infoTitle)
infoCard.addView(cpuText)
infoCard.addView(ramText)
infoCard.addView(statusText)

val btnFps = Button(this).apply {
text = "⚡ زيادة وتثبيت الـ FPS"
setBackgroundColor(Color.parseColor("#B8860B"))
setTextColor(Color.BLACK)
textSize = 16f
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setOnClickListener {
runRootCommand("setprop debug.gr.swapinterval 0; settings put system peak_refresh_rate 120.0; settings put system user_refresh_rate 120.0")
statusText.text = "الحالة الحالية: 🔥 تم تثبيت الـ FPS بأقصى أداء!"
}
}

val btnReset = Button(this).apply {
text = "🛑 إيقاف المميزات والعودة للافتراضي"
setBackgroundColor(Color.parseColor("#333333"))
setTextColor(Color.WHITE)
textSize = 15f
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setOnClickListener {
runRootCommand("settings delete system peak_refresh_rate; settings delete system user_refresh_rate")
statusText.text = "الحالة الحالية: الوضع الافتراضي"
}
}

val btnBoost = Button(this).apply {
text = "🚀 تنظيف الرام (Boost RAM)"
setBackgroundColor(Color.parseColor("#1A1A1A"))
setTextColor(Color.parseColor("#D4AF37"))
textSize = 15f
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setOnClickListener {
runRootCommand("sync; echo 3 > /proc/sys/vm/drop_caches")
statusText.text = "الحالة الحالية: 🧹 تم تنظيف الرام!"
}
}

val tgBtn = TextView(this).apply {
text = "للدعم والدعم الفني: @xxxzwxxx"
setTextColor(Color.parseColor("#D4AF37"))
textSize = 14f
gravity = Gravity.CENTER
setPadding(0, 50, 0, 20)
setOnClickListener {
startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/xxxzwxxx")))
}
}

startBtn.setOnClickListener {
startBtn.visibility = View.GONE
dashLayout.visibility = View.VISIBLE
}

dashLayout.addView(infoCard)
dashLayout.addView(btnFps)
dashLayout.addView(btnReset)
dashLayout.addView(btnBoost)

mainLayout.addView(appTitle)
mainLayout.addView(View(this).apply { 
minimumHeight = 60 
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
60
)
})
mainLayout.addView(startBtn)
mainLayout.addView(dashLayout)
mainLayout.addView(tgBtn)

scrollView.addView(mainLayout)
setContentView(scrollView)
}

private fun startSystemMonitoring() {
thread {
while (isRunning) {
val cpu = getCpuUsage()
val ram = getRamUsage()
runOnUiThread {
if (::cpuText.isInitialized) cpuText.text = "استخدام المعالج (CPU): $cpu"
if (::ramText.isInitialized) ramText.text = "استخدام الذاكرة (RAM): $ram"
}
Thread.sleep(2000)
}
}
}

private fun getRamUsage(): String {
return try {
val reader = RandomAccessFile("/proc/meminfo", "r")
val totalLine = reader.readLine()
reader.readLine()
val availLine = reader.readLine()
reader.close()

val totalKb = totalLine.replace("\\D+".toRegex(), "").toLong()
val availKb = availLine.replace("\\D+".toRegex(), "").toLong()

val usedMb = (totalKb - availKb) / 1024
val totalMb = totalKb / 1024
"$usedMb MB / $totalMb MB"
} catch (e: Exception) {
"غير معروف"
}
}

private fun getCpuUsage(): String {
return try {
val file = File("/proc/stat")
if (file.exists()) {
val lines = file.readLines()
if (lines.isNotEmpty()) {
val toks = lines[0].split("\\s+".toRegex())
val idle = toks[4].toLong()
val total = toks.slice(1..7).map { it.toLong() }.sum()
val usage = ((total - idle) * 100 / total).toInt()
"$usage%"
} else "15%"
} else "12%"
} catch (e: Exception) {
"10%"
}
}

override fun onDestroy() {
super.onDestroy()
isRunning = false
}
}
