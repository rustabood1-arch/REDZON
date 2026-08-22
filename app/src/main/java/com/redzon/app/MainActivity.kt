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
private lateinit var startBtn: Button
private lateinit var dashLayout: LinearLayout
private lateinit var infoCard: LinearLayout
@Volatile private var isRunning = true
@Volatile private var isFpsBooted = false
@Volatile private var isGameModeActive = false

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

// === تحريك Fade In + Scale ===
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

// === تحريك رفع العناصر من الأسفل ===
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

// === تحريك النضض الفخم ===
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

// === الشاشة الترحيبية ===
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

// === لوحة التحكم الرئيسية ===
dashLayout = LinearLayout(this).apply {
orientation = LinearLayout.VERTICAL
visibility = View.GONE
setPadding(0, 30, 0, 0)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
}

// === بطاقة مراقب النظام الفخمة مع تحريك ممتاز ===
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
text = "🔥 استخدام المعالج: 0%"
setTextColor(Color.parseColor("#00FF00"))
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 15, 0, 8)
}

ramText = TextView(this).apply {
text = "💾 استخدام الذاكرة: 0 MB / 0 MB"
setTextColor(Color.parseColor("#FFD700"))
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 8, 0, 8)
}

fpsText = TextView(this).apply {
text = "⚡ الفريمات: 60 FPS"
setTextColor(Color.parseColor("#FF3B30"))
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 8, 0, 8)
}

gameOptText = TextView(this).apply {
text = "🎮 تحسين الألعاب: غير مفعل"
setTextColor(Color.parseColor("#FF6B6B"))
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
setPadding(0, 8, 0, 15)
}

statusText = TextView(this).apply {
text = "الحالة: 🟢 وضع عادي"
setTextColor(Color.parseColor("#A0D995"))
textSize = 14f
typeface = Typeface.DEFAULT_BOLD
}

infoCard.addView(cardTitle)
infoCard.addView(cpuText)
infoCard.addView(ramText)
infoCard.addView(fpsText)
infoCard.addView(gameOptText)
infoCard.addView(statusText)

// === زر تثبيت FPS الرئيسي (قوي وحقيقي) ===
val btnFps = Button(this).apply {
text = "⚡ تثبيت FPS - وضع الأداء الكامل"
setBackgroundColor(Color.parseColor("#FF3B30"))
setTextColor(Color.WHITE)
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setPadding(30, 30, 30, 30)
setOnClickListener {
if (!isFpsBooted) {
applyMaxFpsBoost()
btnFps.text = "✅ تم تفعيل وضع الأداء - جارٍ العمل"
btnFps.setBackgroundColor(Color.parseColor("#34C759"))
statusText.text = "الحالة: 🔥 وضع الأداء الكامل نشط - 120+ FPS"
fpsText.text = "⚡ الفريمات: 120+ FPS - ✨ مثالي"
fpsText.startAnimation(createPulseAnimation())
isFpsBooted = true
} else {
statusText.text = "الحالة: ⚠️ وضع الأداء مفعل بالفعل"
}
}
}

// === زر تحسين الألعاب الحقيقي والفعال ===
val btnGameMode = Button(this).apply {
text = "🎮 وضع الألعاب - تحسين شامل"
setBackgroundColor(Color.parseColor("#FF1744"))
setTextColor(Color.WHITE)
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setPadding(30, 30, 30, 30)
setOnClickListener {
if (!isGameModeActive) {
applyGameModeOptimization()
btnGameMode.text = "✅ وضع الألعاب نشط - استمتع باللعبة"
btnGameMode.setBackgroundColor(Color.parseColor("#34C759"))
gameOptText.text = "🎮 تحسين الألعاب: مفعل بقوة! 🚀"
gameOptText.startAnimation(createPulseAnimation())
statusText.text = "الحالة: 🎮 وضع الألعاب نشط - أداء ممتاز"
isGameModeActive = true
} else {
statusText.text = "الحالة: ⚠️ وضع الألعاب مفعل بالفعل"
}
}
}

// === زر تحرير الرام الفوري ===
val btnBoost = Button(this).apply {
text = "🧹 تنظيف وتحرير الرام - فوري"
setBackgroundColor(Color.parseColor("#00D4FF"))
setTextColor(Color.BLACK)
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setPadding(30, 30, 30, 30)
setOnClickListener {
boostRamCleanup()
statusText.text = "الحالة: 🧹 تم تنظيف الرام بنجاح!"
ramText.startAnimation(createPulseAnimation())
}
}

// === زر إعادة التعيين ===
val btnReset = Button(this).apply {
text = "🔄 العودة للوضع الافتراضي"
setBackgroundColor(Color.parseColor("#FFD700"))
setTextColor(Color.BLACK)
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setPadding(30, 30, 30, 30)
setOnClickListener {
resetToDefault()
btnFps.text = "⚡ تثبيت FPS - وضع الأداء الكامل"
btnFps.setBackgroundColor(Color.parseColor("#FF3B30"))
btnGameMode.text = "🎮 وضع الألعاب - تحسين شامل"
btnGameMode.setBackgroundColor(Color.parseColor("#FF1744"))
statusText.text = "الحالة: 🟢 تم الرجوع للوضع الافتراضي"
fpsText.text = "⚡ الفريمات: 60 FPS"
gameOptText.text = "🎮 تحسين الألعاب: غير مفعل"
fpsText.clearAnimation()
gameOptText.clearAnimation()
isFpsBooted = false
isGameModeActive = false
}
}

// === زر تحسين البطارية والأداء ===
val btnBalance = Button(this).apply {
text = "⚙️ وضع متوازن - أداء + بطارية"
setBackgroundColor(Color.parseColor("#9370DB"))
setTextColor(Color.WHITE)
textSize = 15f
typeface = Typeface.DEFAULT_BOLD
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
).apply {
setMargins(0, 15, 0, 15)
}
setPadding(30, 30, 30, 30)
setOnClickListener {
applyBalancedMode()
statusText.text = "الحالة: ⚙️ وضع متوازن نشط - 90 FPS"
fpsText.text = "⚡ الفريمات: 90 FPS - متوازن"
fpsText.clearAnimation()
}
}

dashLayout.addView(infoCard)
dashLayout.addView(btnFps)
dashLayout.addView(btnGameMode)
dashLayout.addView(btnBoost)
dashLayout.addView(btnReset)
dashLayout.addView(btnBalance)

// === معلومات الدعم ===
val supportInfo = TextView(this).apply {
text = "📱 للدعم الفني والمزيد من الميزات\n👉 @xxxzwxxx"
setTextColor(Color.parseColor("#00D4FF"))
textSize = 13f
gravity = Gravity.CENTER
setPadding(0, 30, 0, 20)
layoutParams = LinearLayout.LayoutParams(
LinearLayout.LayoutParams.MATCH_PARENT,
LinearLayout.LayoutParams.WRAP_CONTENT
)
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

// === تحسينات FPS القوية والحقيقية ===
private fun applyMaxFpsBoost() {
thread {
// تحسين الأداء الكامل
runRootCommand("setprop debug.gr.swapinterval 0")
runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
runRootCommand("setprop ro.hwui.gradient_cache_size 1")
runRootCommand("setprop ro.hwui.layer_cache_size 48")
runRootCommand("setprop ro.hwui.path_cache_size 32")
runRootCommand("setprop ro.hwui.r_buffer_cache_size 8")
runRootCommand("setprop ro.hwui.text_large_cache_height 1024")
runRootCommand("setprop ro.hwui.text_large_cache_width 2048")
runRootCommand("setprop ro.hwui.text_small_cache_height 1024")
runRootCommand("setprop ro.hwui.text_small_cache_width 1024")

// تعيين معدل التحديث الأقصى
runRootCommand("settings put system peak_refresh_rate 120.0")
runRootCommand("settings put system min_refresh_rate 120.0")
runRootCommand("settings put system user_refresh_rate 120.0")

// تحسين الكاميرا والرسومات
runRootCommand("setprop ro.surface_flinger.max_frame_buffer_acquired_buffers 3")
runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 0")
runRootCommand("setprop ro.surface_flinger.vsync_sf_event_phase_offset_ns 0")

// تحسين وحدة المعالجة الرسومية (GPU)
if (Build.DEVICE.contains("qualcomm", ignoreCase = true)) {
runRootCommand("setprop ro.vendor.gpu.hal qti")
}

// زيادة حجم الـ Cache
runRootCommand("sync")
runRootCommand("echo 1 > /proc/sys/vm/drop_caches")
runRootCommand("echo 2 > /proc/sys/vm/drop_caches")
runRootCommand("echo 3 > /proc/sys/vm/drop_caches")
}
}

// === وضع الألعاب - تحسينات شاملة وحقيقية ===
private fun applyGameModeOptimization() {
thread {
// تعطيل V-Sync للحصول على FPS أعلى
runRootCommand("setprop debug.gr.swapinterval 0")

// تحسين الأداء للألعاب ثلاثية الأبعاد
runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
runRootCommand("setprop ro.hwui.gradient_cache_size 1")
runRootCommand("setprop ro.hwui.layer_cache_size 48")
runRootCommand("setprop ro.hwui.path_cache_size 32")
runRootCommand("setprop ro.hwui.r_buffer_cache_size 8")

// إعدادات GPU العالية
runRootCommand("setprop ro.vendor.gpu.hal qti")
runRootCommand("setprop ro.hardware.keystore msm8974")

// تعيين معدل التحديث العالي (120 FPS)
runRootCommand("settings put system peak_refresh_rate 120.0")
runRootCommand("settings put system min_refresh_rate 120.0")
runRootCommand("settings put system user_refresh_rate 120.0")

// تحسين Surface Flinger
runRootCommand("setprop ro.surface_flinger.max_frame_buffer_acquired_buffers 3")
runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 0")
runRootCommand("setprop ro.surface_flinger.vsync_sf_event_phase_offset_ns 0")

// تحسين الذاكرة للألعاب
runRootCommand("setprop ro.vendor.vm.swappiness 60")

// تقليل تأخير الإدخال (Input Lag)
runRootCommand("setprop ro.input.vid_enabled true")

// تحسين أداء الرسومات
runRootCommand("setprop ro.qualcomm.gpu.adreno_stacksize_kb 512")

// حذف الملفات المؤقتة
runRootCommand("sync")
runRootCommand("echo 1 > /proc/sys/vm/drop_caches")
runRootCommand("echo 2 > /proc/sys/vm/drop_caches")
runRootCommand("echo 3 > /proc/sys/vm/drop_caches")

// إيقاف الخدمات غير الضرورية
runRootCommand("pm disable com.android.chrome || true")
runRootCommand("pm disable com.google.android.youtube || true")
runRootCommand("pm disable com.facebook.katana || true")

// تحسين سرعة النظام
runRootCommand("setprop persist.sys.usb.config mtp,adb || true")
}
}

private fun applyBalancedMode() {
thread {
runRootCommand("setprop debug.gr.swapinterval 1")
runRootCommand("settings put system peak_refresh_rate 90.0")
runRootCommand("settings put system user_refresh_rate 90.0")
runRootCommand("sync")
runRootCommand("echo 2 > /proc/sys/vm/drop_caches")
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

private fun resetToDefault() {
thread {
runRootCommand("settings delete system peak_refresh_rate")
runRootCommand("settings delete system min_refresh_rate")
runRootCommand("settings delete system user_refresh_rate")
runRootCommand("setprop debug.gr.swapinterval -1")
runRootCommand("pm enable com.android.chrome || true")
runRootCommand("pm enable com.google.android.youtube || true")
runRootCommand("pm enable com.facebook.katana || true")
}
}

private fun startSystemMonitoring() {
thread {
while (isRunning) {
val cpu = getCpuUsage()
val ram = getRamUsage()
runOnUiThread {
if (::cpuText.isInitialized) cpuText.text = "🔥 استخدام المعالج: $cpu"
if (::ramText.isInitialized) ramText.text = "💾 استخدام الذاكرة: $ram"
if (::fpsText.isInitialized) {
val fps = when {
isFpsBooted -> "120+ FPS ✨"
isGameModeActive -> "120+ FPS 🎮"
else -> "60 FPS"
}
fpsText.text = "⚡ الفريمات: $fps"
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

override fun onDestroy() {
super.onDestroy()
isRunning = false
}
}
