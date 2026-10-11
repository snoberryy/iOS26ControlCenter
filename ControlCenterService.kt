package com.snoberry.ios26cc

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import android.content.ActivityNotFoundException

class ControlCenterService : Service() {
    private lateinit var wm: WindowManager
    private var handle: View? = null
    private var panel: View? = null
    private var torchOn = false
    private val white = Color.WHITE
    private val muted = Color.rgb(220, 224, 235)
    private val audio by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    override fun onBind(intent: Intent?): IBinder? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showHandle()
    }

    override fun onDestroy() {
        remove(handle); remove(panel)
        super.onDestroy()
    }

    private fun remove(view: View?) {
        if (view != null) try { wm.removeView(view) } catch (_: Exception) {}
    }

    private fun overlayParams(width: Int, height: Int, gravity: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width, height, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { this.gravity = gravity }
    }

    private fun showHandle() {
        if (handle != null) return
        val grip = TextView(this).apply {
            text = "⌄"
            textSize = 19f
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            background = rounded(Color.argb(210, 45, 48, 58), 18)
            elevation = dp(8).toFloat()
        }
        var downY = 0f
        grip.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { downY = e.rawY; true }
                MotionEvent.ACTION_UP -> {
                    if (e.rawY - downY > dp(12) || e.rawY - downY < dp(-12)) showPanel()
                    else showPanel()
                    true
                }
                else -> true
            }
        }
        handle = grip
        // Android keeps the status bar above ordinary app overlays. This handle is placed just below it.
        val statusBar = resources.getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }?.let { resources.getDimensionPixelSize(it) } ?: dp(26)
        val p = overlayParams(dp(54), dp(32), Gravity.TOP or Gravity.RIGHT).apply {
            y = statusBar
            x = dp(8)
        }
        try { wm.addView(grip, p) } catch (_: Exception) { handle = null }
    }

    private fun rounded(color: Int, radius: Int, stroke: Int = Color.argb(55,255,255,255)): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(dp(1), stroke)
        }
    }

    private fun glass(color: Int = Color.argb(218, 27, 30, 41), radius: Int = 27) =
        rounded(color, radius, Color.argb(80,255,255,255))

    private fun text(s: String, size: Float, color: Int = white, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = s; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif", Typeface.BOLD)
            gravity = Gravity.CENTER_VERTICAL
        }

    private fun column() = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
    private fun row() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun tile(symbol: String, label: String, active: Boolean = false, action: (() -> Unit)? = null): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(7), dp(9), dp(7), dp(8))
            background = if (active) rounded(Color.rgb(231, 235, 245), 20, Color.argb(100,255,255,255)) else glass()
            isClickable = true
            isFocusable = true
        }
        val icon = text(symbol, 23f, if (active) Color.rgb(22,25,34) else white, true).apply { gravity = Gravity.CENTER }
        val name = text(label, 10.5f, if (active) Color.rgb(22,25,34) else muted).apply { gravity = Gravity.CENTER; maxLines = 1 }
        box.addView(icon, LinearLayout.LayoutParams(-1, dp(32)))
        box.addView(name, LinearLayout.LayoutParams(-1, dp(20)))
        box.setOnClickListener { action?.invoke() }
        return box
    }

    private fun addTileRow(parent: LinearLayout, tiles: List<View>) {
        val r = row()
        tiles.forEachIndexed { i, v ->
            val lp = LinearLayout.LayoutParams(0, dp(78), 1f)
            if (i != 0) lp.leftMargin = dp(8)
            r.addView(v, lp)
        }
        parent.addView(r)
    }

    private fun showPanel() {
        if (panel != null) { closePanel(); return }
        remove(handle); handle = null

        val scrim = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(34), dp(14), dp(18))
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(Color.rgb(8,10,18), Color.rgb(28,31,43), Color.rgb(12,14,22))
            ).apply { cornerRadius = dp(32).toFloat(); setStroke(dp(1), Color.argb(85,255,255,255)) }
            alpha = 0f
        }
        val top = row()
        val time = text(java.text.SimpleDateFormat("EEE, d MMM", java.util.Locale.getDefault()).format(java.util.Date()), 15f, muted)
        top.addView(time, LinearLayout.LayoutParams(0, dp(34), 1f))
        val close = text("Done  ×", 14f, white, true).apply {
            gravity = Gravity.CENTER
            background = rounded(Color.argb(65,255,255,255), 18)
            setPadding(dp(12), 0, dp(12), 0)
            setOnClickListener { closePanel() }
        }
        top.addView(close, LinearLayout.LayoutParams(dp(76), dp(34)))
        scrim.addView(top)
        val heading = text("Control Center", 25f, white, true).apply { setPadding(dp(2), dp(10), 0, dp(12)) }
        scrim.addView(heading)

        val mainRow = row()
        val connectivity = column().apply {
            background = glass(Color.argb(224, 42,45,58), 25)
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        val wifi = tile("⌁", "Wi-Fi", true) { openIntent("android.settings.panel.action.WIFI") }
        val bt = tile("ᛒ", "Bluetooth") { openIntent("android.settings.BLUETOOTH_SETTINGS") }
        val air = tile("✈", "Airplane") { openIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) }
        val data = tile("▥", "Internet") { openIntent("android.settings.panel.action.INTERNET_CONNECTIVITY") }
        addTileRow(connectivity, listOf(wifi, bt))
        val gap = View(this); connectivity.addView(gap, LinearLayout.LayoutParams(1, dp(7)))
        addTileRow(connectivity, listOf(air, data))
        mainRow.addView(connectivity, LinearLayout.LayoutParams(0, dp(174), 1.2f))

        val side = column().apply { setPadding(dp(8), 0, 0, 0) }
        val brightness = sliderCard("☼", "Brightness", 1, 255, currentBrightness()) { value ->
            try { Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value) } catch (_: Exception) {}
        }
        side.addView(brightness, LinearLayout.LayoutParams(-1, dp(82)))
        val gap2 = View(this); side.addView(gap2, LinearLayout.LayoutParams(1, dp(8)))
        val volume = sliderCard("♫", "Volume", 0, audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC), audio.getStreamVolume(AudioManager.STREAM_MUSIC)) { value ->
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, value, 0)
        }
        side.addView(volume, LinearLayout.LayoutParams(-1, dp(82)))
        mainRow.addView(side, LinearLayout.LayoutParams(0, dp(174), 0.8f))
        scrim.addView(mainRow)

        val gap3 = View(this); scrim.addView(gap3, LinearLayout.LayoutParams(1, dp(10)))
        addTileRow(scrim, listOf(
            tile("▣", "Flashlight") { toggleTorch() },
            tile("◉", "Rotation") { openIntent(Settings.ACTION_DISPLAY_SETTINGS) },
            tile("◐", "Dark mode") { Toast.makeText(this, "Use your phone's Display settings for dark mode.", Toast.LENGTH_SHORT).show() },
            tile("◌", "Settings") { openIntent(Settings.ACTION_SETTINGS) }
        ))
        val gap4 = View(this); scrim.addView(gap4, LinearLayout.LayoutParams(1, dp(10)))

        val music = column().apply {
            background = glass(Color.argb(225, 48,50,63), 24)
            setPadding(dp(14), dp(11), dp(14), dp(12))
        }
        val musicTop = row()
        val album = TextView(this).apply {
            text = "♫"; textSize = 27f; gravity = Gravity.CENTER; setTextColor(white)
            background = rounded(Color.rgb(91,93,112), 13)
        }
        musicTop.addView(album, LinearLayout.LayoutParams(dp(52), dp(52)))
        val info = column().apply { setPadding(dp(11), dp(2), 0, 0) }
        info.addView(text("Not Playing", 15f, white, true))
        info.addView(text("Audio controls", 12f, muted))
        musicTop.addView(info, LinearLayout.LayoutParams(0, dp(52), 1f))
        musicTop.addView(text("AirPlay  ◉", 12f, muted))
        music.addView(musicTop)
        val playback = row().apply { gravity = Gravity.CENTER }
        listOf("↶", "▶", "↷").forEachIndexed { index, s ->
            val b = text(s, if (index == 1) 25f else 20f, white, true).apply {
                gravity = Gravity.CENTER
                background = rounded(Color.argb(50,255,255,255), 24)
                setOnClickListener {
                    if (index == 1) openIntent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                    else Toast.makeText(this@ControlCenterService, "Use the media notification for track controls.", Toast.LENGTH_SHORT).show()
                }
            }
            val lp = LinearLayout.LayoutParams(dp(48), dp(42)); lp.leftMargin = dp(7); lp.rightMargin = dp(7)
            playback.addView(b, lp)
        }
        music.addView(playback)
        scrim.addView(music)
        val hint = text("Swipe down on the top-right handle to open", 11f, Color.LTGRAY).apply {
            gravity = Gravity.CENTER; setPadding(0, dp(10), 0, 0)
        }
        scrim.addView(hint)

        val scroll = android.widget.ScrollView(this).apply {
            isFillViewport = true
            addView(scrim)
        }
        panel = scroll
        val p = overlayParams(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, Gravity.TOP or Gravity.START).apply {
            flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING
        }
        try {
            wm.addView(scroll, p)
            scroll.animate().alpha(1f).setDuration(180).start()
        } catch (e: Exception) {
            panel = null
            Toast.makeText(this, "Could not display overlay: ${e.message}", Toast.LENGTH_LONG).show()
            showHandle()
        }
    }

    private fun sliderCard(symbol: String, label: String, min: Int, max: Int, initial: Int, changed: (Int) -> Unit): LinearLayout {
        val card = column().apply {
            background = glass(Color.argb(225, 49,52,66), 22)
            setPadding(dp(10), dp(7), dp(10), dp(4))
        }
        val head = row()
        head.addView(text(symbol, 17f, white, true))
        val title = text(label, 10.5f, muted)
        title.setPadding(dp(5), 0, 0, 0)
        head.addView(title)
        card.addView(head, LinearLayout.LayoutParams(-1, dp(23)))
        val seek = SeekBar(this).apply {
            this.max = (max - min).coerceAtLeast(1)
            progress = (initial - min).coerceIn(0, this.max)
            thumbTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressTintList = android.content.res.ColorStateList.valueOf(Color.WHITE)
            progressBackgroundTintList = android.content.res.ColorStateList.valueOf(Color.argb(100,255,255,255))
            setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) { if (fromUser) changed(progress + min) }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            })
        }
        card.addView(seek, LinearLayout.LayoutParams(-1, dp(37)))
        return card
    }

    private fun currentBrightness(): Int = try {
        Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS).coerceIn(1,255)
    } catch (_: Exception) { 130 }

    private fun toggleTorch() {
        try {
            val cm = getSystemService(CAMERA_SERVICE) as CameraManager
            val id = cm.cameraIdList.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
            if (id == null) { Toast.makeText(this, "No flashlight found.", Toast.LENGTH_SHORT).show(); return }
            torchOn = !torchOn
            cm.setTorchMode(id, torchOn)
        } catch (e: Exception) {
            torchOn = false
            Toast.makeText(this, "Flashlight unavailable: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openIntent(action: String) {
        try {
            val intent = Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: Exception) {
            Toast.makeText(this, "This setting isn't available on this Android version.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun closePanel() {
        val old = panel
        panel = null
        if (old != null) {
            old.animate().alpha(0f).setDuration(140).withEndAction {
                remove(old)
                showHandle()
            }.start()
        } else showHandle()
    }
}
