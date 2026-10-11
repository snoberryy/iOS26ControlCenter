package com.snoberry.ios26cc

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Color
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(10, 12, 20)
        window.navigationBarColor = Color.rgb(10, 12, 20)

        val page = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(24), dp(24), dp(24))
            setBackgroundColor(Color.rgb(10, 12, 20))
        }
        val title = TextView(this).apply {
            text = "iOS 26 Control Center"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
        }
        val desc = TextView(this).apply {
            text = "Liquid Glass-inspired Android overlay\\n\\n1. Grant overlay permission\\n2. Allow modify system settings for brightness\\n3. Allow camera permission for flashlight\\n4. Start the control center"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, dp(18), 0, dp(24))
        }
        val start = Button(this).apply {
            text = "Set up and start"
            setOnClickListener { setupAndStart() }
        }
        page.addView(title)
        page.addView(desc)
        page.addView(start)
        setContentView(page)
    }

    private fun setupAndStart() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Enable 'Display over other apps', then return and tap Start again.", Toast.LENGTH_LONG).show()
            return
        }
        if (!Settings.System.canWrite(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:$packageName")))
            Toast.makeText(this, "Allow modify system settings, then return and tap Start again.", Toast.LENGTH_LONG).show()
            return
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 45)
            return
        }
        startService(Intent(this, ControlCenterService::class.java))
        Toast.makeText(this, "Control Center started. Swipe down on the small handle at the top-right.", Toast.LENGTH_LONG).show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 45 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupAndStart()
        } else {
            Toast.makeText(this, "Camera permission is needed for the flashlight tile.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onResume() {
        super.onResume()
    }
}
