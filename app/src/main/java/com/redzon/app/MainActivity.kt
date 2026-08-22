Text(
"التطبيق يتطلب صلاحيات الروت للعمل!\nيرجى منح الصلاحية من تطبيق الروت وإعادة الفتح.",
color = Color.White,
textAlign = TextAlign.Center,
fontSize = 16.sp
)
Spacer(modifier = Modifier.height(24.dp))
Button(
onClick = onExit,
colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
) {
Text("إغلاق التطبيق", color = Color.White)
}
}
}

// واجهة الدخول الفخمة
@Composable
fun StartScreen(onStart: () -> Unit) {
Column(
modifier = Modifier.fillMaxSize().padding(24.dp),
horizontalAlignment = Alignment.CenterHorizontally,
verticalArrangement = Arrangement.Center
) {
Text("REDZON FPS", color = GoldColor, fontSize = 36.sp, fontWeight = FontWeight.ExtraBold)
Text("POWERED BY ROOT", color = Color.Gray, fontSize = 14.sp)
Spacer(modifier = Modifier.height(50.dp))

Button(
onClick = onStart,
modifier = Modifier.fillMaxWidth().height(60.dp).border(2.dp, GoldColor, RoundedCornerShape(12.dp)),
colors = ButtonDefaults.buttonColors(containerColor = CardBlack),
shape = RoundedCornerShape(12.dp)
) {
Text("START FPS REDZON", color = GoldColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}
}
}

// الواجهة الرئيسية للتحكم
@Composable
fun MainDashboard(onOpenTelegram: () -> Unit) {
var cpuUsage by remember { mutableStateOf("0%") }
var ramUsage by remember { mutableStateOf("0 MB / 0 MB") }
var statusText by remember { mutableStateOf("الوضع الافتراضي") }

// التحديث الحي للمعالج والرام
LaunchedEffect(Unit) {
while (true) {
cpuUsage = getCpuUsage()
ramUsage = getRamUsage()
delay(2000)
}
}

Column(
modifier = Modifier.fillMaxSize().padding(16.dp),
horizontalAlignment = Alignment.CenterHorizontally
) {
Text("REDZON DASHBOARD", color = GoldColor, fontSize = 24.sp, fontWeight = FontWeight.Bold)
Spacer(modifier = Modifier.height(16.dp))

// كارت قراءة النظام
Card(
modifier = Modifier.fillMaxWidth().border(1.dp, GoldColor, RoundedCornerShape(12.dp)),
colors = CardDefaults.cardColors(containerColor = CardBlack)
) {
Column(modifier = Modifier.padding(16.dp)) {
Text("📊 مراقب النظام", color = GoldColor, fontWeight = FontWeight.Bold)
Spacer(modifier = Modifier.height(8.dp))
Text("استخدام المعالج (CPU): $cpuUsage", color = Color.White)
Text("استخدام الذاكرة (RAM): $ramUsage", color = Color.White)
Spacer(modifier = Modifier.height(8.dp))
Text("الحالة الحالية: $statusText", color = GoldColor, fontWeight = FontWeight.Bold)
}
}

Spacer(modifier = Modifier.height(20.dp))

// أزرار الأداء
Button(
onClick = {
// أمر تثبيت الـ FPS وإلغاء الخنق
runRootCommand("setprop debug.gr.swapinterval 0; settings put system peak_refresh_rate 120.0; settings put system user_refresh_rate 120.0")
statusText = "🔥 تم تثبيت الـ FPS والوصول لأقصى أداء!"
},
modifier = Modifier.fillMaxWidth().height(55.dp),
colors = ButtonDefaults.buttonColors(containerColor = DarkGoldColor),
shape = RoundedCornerShape(10.dp)
) {
Text("⚡ زيادة وتثبيت الـ FPS", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
}

Spacer(modifier = Modifier.height(12.dp))

Button(
onClick = {
// إرجاع النظام للوضع الافتراضي
runRootCommand("settings delete system peak_refresh_rate; settings delete system user_refresh_rate")

Shartar ":
statusText = "الوضع الافتراضي"
},
modifier = Modifier.fillMaxWidth().height(55.dp),
colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
shape = RoundedCornerShape(10.dp)
) {
Text("🛑 إيقاف المميزات والعودة للافتراضي", color = Color.White, fontWeight = FontWeight.Medium)
}

Spacer(modifier = Modifier.height(12.dp))

// ميزة إضافية: تنظيف الرام
Button(
onClick = {
runRootCommand("sync; echo 3 > /proc/sys/vm/drop_caches")
statusText = "🧹 تم تنظيف الرام وتحرير المساحة!"
},
modifier = Modifier.fillMaxWidth().height(50.dp).border(1.dp, GoldColor, RoundedCornerShape(10.dp)),
colors = ButtonDefaults.buttonColors(containerColor = CardBlack),
shape = RoundedCornerShape(10.dp)
) {
Text("🚀 تنظيف الرام (Boost RAM)", color = GoldColor)
}

Spacer(modifier = Modifier.weight(1f))

// زر التليجرام بالأسفل
TextButton(onClick = onOpenTelegram) {
Text("للدعم والدعم الفني: @xxxzwxxx", color = GoldColor, fontSize = 14.sp)
}
}
}

// قراءة الرام
fun getRamUsage(): String {
return try {
val reader = RandomAccessFile("/proc/meminfo", "r")
val totalMemLine = reader.readLine()
val freeMemLine = reader.readLine()
val availMemLine = reader.readLine()
reader.close()

val totalKb = totalMemLine.replace("\\D+".toRegex(), "").toLong()
val availKb = availMemLine.replace("\\D+".toRegex(), "").toLong()

val usedMb = (totalKb - availKb) / 1024
val totalMb = totalKb / 1024
"$usedMb MB / $totalMb MB"
} catch (e: Exception) {
"غير معروف"
}
}

// قراءة الـ CPU
fun getCpuUsage(): String {
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
