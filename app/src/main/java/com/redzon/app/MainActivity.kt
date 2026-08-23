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

    // ── monitor TextViews (only valid after dashboard is shown) ──
    private lateinit var statusText: TextView
    private lateinit var cpuText: TextView
    private lateinit var ramText: TextView
    private lateinit var fpsText: TextView
    private lateinit var gameOptText: TextView
    private lateinit var gpuText: TextView
    private lateinit var thermalText: TextView

    // ── root state ──
    @Volatile private var isRunning = true
    @Volatile private var isFpsBooted = false
    @Volatile private var isGameModeActive = false

    // ─────────────────────────────────────────────────────────────
    //  Lifecycle
    // ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!checkRootPermission()) {
            showRootDeniedUI()
            return
        }
        showMainUI()
        startSystemMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
    }

    // ─────────────────────────────────────────────────────────────
    //  Root helpers
    // ─────────────────────────────────────────────────────────────

    private fun checkRootPermission(): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", "id")).waitFor() == 0
    } catch (e: Exception) { false }

    private fun runRootCommand(cmd: String): Boolean = try {
        Runtime.getRuntime().exec(arrayOf("su", "-c", cmd)).waitFor() == 0
    } catch (e: Exception) { false }

    // ─────────────────────────────────────────────────────────────
    //  Root-denied screen
    // ─────────────────────────────────────────────────────────────

    private fun showRootDeniedUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(BG_DARK)
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
        }
        root.addView(label("⚠️ صلاحيات الروت مطلوبة", COLOR_RED, 22f, bold = true).also {
            it.gravity = Gravity.CENTER
        })
        root.addView(label(
            "التطبيق يتطلب صلاحيات الروت للعمل.\nيرجى منح الصلاحية من تطبيق الروت وإعادة الفتح.",
            COLOR_MUTED, 15f
        ).also {
            it.gravity = Gravity.CENTER
            it.setPadding(0, dp(16), 0, dp(32))
        })
        root.addView(actionButton("إغلاق التطبيق", COLOR_RED, Color.WHITE) {
            finishAffinity()
            Process.killProcess(Process.myPid())
        })
        setContentView(root)
    }

    // ─────────────────────────────────────────────────────────────
    //  Main UI
    // ─────────────────────────────────────────────────────────────

    private fun showMainUI() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(BG_DARK)
            isFillViewport = true
        }

        val main = vLayout(pad = dp(20)).also { it.gravity = Gravity.CENTER_HORIZONTAL }

        // ── Landing / Hero ──────────────────────────────────────
        val heroSection = buildHeroSection()

        // ── Dashboard (hidden until hero button tapped) ─────────
        val dashSection = buildDashSection()
        dashSection.visibility = View.GONE

        heroSection.second.setOnClickListener {
            heroSection.first.visibility = View.GONE
            heroSection.second.visibility = View.GONE
            dashSection.visibility = View.VISIBLE
            dashSection.startAnimation(fadeIn(400))
        }

        main.addView(heroSection.first)
        main.addView(heroSection.second)
        main.addView(dashSection)
        main.addView(footerView())

        scroll.addView(main)
        setContentView(scroll)
    }

    /** Returns Pair<heroLayout, startButton> */
    private fun buildHeroSection(): Pair<LinearLayout, Button> {
        val hero = vLayout(pad = dp(24)).also { it.gravity = Gravity.CENTER }

        val badge = label("● ROOT ACTIVE", Color.parseColor("#34C759"), 11f, bold = true).also {
            it.gravity = Gravity.CENTER
            it.setPadding(dp(12), dp(6), dp(12), dp(6))
            it.setBackgroundColor(Color.parseColor("#1A2B1A"))
            it.letterSpacing = 0.12f
        }

        val appName = label("REDZON", COLOR_ACCENT, 40f, bold = true).also {
            it.gravity = Gravity.CENTER
            it.letterSpacing = 0.08f
        }

        val tagline = label("تحسين أداء الجهاز بصلاحيات الروت", COLOR_MUTED, 14f).also {
            it.gravity = Gravity.CENTER
            it.setPadding(0, dp(8), 0, dp(4))
        }

        val pillsRow = hLayout().also { row ->
            row.gravity = Gravity.CENTER
            listOf("⚡ أداء عالٍ", "🎮 ألعاب", "🌡️ تبريد").forEach { txt ->
                row.addView(pill(txt))
            }
        }

        badge.startAnimation(fadeIn(800))
        appName.startAnimation(fadeScaleIn(900))
        tagline.startAnimation(fadeIn(1000))
        pillsRow.startAnimation(slideUp(1100))

        hero.addView(badge, marginParams(bottom = dp(16)))
        hero.addView(appName)
        hero.addView(tagline)
        hero.addView(pillsRow, marginParams(top = dp(16), bottom = dp(32)))

        val startBtn = actionButton(
            "🚀  ابدأ — تفعيل لوحة التحكم",
            Color.parseColor("#FF3B30"), Color.WHITE, size = 16f
        ) {}
        startBtn.layoutParams = marginParams(top = dp(8), bottom = dp(8), fill = true)
        startBtn.startAnimation(slideUp(1200))

        return Pair(hero, startBtn)
    }

    private fun buildDashSection(): LinearLayout {
        val dash = vLayout()

        // ── Monitor card ─────────────────────────────────────────
        dash.addView(sectionHeader("📊  مراقب النظام"))
        val monitorCard = card()

        cpuText     = monitorRow("🔥  CPU", "جارٍ القراءة…", COLOR_GREEN)
        ramText     = monitorRow("💾  RAM", "جارٍ القراءة…", COLOR_GOLD)
        gpuText     = monitorRow("🎨  GPU", "جارٍ القراءة…", Color.parseColor("#00FF88"))
        thermalText = monitorRow("🌡️  الحرارة", "جارٍ القراءة…", Color.parseColor("#87CEEB"))
        fpsText     = monitorRow("⚡  وضع FPS", "عادي", COLOR_RED)
        gameOptText = monitorRow("🎮  وضع الألعاب", "غير مفعل", Color.parseColor("#FF6B6B"))
        statusText  = monitorRow("●  الحالة", "وضع عادي", Color.parseColor("#A0D995"))

        listOf(cpuText, ramText, gpuText, thermalText, fpsText, gameOptText, statusText)
            .forEach { monitorCard.addView(it) }
        dash.addView(monitorCard, cardMargin())

        // ── Uptime / Usage card ──────────────────────────────────
        dash.addView(sectionHeader("🖥️  استخدام الجهاز"))
        dash.addView(buildUsageCard(), cardMargin())

        // ── Performance actions ───────────────────────────────────
        dash.addView(sectionHeader("⚡  تحسينات الأداء"))
        dash.addView(buildPerformanceCard(), cardMargin())

        // ── Oxide Survival (collapsible) ──────────────────────────
        dash.addView(buildOxideSection(), cardMargin())

        // ── System utilities ──────────────────────────────────────
        dash.addView(sectionHeader("🛠️  أدوات النظام"))
        dash.addView(buildUtilitiesCard(), cardMargin())

        return dash
    }

    // ─────────────────────────────────────────────────────────────
    //  Dashboard sub-cards
    // ─────────────────────────────────────────────────────────────

    private fun buildUsageCard(): LinearLayout {
        val c = card()

        val uptimeTv = label("⏱️  وقت التشغيل: جارٍ الحساب…", COLOR_MUTED, 13f)
        val processTv = label("📋  أعلى العمليات استهلاكاً:\nجارٍ الجلب…", COLOR_MUTED, 12f)
        processTv.setPadding(0, dp(8), 0, 0)

        c.addView(uptimeTv)
        c.addView(processTv)

        // refresh in background
        thread {
            val uptime = getUptimeString()
            val procs  = getTopProcesses()
            runOnUiThread {
                uptimeTv.text  = "⏱️  وقت التشغيل: $uptime"
                processTv.text = "📋  أعلى العمليات استهلاكاً:\n$procs"
            }
        }

        val refreshBtn = actionButton("🔄  تحديث", Color.parseColor("#2C3A5C"), COLOR_ACCENT, size = 13f) {
            uptimeTv.text  = "⏱️  وقت التشغيل: جارٍ الحساب…"
            processTv.text = "📋  أعلى العمليات استهلاكاً:\nجارٍ الجلب…"
            thread {
                val uptime = getUptimeString()
                val procs  = getTopProcesses()
                runOnUiThread {
                    uptimeTv.text  = "⏱️  وقت التشغيل: $uptime"
                    processTv.text = "📋  أعلى العمليات استهلاكاً:\n$procs"
                }
            }
        }
        refreshBtn.layoutParams = marginParams(top = dp(12), fill = true)
        c.addView(refreshBtn)
        return c
    }

    private fun buildPerformanceCard(): LinearLayout {
        val c = card()

        val btnFps = actionButton("⚡  تثبيت FPS — أقصى أداء", COLOR_RED, Color.WHITE) {}
        btnFps.layoutParams = marginParams(bottom = dp(10), fill = true)
        btnFps.setOnClickListener {
            if (!isFpsBooted) {
                applyMaxFpsBoost()
                btnFps.text = "✅  FPS معزَّز — نشط"
                btnFps.setBackgroundColor(COLOR_GREEN)
                fpsText.text = "⚡  وضع FPS: أقصى أداء ✨"
                fpsText.startAnimation(pulse())
                statusText.text = "●  الحالة: وضع الأداء الكامل 🔥"
                isFpsBooted = true
            } else {
                statusText.text = "●  الحالة: وضع الأداء مفعل بالفعل"
            }
        }

        val btnGameMode = actionButton("🎮  وضع الألعاب الاحترافي", Color.parseColor("#FF1744"), Color.WHITE) {}
        btnGameMode.layoutParams = marginParams(bottom = dp(10), fill = true)
        btnGameMode.setOnClickListener {
            if (!isGameModeActive) {
                applyAdvancedGameMode()
                btnGameMode.text = "✅  وضع الألعاب — نشط"
                btnGameMode.setBackgroundColor(COLOR_GREEN)
                gameOptText.text = "🎮  وضع الألعاب: مفعل قوي 🚀"
                gameOptText.startAnimation(pulse())
                statusText.text = "●  الحالة: تحسين شامل للألعاب 🎮"
                isGameModeActive = true
            } else {
                statusText.text = "●  الحالة: وضع الألعاب مفعل بالفعل"
            }
        }

        val btnGraphics = actionButton("🎨  تحسين الرسومات العميق", Color.parseColor("#7C3AED"), Color.WHITE) {
            applyDeepGraphicsOptimization()
            gpuText.text = "🎨  GPU: تحسين عميق نشط!"
            gpuText.startAnimation(pulse())
            statusText.text = "●  الحالة: تحسين رسومات عميق 🎨"
        }
        btnGraphics.layoutParams = marginParams(bottom = dp(10), fill = true)

        val btnLag = actionButton("⚡  تقليل التأخير (Input Lag)", Color.parseColor("#EC4899"), Color.WHITE) {
            reduceLagAndLatency()
            statusText.text = "●  الحالة: تقليل التأخير مفعل ⚡"
        }
        btnLag.layoutParams = marginParams(bottom = dp(10), fill = true)

        val btnBalance = actionButton("⚙️  وضع متوازن", Color.parseColor("#6D4AFF"), Color.WHITE) {
            applyBalancedMode()
            fpsText.text = "⚡  وضع FPS: متوازن 90"
            statusText.text = "●  الحالة: وضع متوازن ⚙️"
        }
        btnBalance.layoutParams = marginParams(bottom = dp(10), fill = true)

        val btnReset = actionButton("🔄  إعادة الضبط الافتراضي", Color.parseColor("#FFD700"), Color.parseColor("#111")) {
            resetToDefault()
            btnFps.text = "⚡  تثبيت FPS — أقصى أداء"
            btnFps.setBackgroundColor(COLOR_RED)
            btnGameMode.text = "🎮  وضع الألعاب الاحترافي"
            btnGameMode.setBackgroundColor(Color.parseColor("#FF1744"))
            fpsText.text = "⚡  وضع FPS: عادي"
            gameOptText.text = "🎮  وضع الألعاب: غير مفعل"
            statusText.text = "●  الحالة: وضع عادي 🟢"
            fpsText.clearAnimation(); gameOptText.clearAnimation()
            isFpsBooted = false; isGameModeActive = false
        }
        btnReset.layoutParams = marginParams(fill = true)

        listOf(btnFps, btnGameMode, btnGraphics, btnLag, btnBalance, btnReset).forEach { c.addView(it) }
        return c
    }

    private fun buildUtilitiesCard(): LinearLayout {
        val c = card()

        val btnCooling = actionButton("❄️  تحسين التبريد والبطارية", Color.parseColor("#06B6D4"), Color.parseColor("#0A0A0A")) {
            optimizeCoolingAndBattery()
            thermalText.startAnimation(pulse())
            statusText.text = "●  الحالة: تحسين التبريد نشط ❄️"
        }
        btnCooling.layoutParams = marginParams(bottom = dp(10), fill = true)

        val btnRam = actionButton("🧹  تنظيف الرام الفوري", Color.parseColor("#00D4FF"), Color.parseColor("#0A0A0A")) {
            boostRamCleanup()
            ramText.startAnimation(pulse())
            statusText.text = "●  الحالة: تم تنظيف الرام ✅"
        }
        btnRam.layoutParams = marginParams(fill = true)

        c.addView(btnCooling)
        c.addView(btnRam)
        return c
    }

    /** Collapsible Oxide Survival section */
    private fun buildOxideSection(): LinearLayout {
        val wrapper = vLayout()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(Color.parseColor("#1A1E36"))
            setPadding(dp(18), dp(14), dp(18), dp(14))
        }

        val headerTitle = label("🟠  Oxide Survival — تحسينات مخصصة", COLOR_GOLD, 15f, bold = true)
        headerTitle.layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        val chevron = label("▼", COLOR_MUTED, 13f)

        header.addView(headerTitle)
        header.addView(chevron)

        val body = card()
        body.visibility = View.GONE

        body.addView(label(
            "يعمل هذا القسم على تحسين استقرار معدل الإطارات عند 60 FPS داخل Oxide Survival.\n" +
            "لا يضمن قفلًا صارمًا على 60 FPS في جميع الظروف، لكنه يُقلل من التذبذبات ويرفع الاتساق.",
            COLOR_MUTED, 12f
        ).also { it.setPadding(0, 0, 0, dp(14)) })

        val btn60fps = actionButton("🎯  تحسين ثبات 60 FPS (Oxide)", Color.parseColor("#E65100"), Color.WHITE) {
            applyOxide60FpsStability()
            statusText.text = "●  الحالة: تحسين ثبات Oxide مفعل 🟠"
            fpsText.text = "⚡  وضع FPS: ثبات 60 (Oxide)"
        }
        btn60fps.layoutParams = marginParams(fill = true)
        body.addView(btn60fps)

        var expanded = false
        header.setOnClickListener {
            expanded = !expanded
            body.visibility = if (expanded) View.VISIBLE else View.GONE
            chevron.text = if (expanded) "▲" else "▼"
            if (expanded) body.startAnimation(fadeIn(250))
        }

        wrapper.addView(header)
        wrapper.addView(body)
        return wrapper
    }

    // ─────────────────────────────────────────────────────────────
    //  Root actions
    // ─────────────────────────────────────────────────────────────

    private fun applyMaxFpsBoost() = thread {
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

    private fun applyAdvancedGameMode() = thread {
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

    private fun applyDeepGraphicsOptimization() = thread {
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

    private fun reduceLagAndLatency() = thread {
        runRootCommand("setprop ro.input.vid_enabled true")
        runRootCommand("setprop ro.qti.sensors.max_accel_rate 50")
        runRootCommand("setprop ro.iorapd.enable true")
        runRootCommand("setprop ro.sys.usb.config mtp,adb")
        runRootCommand("setprop ro.hardware.keystore msm8974")
        runRootCommand("setprop debug.gr.swapinterval 0")
        runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 0")
    }

    private fun optimizeCoolingAndBattery() = thread {
        runRootCommand("setprop persist.sys.usb.config mtp,adb")
        runRootCommand("setprop ro.vendor.thermal.polling_delay 10000")
        runRootCommand("setprop persist.sys.profiler_ms 0")
        runRootCommand("setprop sys.sysctl.extra_free_kbytes 43200")
        runRootCommand("setprop ro.vendor.extension_library /vendor/lib/rfsa/adsp/libfastcvopt.so")
        runRootCommand("sync && echo 2 > /proc/sys/vm/drop_caches")
    }

    private fun boostRamCleanup() = thread {
        runRootCommand("sync")
        runRootCommand("echo 1 > /proc/sys/vm/drop_caches")
        runRootCommand("echo 2 > /proc/sys/vm/drop_caches")
        runRootCommand("echo 3 > /proc/sys/vm/drop_caches")
        runRootCommand("killall com.android.systemui || true")
        runRootCommand("am trim-caches 100M || true")
    }

    private fun applyBalancedMode() = thread {
        runRootCommand("setprop debug.gr.swapinterval 1")
        runRootCommand("settings put system peak_refresh_rate 90.0")
        runRootCommand("settings put system user_refresh_rate 90.0")
        runRootCommand("sync && echo 2 > /proc/sys/vm/drop_caches")
    }

    private fun resetToDefault() = thread {
        runRootCommand("settings delete system peak_refresh_rate")
        runRootCommand("settings delete system user_refresh_rate")
        runRootCommand("setprop debug.gr.swapinterval -1")
        runRootCommand("pm enable --user 0 com.google.android.gms || true")
        runRootCommand("pm enable --user 0 com.android.chrome || true")
    }

    /** Oxide Survival 60 FPS stability — reduces jitter, improves consistency */
    private fun applyOxide60FpsStability() = thread {
        runRootCommand("settings put system peak_refresh_rate 60.0")
        runRootCommand("settings put system user_refresh_rate 60.0")
        runRootCommand("setprop debug.gr.swapinterval 1")
        runRootCommand("setprop ro.surface_flinger.vsync_event_phase_offset_ns 2000000")
        runRootCommand("setprop ro.surface_flinger.sf_vsync_event_phase_offset_ns 6000000")
        runRootCommand("setprop ro.hwui.layer_cache_size 48")
        runRootCommand("setprop ro.hwui.path_cache_size 32")
        runRootCommand("sync && echo 3 > /proc/sys/vm/drop_caches")
    }

    // ─────────────────────────────────────────────────────────────
    //  System monitoring
    // ─────────────────────────────────────────────────────────────

    private fun startSystemMonitoring() = thread {
        while (isRunning) {
            val cpu  = getCpuUsage()
            val ram  = getRamUsage()
            val gpu  = getGpuUsage()
            val temp = getSystemTemperature()

            runOnUiThread {
                if (::cpuText.isInitialized)     cpuText.text = "🔥  CPU: $cpu"
                if (::ramText.isInitialized)     ramText.text = "💾  RAM: $ram"
                if (::gpuText.isInitialized)     gpuText.text = "🎨  GPU: $gpu"
                if (::thermalText.isInitialized) {
                    val lbl = when {
                        temp > 45 -> "🔴 حار جداً"
                        temp > 40 -> "🟠 ساخن"
                        temp > 35 -> "🟡 دافئ"
                        else      -> "🟢 ممتاز"
                    }
                    thermalText.text = "🌡️  الحرارة: ${temp}°C — $lbl"
                }
                if (::fpsText.isInitialized && !isFpsBooted && !isGameModeActive) {
                    fpsText.text = "⚡  وضع FPS: عادي"
                }
            }
            Thread.sleep(2500)
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Data helpers
    // ─────────────────────────────────────────────────────────────

    private fun getRamUsage(): String = try {
        val r = RandomAccessFile("/proc/meminfo", "r")
        val totalLine = r.readLine(); r.readLine(); val availLine = r.readLine(); r.close()
        val total = totalLine.replace("\\D+".toRegex(), "").toLong()
        val avail = availLine.replace("\\D+".toRegex(), "").toLong()
        "${(total - avail) / 1024} MB / ${total / 1024} MB"
    } catch (e: Exception) { "غير معروف" }

    private fun getCpuUsage(): String = try {
        val lines = File("/proc/stat").readLines()
        if (lines.isNotEmpty()) {
            val toks  = lines[0].split("\\s+".toRegex())
            val idle  = toks[4].toLong()
            val total = toks.slice(1..7).sumOf { it.toLong() }
            "${((total - idle) * 100 / total).toInt()}%"
        } else "N/A"
    } catch (e: Exception) { "N/A" }

    private fun getGpuUsage(): String = try {
        val freq = File("/sys/class/kgsl/kgsl-3d0/devfreq/cur_freq").readText().trim().toLong() / 1_000_000
        "${freq} MHz"
    } catch (e: Exception) { "N/A" }

    private fun getSystemTemperature(): Int = try {
        File("/sys/class/thermal/thermal_zone0/temp").readText().trim().toInt() / 1000
    } catch (e: Exception) { 35 }

    private fun getUptimeString(): String = try {
        val seconds = File("/proc/uptime").readText().trim().split(" ")[0].toDouble().toLong()
        val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
        "${h}س ${m}د ${s}ث"
    } catch (e: Exception) { "غير متاح" }

    private fun getTopProcesses(): String = try {
        val lines = File("/proc").listFiles()
            ?.filter { it.isDirectory && it.name.all { c -> c.isDigit() } }
            ?.mapNotNull { pid ->
                runCatching {
                    val cmdline = File(pid, "cmdline").readText().replace('\u0000', ' ').trim()
                    val statLine = File(pid, "stat").readText()
                    val statParts = statLine.split(" ")
                    val utime = statParts.getOrNull(13)?.toLongOrNull() ?: 0L
                    val stime = statParts.getOrNull(14)?.toLongOrNull() ?: 0L
                    Pair(cmdline.ifEmpty { "?" }, utime + stime)
                }.getOrNull()
            }
            ?.sortedByDescending { it.second }
            ?.take(5)
            ?.mapIndexed { i, (name, ticks) ->
                val shortName = name.substringAfterLast("/").take(30)
                "${i + 1}. $shortName  (${ticks} ticks)"
            }
            ?.joinToString("\n") ?: "غير متاح"
        lines
    } catch (e: Exception) { "غير متاح" }

    // ─────────────────────────────────────────────────────────────
    //  UI factory helpers
    // ─────────────────────────────────────────────────────────────

    private fun vLayout(pad: Int = 0) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
        if (pad > 0) setPadding(pad, pad, pad, pad)
    }

    private fun hLayout() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(Color.parseColor("#141829"))
        setPadding(dp(18), dp(18), dp(18), dp(18))
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun sectionHeader(title: String) = label(title, COLOR_ACCENT, 13f, bold = true).also {
        it.letterSpacing = 0.06f
        it.setPadding(dp(4), dp(20), dp(4), dp(8))
    }

    private fun monitorRow(key: String, value: String, valueColor: Int): TextView {
        return label("$key:  $value", valueColor, 13f, bold = true).also {
            it.setPadding(0, dp(5), 0, dp(5))
        }
    }

    private fun label(
        text: String,
        color: Int,
        size: Float,
        bold: Boolean = false
    ) = TextView(this).apply {
        this.text = text
        setTextColor(color)
        textSize = size
        if (bold) typeface = Typeface.DEFAULT_BOLD
        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun pill(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(COLOR_MUTED)
        textSize = 11f
        setPadding(dp(10), dp(4), dp(10), dp(4))
        setBackgroundColor(Color.parseColor("#1E2540"))
        layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).also { it.setMargins(dp(4), 0, dp(4), 0) }
    }

    private fun actionButton(
        text: String,
        bg: Int,
        fg: Int,
        size: Float = 14f,
        onClick: () -> Unit
    ) = Button(this).apply {
        this.text = text
        setBackgroundColor(bg)
        setTextColor(fg)
        textSize = size
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(20), dp(14), dp(20), dp(14))
        setOnClickListener { onClick() }
    }

    private fun footerView() = label("📱  للدعم: @xxxzwxxx", COLOR_ACCENT, 12f).also {
        it.gravity = Gravity.CENTER
        it.setPadding(0, dp(24), 0, dp(24))
        it.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/xxxzwxxx")))
        }
    }

    // ─────────────────────────────────────────────────────────────
    //  Layout param helpers
    // ─────────────────────────────────────────────────────────────

    private fun marginParams(
        top: Int = 0, bottom: Int = 0, fill: Boolean = false
    ) = LinearLayout.LayoutParams(
        if (fill) LinearLayout.LayoutParams.MATCH_PARENT else LinearLayout.LayoutParams.WRAP_CONTENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    ).also { it.setMargins(0, top, 0, bottom) }

    private fun cardMargin() = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
    ).also { it.setMargins(0, 0, 0, dp(4)) }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    // ─────────────────────────────────────────────────────────────
    //  Animation helpers
    // ─────────────────────────────────────────────────────────────

    private fun fadeIn(duration: Long) = AlphaAnimation(0f, 1f).apply { this.duration = duration }

    private fun fadeScaleIn(duration: Long): AnimationSet {
        val set = AnimationSet(true).apply { this.duration = duration }
        set.addAnimation(AlphaAnimation(0f, 1f).apply { this.duration = duration })
        set.addAnimation(ScaleAnimation(0.85f, 1f, 0.85f, 1f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            this.duration = duration
        })
        return set
    }

    private fun slideUp(duration: Long): AnimationSet {
        val set = AnimationSet(true).apply { this.duration = duration }
        set.addAnimation(TranslateAnimation(
            TranslateAnimation.RELATIVE_TO_SELF, 0f, TranslateAnimation.RELATIVE_TO_SELF, 0f,
            TranslateAnimation.RELATIVE_TO_SELF, 0.4f, TranslateAnimation.RELATIVE_TO_SELF, 0f
        ).apply { this.duration = duration })
        set.addAnimation(AlphaAnimation(0f, 1f).apply { this.duration = duration })
        return set
    }

    private fun pulse(): AnimationSet {
        val set = AnimationSet(true).apply {
            duration = 900
            repeatCount = android.view.animation.Animation.INFINITE
            repeatMode  = android.view.animation.Animation.REVERSE
        }
        set.addAnimation(ScaleAnimation(1f, 1.04f, 1f, 1.04f,
            ScaleAnimation.RELATIVE_TO_SELF, 0.5f, ScaleAnimation.RELATIVE_TO_SELF, 0.5f).apply {
            duration = 900
        })
        return set
    }

    // ─────────────────────────────────────────────────────────────
    //  Color constants
    // ─────────────────────────────────────────────────────────────

    companion object {
        private val BG_DARK    = Color.parseColor("#09111D")
        private val COLOR_ACCENT = Color.parseColor("#00D4FF")
        private val COLOR_GOLD = Color.parseColor("#FFD700")
        private val COLOR_RED  = Color.parseColor("#FF3B30")
        private val COLOR_GREEN = Color.parseColor("#34C759")
        private val COLOR_MUTED = Color.parseColor("#8A9BB5")
    }
}
