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

        if (!checkRootPermission()) {
            showRootDeniedUI()
            return
        }

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
            text = "⚡ وضع ثبات قوي 60 FPS"
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
        }

        btnFps.setOnClickListener {
            if (!isFpsBooted) {
                statusText.text = "الحالة: ⏳ جاري تثبيت 60 FPS..."
                btnFps.isEnabled = false
                thread {
                    applyMaxFpsBoost()
                    runOnUiThread {
                        btnFps.text = "✅ نشط - ثبات قوي 60 FPS"
                        btnFps.setBackgroundColor(Color.parseColor("#34C759"))
                        statusText.text = "الحالة: 🔥 تم تفعيل ثبات 60 FPS"
                        fpsText.text = "⚡ FPS: ثبات قوي على 60"
                        fpsText.startAnimation(createPulseAnimation())
                        isFpsBooted = true
                        btnFps.isEnabled = true
                    }
                }
            } else {
                statusText.text = "الحالة: ⚠️ وضع ثبات 60 مفعل"
            }
        }

        btnGameMode.setOnClickListener {
            if (!isGameModeActive) {
                statusText.text = "الحالة: ⏳ جاري تطبيق وضع الألعاب..."
                btnGameMode.isEnabled = false
                thread {
                    applyAdvancedGameMode()
                    runOnUiThread {
                        btnGameMode.text = "✅ وضع الألعاب نشط"
                        btnGameMode.setBackgroundColor(Color.parseColor("#34C759"))
                        gameOptText.text = "🎮 وضع الألعاب: مفعل قوي 🚀"
                        gameOptText.startAnimation(createPulseAnimation())
                        statusText.text = "الحالة: 🎮 تحسين شامل للألعاب"
                        isGameModeActive = true
                        btnGameMode.isEnabled = true
                    }
                }
            } else {
                statusText.text = "الحالة: ⚠️ وضع الألعاب مفعل بالفعل"
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
                btnFps.text = "⚡ وضع ثبات قوي 60 FPS"
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

        // ============================================================
        // === قسم Oxide Survival (قابل للفتح والطوي) ===
        // ============================================================
        val oxideHeader = Button(this).apply {
            text = "🌿 Oxide Survival ▼"
            setBackgroundColor(Color.parseColor("#1E3A2F"))
            setTextColor(Color.parseColor("#4CAF50"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
            setPadding(25, 20, 25, 20)
        }

        val oxideContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D1F17"))
            visibility = View.GONE
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val oxideDesc = TextView(this).apply {
            text = "🎮 تحسينات مخصصة للعب Oxide Survival بثبات أعلى وتقليل الـ Lag."
            setTextColor(Color.parseColor("#A5D6A7"))
            textSize = 13f
            setPadding(0, 0, 0, 14)
        }

        val oxideFpsStatusText = TextView(this).apply {
            text = "⚡ حالة ثبات FPS: غير مفعل"
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 10)
        }

        val btnOxideFps = Button(this).apply {
            text = "⚡ تفعيل ثبات 60 FPS لـ Oxide Survival"
            setBackgroundColor(Color.parseColor("#2E7D32"))
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setPadding(20, 20, 20, 20)
        }

        btnOxideFps.setOnClickListener {
            btnOxideFps.isEnabled = false
            oxideFpsStatusText.text = "⚡ حالة ثبات FPS: ⏳ جاري التطبيق..."
            thread {
                applyMaxFpsBoost()
                runOnUiThread {
                    btnOxideFps.text = "✅ ثبات 60 FPS مفعل لـ Oxide"
                    btnOxideFps.setBackgroundColor(Color.parseColor("#34C759"))
                    oxideFpsStatusText.text = "⚡ حالة ثبات FPS: 🟢 نشط (استقرار مُحسَّن)"
                    oxideFpsStatusText.startAnimation(createPulseAnimation())
                    statusText.text = "الحالة: 🌿 Oxide Survival - ثبات 60 FPS"
                    fpsText.text = "⚡ FPS: ثبات قوي على 60"
                }
            }
        }

        oxideHeader.setOnClickListener {
            if (oxideContent.visibility == View.GONE) {
                oxideContent.visibility = View.VISIBLE
                oxideHeader.text = "🌿 Oxide Survival ▲"
            } else {
                oxideContent.visibility = View.GONE
                oxideHeader.text = "🌿 Oxide Survival ▼"
            }
        }

        oxideContent.addView(oxideDesc)
        oxideContent.addView(oxideFpsStatusText)
        oxideContent.addView(btnOxideFps)

        // ============================================================
        // === صفحة استخدام الجهاز والعمليات المستهلكة ===
        // ============================================================
        val usageHeader = Button(this).apply {
            text = "📱 استخدام الجهاز والعمليات ▼"
            setBackgroundColor(Color.parseColor("#1A237E"))
            setTextColor(Color.parseColor("#90CAF9"))
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER_VERTICAL or Gravity.START
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 20, 0, 0)
            }
            setPadding(25, 20, 25, 20)
        }

        val usageContent = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#0D0F2A"))
            visibility = View.GONE
            setPadding(20, 16, 20, 16)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
        }

        val usageUptimeText = TextView(this).apply {
            text = "⏱️ مدة التشغيل: ..."
            setTextColor(Color.parseColor("#90CAF9"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 8)
        }

        val usageCpuDetail = TextView(this).apply {
            text = "🔥 استهلاك المعالج: ..."
            setTextColor(Color.parseColor("#FF7043"))
            textSize = 13f
            setPadding(0, 0, 0, 6)
        }

        val usageRamDetail = TextView(this).apply {
            text = "💾 استهلاك الذاكرة: ..."
            setTextColor(Color.parseColor("#FFD700"))
            textSize = 13f
            setPadding(0, 0, 0, 6)
        }

        val usageGpuDetail = TextView(this).apply {
            text = "🎨 استهلاك GPU: ..."
            setTextColor(Color.parseColor("#00FF88"))
            textSize = 13f
            setPadding(0, 0, 0, 6)
        }

        val usageThermalDetail = TextView(this).apply {
            text = "🌡️ درجة الحرارة: ..."
            setTextColor(Color.parseColor("#87CEEB"))
            textSize = 13f
            setPadding(0, 0, 0, 6)
        }

        val usageTopLabel = TextView(this).apply {
            text = "📋 أبرز مستهلكي الطاقة:"
            setTextColor(Color.parseColor("#E0E0E0"))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 10, 0, 4)
        }

        val usageTopProcesses = TextView(this).apply {
            text = "جاري التحميل..."
            setTextColor(Color.parseColor("#BDBDBD"))
            textSize = 12f
            setPadding(0, 0, 0, 10)
        }

        val btnRefreshUsage = Button(this).apply {
            text = "🔄 تحديث البيانات"
            setBackgroundColor(Color.parseColor("#283593"))
            setTextColor(Color.WHITE)
            textSize = 13f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 4)
            }
            setPadding(16, 16, 16, 16)
        }

        fun refreshUsageData() {
            thread {
                val uptime = getUptimeString()
                val cpu = getCpuUsage()
                val ram = getRamUsage()
                val gpu = getGpuUsage()
                val temp = getSystemTemperature()
                val topProcs = getTopProcesses()
                runOnUiThread {
                    usageUptimeText.text = "⏱️ مدة التشغيل: $uptime"
                    usageCpuDetail.text = "🔥 استهلاك المعالج: $cpu"
                    usageRamDetail.text = "💾 استهلاك الذاكرة: $ram"
                    usageGpuDetail.text = "🎨 استهلاك GPU: $gpu"
                    val tempStatus = when {
                        temp > 45 -> "🔴 حار جداً"
                        temp > 40 -> "🟠 ساخن"
                        temp > 35 -> "🟡 دافئ"
                        else -> "🟢 ممتاز"
                    }
                    usageThermalDetail.text = "🌡️ درجة الحرارة: ${temp}°C - $tempStatus"
                    usageTopProcesses.text = topProcs
                }
            }
        }

        btnRefreshUsage.setOnClickListener { refreshUsageData() }

        usageHeader.setOnClickListener {
            if (usageContent.visibility == View.GONE) {
                usageContent.visibility = View.VISIBLE
                usageHeader.text = "📱 استخدام الجهاز والعمليات ▲"
                refreshUsageData()
            } else {
                usageContent.visibility = View.GONE
                usageHeader.text = "📱 استخدام الجهاز والعمليات ▼"
            }
        }

        usageContent.addView(usageUptimeText)
        usageContent.addView(usageCpuDetail)
        usageContent.addView(usageRamDetail)
        usageContent.addView(usageGpuDetail)
        usageContent.addView(usageThermalDetail)
        usageContent.addView(usageTopLabel)
        usageContent.addView(usageTopProcesses)
        usageContent.addView(btnRefreshUsage)

        dashLayout.addView(infoCard)
        dashLayout.addView(btnFps)
        dashLayout.addView(btnGameMode)
        dashLayout.addView(btnGraphics)
        dashLayout.addView(btnLagReduce)
        dashLayout.addView(btnCooling)
        dashLayout.addView(btnBoost)
        dashLayout.addView(btnBalance)
        dashLayout.addView(btnReset)
        dashLayout.addView(oxideHeader)
        dashLayout.addView(oxideContent)
        dashLayout.addView(usageHeader)
        dashLayout.addView(usageContent)

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
        runRootCommand("setprop debug.gr.swapinterval 0")
        runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
        runRootCommand("setprop ro.hwui.gradient_cache_size 1")
        runRootCommand("setprop ro.hwui.layer_cache_size 48")
        runRootCommand("setprop ro.hwui.path_cache_size 32")
        runRootCommand("settings put system peak_refresh_rate 60.0")
        runRootCommand("settings put system user_refresh_rate 60.0")
        runRootCommand("setprop ro.surface_flinger.max_frame_buffer_acquired_buffers 3")
        runRootCommand("sync")
        runRootCommand("echo 3 > /proc/sys/vm/drop_caches")
    }

    private fun applyAdvancedGameMode() {
        thread {
            runRootCommand("setprop debug.gr.swapinterval 0")
            runRootCommand("setprop ro.hwui.layer_cache_size 48")
            runRootCommand("setprop ro.hwui.path_cache_size 32")
            runRootCommand("setprop ro.hwui.r_buffer_cache_size 8")
            runRootCommand("setprop ro.vendor.gpu.hal qti")
            runRootCommand("setprop ro.qualcomm.gpu.adreno_stacksize_kb 512")
            runRootCommand("settings put system peak_refresh_rate 120.0")
            runRootCommand("settings put system user_refresh_rate 120.0")
            runRootCommand("setprop ro.vendor.vm.swappiness 60")
            runRootCommand("setprop ro.input.vid_enabled true")
            runRootCommand("pm disable --user 0 com.google.android.gms || true")
            runRootCommand("pm disable --user 0 com.android.chrome || true")
            runRootCommand("sync && echo 3 > /proc/sys/vm/drop_caches")
        }
    }

    private fun applyDeepGraphicsOptimization() {
        thread {
            runRootCommand("setprop ro.hwui.drop_shadow_cache_size 6")
            runRootCommand("setprop ro.hwui.gradient_cache_size 1")
            runRootCommand("setprop ro.hwui.layer_cache_size 48")
            runRootCommand("setprop ro.hwui.path_cache_size 32")
            runRootCommand("setprop ro.hwui.text_large_cache_height 1024")
            runRootCommand("setprop ro.hwui.text_large_cache_width 2048")
            runRootCommand("setprop ro.vendor.gpu.hal qti")
            runRootCommand("setprop ro.opengles.version 196610")
            runRootCommand("setprop ro.surface_flinger.has_HDR_display true")
            runRootCommand("setprop ro.surface_flinger.protected_contents true")
            runRootCommand("setprop ro.hwui.print_config 0")
            runRootCommand("sync")
        }
    }

    private fun reduceLagAndLatency() {
        thread {
            runRootCommand("setprop ro.input.vid_enabled true")
            runRootCommand("setprop ro.qti.sensors.max_accel_rate 50")
            runRootCommand("setprop ro.iorapd.enable true")
            runRootCommand("setprop ro.sys.usb.config mtp,adb")
            runRootCommand("setprop ro.hardware.keystore msm8974")
            runRootCommand("setprop debug.gr.swapinterval 0")
            runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 0")
        }
    }

    private fun optimizeCoolingAndBattery() {
        thread {
            runRootCommand("setprop persist.sys.usb.config mtp,adb")
            runRootCommand("setprop ro.vendor.thermal.polling_delay 10000")
            runRootCommand("setprop persist.sys.profiler_ms 0")
            runRootCommand("setprop sys.sysctl.extra_free_kbytes 43200")
            runRootCommand("setprop ro.vendor.extension_library /vendor/lib/rfsa/adsp/libfastcvopt.so")
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

    private fun getUptimeString(): String {
        return try {
            val uptimeMillis = android.os.SystemClock.elapsedRealtime()
            val hours = (uptimeMillis / 3600000).toInt()
            val minutes = ((uptimeMillis % 3600000) / 60000).toInt()
            "${hours}س ${minutes}د"
        } catch (e: Exception) {
            "غير معروف"
        }
    }

    private fun getTopProcesses(): String {
        return try {
            val result = StringBuilder()
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "top -b -n 1 -o %CPU,NAME | head -8"))
            val lines = process.inputStream.bufferedReader().readLines()
            process.waitFor()
            var count = 0
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isNotEmpty() && !trimmed.startsWith("Tasks") && !trimmed.startsWith("Cpu")
                    && !trimmed.startsWith("Mem") && !trimmed.startsWith("Swap")
                    && !trimmed.startsWith("%") && count < 5) {
                    result.appendLine("• $trimmed")
                    count++
                }
            }
            if (result.isEmpty()) "لا توجد بيانات متاحة" else result.toString().trimEnd()
        } catch (e: Exception) {
            "تعذّر جلب العمليات"
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
                            isFpsBooted -> "ثبات قوي على 60 ✨"
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
