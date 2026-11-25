package ru.yumani.terminal

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class PaymentActivity : AppCompatActivity() {
    private var overlayView: View? = null
    private var bmpQr: Bitmap? = null
    private var soundPool: SoundPool? = null
    private var sndSuccess = 0
    private var sndFail = 0
    private var sndTick = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)
        val amount = intent.getIntExtra("amount", 0)
        findViewById<TextView>(R.id.tvAmount).text = "$amount ₽"
        findViewById<TextView>(R.id.tvTerm).text = "Терминал: ${TerminalState.code}"
        findViewById<TextView>(R.id.tvBig).text = "платите как вам удобно"
        makeQr(amount)
        initSounds()
        setupOverlay()

        findViewById<View>(R.id.cardSmile).setOnClickListener {
            soundPool?.play(sndTick, 0.6f, 0.6f, 1, 0, 1f)
            startActivity(Intent(this, SmileActivity::class.java))
        }
        findViewById<View>(R.id.cardQr).setOnClickListener { showBigQr() }
        findViewById<View>(R.id.btnClose).setOnClickListener { finishPayment(false) }
    }

    override fun onResume() {
        super.onResume()
        if (TerminalState.status == "success") finishPayment(true)
        if (TerminalState.status == "fail") finishPayment(false)
    }

    @SuppressLint("SetTextI18n")
    private fun finishPayment(success: Boolean) {
        TerminalState.status = if (success) "success" else "fail"
        runOnUiThread {
            findViewById<View>(R.id.cardSmile).visibility = View.GONE
            findViewById<View>(R.id.cardQr).visibility = View.GONE
            findViewById<View>(R.id.btnClose).visibility = View.GONE
            findViewById<View>(R.id.tvAmount).visibility = View.GONE
            val tv = findViewById<TextView>(R.id.tvBig)
            if (success) {
                tv.text = "оплата прошла ✓"
                tv.setTextColor(Color.parseColor("#7CFC9B"))
            } else {
                tv.text = "оплата не прошла"
                tv.setTextColor(Color.parseColor("#FF8A80"))
            }
            soundPool?.play(if (success) sndSuccess else sndFail, 1f, 1f, 1, 0, 1f)
            removeOverlay()
        }
    }

    private fun makeQr(amount: Int) {
        try {
            val content = "yumani://pay?terminal=${TerminalState.code}&amount=$amount"
            val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 512, 512)
            val bmp = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565)
            for (x in 0 until 512) for (y in 0 until 512)
                bmp.setPixel(x, y, if (matrix[x, y]) Color.WHITE else Color.rgb(18, 24, 22))
            bmpQr = bmp
            findViewById<ImageView>(R.id.ivQr).setImageBitmap(bmp)
        } catch (_: Exception) { }
    }

    private fun showBigQr() {
        soundPool?.play(sndTick, 0.6f, 0.6f, 1, 0, 1f)
        val iv = ImageView(this)
        iv.setImageBitmap(bmpQr)
        iv.setPadding(32, 32, 32, 32)
        AlertDialog.Builder(this)
            .setTitle("покажите QR покупателю")
            .setView(iv)
            .setPositiveButton("готово", null)
            .show()
    }

    private fun setupOverlay() {
        if (Settings.canDrawOverlays(this)) { addOverlay(); return }
        AlertDialog.Builder(this)
            .setTitle("защита от сворачивания")
            .setMessage("Чтобы экран оплаты не пропал случайно, разрешите приложению показывать поверх других окон.")
            .setPositiveButton("разрешить") { _, _ ->
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                addOverlay()
            }
            .setNegativeButton("позже") { _, _ -> }
            .show()
    }

    private fun addOverlay() {
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
            )
            params.gravity = Gravity.TOP
            val tv = TextView(this)
            tv.text = "●  идёт оплата — не выходите с экрана"
            tv.setTextColor(Color.WHITE)
            tv.textSize = 16f
            tv.gravity = Gravity.CENTER
            tv.setPadding(24, 48, 24, 48)
            tv.setBackgroundColor(0xCC0E7A3F.toInt())
            overlayView = tv
            wm.addView(tv, params)
        } catch (_: Exception) { }
    }

    private fun removeOverlay() {
        try {
            overlayView?.let { (getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it) }
        } catch (_: Exception) { }
        overlayView = null
    }

    override fun onDestroy() {
        super.onDestroy()
        removeOverlay()
        soundPool?.release()
    }
}
