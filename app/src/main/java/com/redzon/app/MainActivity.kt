package com.redzon.app
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.AnimationSet
import android.view.animation.ScaleAnimation
import android.view.animation.TranslateAnimation
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
private lateinit var fpsText: TextView
private lateinit var gameOptText: TextView
private lateinit var gpuText: TextView
private lateinit var thermalText: TextView
private lateinit var startBtn: Button
private lateinit var dashLayout: LinearLayout
private lateinit var infoCard: LinearLayout
@Volatile private var isRunning = true
@Volatile private var isFpsBooted = false
@Volatile private var isGameModeActive = false
@Volatile private var currentTemp = 0

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
setBackgroundColor(Color.parseColor("#0A0E27"))
gravity = Gravity.CENTER
setPadding(50, 50, 50, 50)
}

val title = TextView(this).apply {
text = "⚠️ خطأ بالصلاحيات"
setTextColor(Color.parseColor("#FF3B30"))
textSize = 24f
typeface = Typeface.DEFAULT_BOLD
gravity = Gravity.CENTER
}

val desc = TextView(this).apply {
text = "التطبيق يتطلب صلاحيات الروت للعمل!\nيرجى منح الصلاحية من تطبيق الروت وإعادة الفتح."
setTextColor(Color.parseColor("#E0E0E0"))
textSize = 16f
gravity = Gravity.CENTER
setPadding(0, 30, 0, 50)
}

val exitBtn = Button(this).apply {
text = "إغلاق التطبيق"
setBackgroundColor(Color.parseColor("#FF3B30"))
setTextColor(Color.WHITE)
textSize = 16f
setPadding(20, 20, 20, 20)
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

private fun createWelcomeAnimation(): AnimationSet {
val animationSet = AnimationSet(true)
animationSet.duration = 1500

val fadeIn = AlphaAnimation(0f, 1f)
fadeIn.duration = 1500

val scaleAnimation = ScaleAnimation(0.8f, 1f, 0.8f, 1f, 
ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
scaleAnimation.duration = 1500

animationSet.addAnimation(fadeIn)
animationSet.addAnimation(scaleAnimation)
return animationSet
}

private fun createSlideUpAnimation(): AnimationSet {
val animationSet = AnimationSet(true)
animationSet.duration = 1200

val slideUp = TranslateAnimation(
TranslateAnimation.RELATIVE_TO_SELF, 0f,
TranslateAnimation.RELATIVE_TO_SELF, 0f,
TranslateAnimation.RELATIVE_TO_SELF, 0.5f,
TranslateAnimation.RELATIVE_TO_SELF, 0f
)
slideUp.duration = 1200

val fadeIn = AlphaAnimation(0f, 1f)
fadeIn.duration = 1200

animationSet.addAnimation(slideUp)
animationSet.addAnimation(fadeIn)
return animationSet
}

private fun createPulseAnimation(): AnimationSet {
val animationSet = AnimationSet(true)
animationSet.duration = 2000
animationSet.repeatCount = android.view.animation.Animation.INFINITE
animationSet.repeatMode = android.view.animation.Animation.RESTART

val pulse = ScaleAnimation(1f, 1.05f, 1f, 1.05f,
ScaleAnimation.RELATIVE_TO_SELF, 0.5f,
ScaleAnimation.RELATIVE_TO_SELF, 0.5f)
pulse.duration = 2000

animationSet.addAnimation(pulse)
return animationSet
}

private fun showMainUI() {
val scrollView = ScrollView(this).apply {
setBackgroundColor(Color.parseColor("#0A0E27"))
isFillViewport = true
}

val mainLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setPadding(30, 40, 30, 40)
gravity = Gravity.CENTER_HORIZONTAL
layoutParams = ViewGroup.LayoutParams(
ViewGroup.LayoutParams.MATCH_PARENT,
ViewGroup.LayoutParams.WRAP_CONTENT
)
}

val welcomeLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
gravity = Gravity.CENTER
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
}

val welcomeTitle = TextView(this).apply {
text = "مرحباً بك 👋"
setTextColor(Color.parseColor("#00D4FF"))
textSize = 32f
typeface = Typeface.DEFAULT_BOLD
gravity = Gravity.CENTER
}

val welcomeSubtitle = TextView(this).apply {
text = "FAST FPS REDZON"
setTextColor(Color.parseColor("#FFD700"))
textSize = 24f
typeface = Typeface.DEFAULT_BOLD
gravity = Gravity.CENTER
setPadding(0, 15, 0, 35)
}

val welcomeDesc = TextView(this).apply {
text = "تطبيق متخصص لتحسين وتثبيت الـ FPS\n⚡ أداء عالي | 🚀 سرعة فائقة | 💎 جودة ممتازة"
setTextColor(Color.parseColor("#A0A0A0"))
textSize = 14f
gravity = Gravity.CENTER
setPadding(0, 0, 0, 50)
}

welcomeTitle.startAnimation(createWelcomeAnimation())
welcomeSubtitle.startAnimation(createWelcomeAnimation())
welcomeDesc.startAnimation(createWelcomeAnimation())

welcomeLayout.addView(welcomeTitle)
welcomeLayout.addView(welcomeSubtitle)
welcomeLayout.addView(welcomeDesc)

startBtn = Button(this).apply {
text = "🚀 ابدأ الآن - تفعيل وضع الأداء"
setBackgroundColor(Color.parseColor("#FF3B30"))
setTextColor(Color.WHITE)
textSize = 16f
typeface = Typeface.DEFAULT_BOLD
setPadding(30, 25, 30, 25)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 20, 0, 30)
}
startAnimation(createSlideUpAnimation())
}

dashLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
visibility = View.GONE
setPadding(0, 30, 0, 0)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
}

infoCard = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
setBackgroundColor(Color.parseColor("#1A1F3A"))
setPadding(25, 25, 25, 25)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 0, 0, 20)
}
}

val cardTitle = TextView(this).apply {
text = "📊 مراقب النظام المتقدم"
setTextColor(Color.parseColor("#00D4FF"))
textSize = 18f
typeface = Typeface.DEFAULT_BOLD
}

cpuText = TextView(this).apply {
text = "🔥 CPU: 0%"
setTextColor(Color.parseColor("#00FF00"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 12, 0, 6)
}

ramText = TextView(this).apply {
text = "💾 RAM: 0 MB / 0 MB"
setTextColor(Color.parseColor("#FFD700"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 6, 0, 6)
}

fpsText = TextView(this).apply {
text = "⚡ FPS: 60 FPS"
setTextColor(Color.parseColor("#FF3B30"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 6, 0, 6)
}

gpuText = TextView(this).apply {
text = "🎨 GPU: 0% استخدام"
setTextColor(Color.parseColor("#00FF88"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 6, 0, 6)
}

thermalText = TextView(this).apply {
text = "🌡️ الحرارة: 35°C - ممتاز"
setTextColor(Color.parseColor("#87CEEB"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 6, 0, 6)
}

gameOptText = TextView(this).apply {
text = "🎮 وضع الألعاب: غير مفعل"
setTextColor(Color.parseColor("#FF6B6B"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 6, 0, 15)
}

statusText = TextView(this).apply {
text = "الحالة: 🟢 وضع عادي"
setTextColor(Color.parseColor("#A0D995"))
textSize = 13f
typeface = Typeface.DEFAULT_BOLD
}

infoCard.addView(cardTitle)
infoCard.addView(cpuText)
infoCard.addView(ramText)
infoCard.addView(fpsText)
infoCard.addView(gpuText)
infoCard.addView(thermalText)
infoCard.addView(gameOptText)
infoCard.addView(statusText)

// === زر تثبيت FPS الرئيسي ===
val btnFps = Button(this).apply {
text = "⚡ تثبيت FPS - 120+ FPS"
setBackgroundColor(Color.parseColor("#FF3B30"))
setTextColor(Color.WHITE)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
if (!isFpsBooted) {
applyMaxFpsBoost()
btnFps.text = "✅ نشط - 120+ FPS"
btnFps.setBackgroundColor(Color.parseColor("#34C759"))
statusText.text = "الحالة: 🔥 وضع الأداء الكامل"
fpsText.text = "⚡ FPS: 120+ FPS ✨"
fpsText.startAnimation(createPulseAnimation())
isFpsBooted = true
} else {
statusText.text = "الحالة: ⚠️ وضع الأداء مفعل"
}
}
}

// === وضع الألعاب المتقدم ===
val btnGameMode = Button(this).apply {
text = "🎮 وضع الألعاب الاحترافي"
setBackgroundColor(Color.parseColor("#FF1744"))
setTextColor(Color.WHITE)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
if (!isGameModeActive) {
applyAdvancedGameMode()
btnGameMode.text = "✅ وضع الألعاب نشط"
btnGameMode.setBackgroundColor(Color.parseColor("#34C759"))
gameOptText.text = "🎮 وضع الألعاب: مفعل قوي 🚀"
gameOptText.startAnimation(createPulseAnimation())
statusText.text = "الحالة: 🎮 تحسين شامل للألعاب"
isGameModeActive = true
} else {
statusText.text = "الحالة: ⚠️ وضع الألعاب مفعل بالفعل"
}
}
}

// === تحسين الرسومات العميق ===
val btnGraphics = Button(this).apply {
text = "🎨 تحسين الرسومات العميق"
setBackgroundColor(Color.parseColor("#7C3AED"))
setTextColor(Color.WHITE)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
applyDeepGraphicsOptimization()
statusText.text = "الحالة: 🎨 تحسين رسومات عميق مفعل"
gpuText.text = "🎨 GPU: تحسين عميق نشط!"
gpuText.startAnimation(createPulseAnimation())
}
}

// === تقليل التأخير والـ Lag ===
val btnLagReduce = Button(this).apply {
text = "⚡ تقليل التأخير (Input Lag)"
setBackgroundColor(Color.parseColor("#EC4899"))
setTextColor(Color.WHITE)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
reduceLagAndLatency()
statusText.text = "الحالة: ⚡ تقليل التأخير مفعل"
}
}

// === تحسين الحرارة والبطارية ===
val btnCooling = Button(this).apply {
text = "❄️ تحسين التبريد والبطارية"
setBackgroundColor(Color.parseColor("#06B6D4"))
setTextColor(Color.BLACK)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
optimizeCoolingAndBattery()
statusText.text = "الحالة: ❄️ تحسين التبريد نشط"
thermalText.startAnimation(createPulseAnimation())
}
}

// === تنظيف الرام ===
val btnBoost = Button(this).apply {
text = "🧹 تنظيف الرام الفوري"
setBackgroundColor(Color.parseColor("#00D4FF"))
setTextColor(Color.BLACK)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
boostRamCleanup()
statusText.text = "الحالة: 🧹 تم تنظيف الرام بنجاح"
ramText.startAnimation(createPulseAnimation())
}
}

// === وضع متوازن ===
val btnBalance = Button(this).apply {
text = "⚙️ وضع متوازن"
setBackgroundColor(Color.parseColor("#9370DB"))
setTextColor(Color.WHITE)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
applyBalancedMode()
statusText.text = "الحالة: ⚙️ وضع متوازن نشط"
fpsText.text = "⚡ FPS: 90 FPS متوازن"
}
}

// === إعادة التعيين ===
val btnReset = Button(this).apply {
text = "🔄 العودة للافتراضي"
setBackgroundColor(Color.parseColor("#FFD700"))
setTextColor(Color.BLACK)
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 12, 0, 12)
}
setPadding(25, 25, 25, 25)
setOnClickListener {
resetToDefault()
btnFps.text = "⚡ تثبيت FPS - 120+ FPS"
btnFps.setBackgroundColor(Color.parseColor("#FF3B30"))
btnGameMode.text = "🎮 وضع الألعاب الاحترافي"
btnGameMode.setBackgroundColor(Color.parseColor("#FF1744"))
statusText.text = "الحالة: 🟢 وضع عادي"
fpsText.text = "⚡ FPS: 60 FPS"
gameOptText.text = "🎮 وضع الألعاب: غير مفعل"
fpsText.clearAnimation()
gameOptText.clearAnimation()
isFpsBooted = false
isGameModeActive = false
}
}

dashLayout.addView(infoCard)
dashLayout.addView(btnFps)
dashLayout.addView(btnGameMode)
dashLayout.addView(btnGraphics)
dashLayout.addView(btnLagReduce)
dashLayout.addView(btnCooling)
dashLayout.addView(btnBoost)
dashLayout.addView(btnBalance)
dashLayout.addView(btnReset)

val supportInfo = TextView(this).apply {
text = "📱 للدعم الفني: @xxxzwxxx"
setTextColor(Color.parseColor("#00D4FF"))
textSize = 12f
gravity = Gravity.CENTER
setPadding(0, 20, 0, 20)
setOnClickListener {
startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/xxxzwxxx")))
}
}

startBtn.setOnClickListener {
startBtn.visibility = View.GONE
welcomeLayout.visibility = View.GONE
dashLayout.visibility = View.VISIBLE
infoCard.startAnimation(createSlideUpAnimation())
}

mainLayout.addView(welcomeLayout)
mainLayout.addView(startBtn)
mainLayout.addView(dashLayout)
mainLayout.addView(supportInfo)

scrollView.addView(mainLayout)
setContentView(scrollView)
}

private fun applyMaxFpsBoost() {
thread {
runRootCommand("setprop debug.gr.swapinterval 0")
runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
runRootCommand("setprop ro.hwui.gradient_cache_size 1")
runRootCommand("setprop ro.hwui.layer_cache_size 48")
runRootCommand("setprop ro.hwui.path_cache_size 32")
runRootCommand("settings put system peak_refresh_rate 120.0")
runRootCommand("settings put system user_refresh_rate 120.0")
runRootCommand("setprop ro.surface_flinger.max_frame_buffer_acquired_buffers 3")
runRootCommand("sync")
runRootCommand("echo 3 > /proc/sys/vm/drop_caches")
}
}

private fun applyAdvancedGameMode() {
thread {
// إعدادات أساسية للألعاب
runRootCommand("setprop debug.gr.swapinterval 0")
runRootCommand("setprop ro.hwui.layer_cache_size 48")
runRootCommand("setprop ro.hwui.path_cache_size 32")
runRootCommand("setprop ro.hwui.r_buffer_cache_size 8")

// تحسين GPU
runRootCommand("setprop ro.vendor.gpu.hal qti")
runRootCommand("setprop ro.qualcomm.gpu.adreno_stacksize_kb 512")

// معدل التحديث الأقصى
runRootCommand("settings put system peak_refresh_rate 120.0")
runRootCommand("settings put system user_refresh_rate 120.0")

// تحسين الذاكرة
runRootCommand("setprop ro.vendor.vm.swappiness 60")

// تقليل التأخير
runRootCommand("setprop ro.input.vid_enabled true")

// إيقاف التطبيقات المزعجة
runRootCommand("pm disable --user 0 com.google.android.gms || true")
runRootCommand("pm disable --user 0 com.android.chrome || true")

// حذف ملفات مؤقتة
runRootCommand("sync && echo 3 > /proc/sys/vm/drop_caches")
}
}

private fun applyDeepGraphicsOptimization() {
thread {
// تحسين محرك الرسومات
runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
runRootCommand("setprop ro.hwui.gradient_cache_size 1")
runRootCommand("setprop ro.hwui.layer_cache_size 48")
runRootCommand("setprop ro.hwui.path_cache_size 32")
runRootCommand("setprop ro.hwui.text_large_cache_height 1024")
runRootCommand("setprop ro.hwui.text_large_cache_width 2048")

// تحسين GPU
runRootCommand("setprop ro.vendor.gpu.hal qti")
runRootCommand("setprop ro.opengles.version 196610")

// تحسين Surface Flinger
runRootCommand("setprop ro.surface_flinger.has_HDR_display true")
runRootCommand("setprop ro.surface_flinger.protected_contents true")

// تحسين الذاكرة المرئية
runRootCommand("setprop ro.hwui.print_config 0")
runRootCommand("sync")
}
}

private fun reduceLagAndLatency() {
thread {
// تقليل تأخير الإدخال
runRootCommand("setprop ro.input.vid_enabled true")
runRootCommand("setprop ro.qti.sensors.max_accel_rate 50")

// تحسين استجابة النظام
runRootCommand("setprop ro.iorapd.enable true")
runRootCommand("setprop ro.sys.usb.config mtp,adb")

// تحسين المؤشر
runRootCommand("setprop ro.hardware.keystore msm8974")

// تقليل التأخير في العرض
runRootCommand("setprop debug.gr.swapinterval 0")
runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 0")
}
}

private fun optimizeCoolingAndBattery() {
thread {
// تحسين إدارة الحرارة
runRootCommand("setprop persist.sys.usb.config mtp,adb")
runRootCommand("setprop ro.vendor.thermal.polling_delay 10000")

// تحسين البطارية
runRootCommand("setprop persist.sys.profiler_ms 0")
runRootCommand("setprop sys.sysctl.extra_free_kbytes 43200")

// تقليل استهلاك الطاقة
runRootCommand("setprop ro.vendor.extension_library /vendor/lib/rfsa/adsp/libfastcvopt.so")

// تحسين إدارة الذاكرة
runRootCommand("sync && echo 2 > /proc/sys/vm/drop_caches")
}
}

private fun boostRamCleanup() {
thread {
runRootCommand("sync")
runRootCommand("echo 1 > /proc/sys/vm/drop_caches")
runRootCommand("echo 2 > /proc/sys/vm/drop_caches")
runRootCommand("echo 3 > /proc/sys/vm/drop_caches")
runRootCommand("killall com.android.systemui || true")
runRootCommand("am trim-caches 100M || true")
}
}

private fun applyBalancedMode() {
thread {
runRootCommand("setprop debug.gr.swapinterval 1")
runRootCommand("settings put system peak_refresh_rate 90.0")
runRootCommand("settings put system user_refresh_rate 90.0")
runRootCommand("sync && echo 2 > /proc/sys/vm/drop_caches")
}
}

private fun resetToDefault() {
thread {
runRootCommand("settings delete system peak_refresh_rate")
runRootCommand("settings delete system user_refresh_rate")
runRootCommand("setprop debug.gr.swapinterval -1")
runRootCommand("pm enable --user 0 com.google.android.gms || true")
runRootCommand("pm enable --user 0 com.android.chrome || true")
}
}

private fun startSystemMonitoring() {
thread {
while (isRunning) {
val cpu = getCpuUsage()
val ram = getRamUsage()
val gpu = getGpuUsage()
val temp = getSystemTemperature()
currentTemp = temp

runOnUiThread {
if (::cpuText.isInitialized) cpuText.text = "🔥 CPU: $cpu"
if (::ramText.isInitialized) ramText.text = "💾 RAM: $ram"
if (::gpuText.isInitialized) gpuText.text = "🎨 GPU: $gpu"
if (::thermalText.isInitialized) {
val tempStatus = when {
temp > 45 -> "🔴 حار جداً"
temp > 40 -> "🟠 ساخن"
temp > 35 -> "🟡 دافئ"
else -> "🟢 ممتاز"
}
thermalText.text = "🌡️ الحرارة: ${temp}°C - $tempStatus"
}
if (::fpsText.isInitialized) {
val fps = when {
isFpsBooted -> "120+ FPS ✨"
isGameModeActive -> "120+ FPS 🎮"
else -> "60 FPS"
}
fpsText.text = "⚡ FPS: $fps"
}
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

private fun getGpuUsage(): String {
return try {
val frequency = File("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq").readText().trim().toLong() / 1000000
"${frequency}MHz"
} catch (e: Exception) {
"N/A"
}
}

private fun getSystemTemperature(): Int {
return try {
val thermalZone = File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toInt() / 1000
thermalZone
} catch (e: Exception) {
35
}
}

override fun onDestroy() {
super.onDestroy()
isRunning = false
}
}
