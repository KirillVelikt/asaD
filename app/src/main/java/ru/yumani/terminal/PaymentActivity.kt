package ru.yumani.terminal

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

class PaymentActivity : AppCompatActivity() {

    private var amount = 0
    private var code = ""
    private var successPlayer: MediaPlayer? = null
    private var failPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_payment)

        amount = intent.getIntExtra("amount", 0)
        code = intent.getStringExtra("code") ?: ""

        findViewById<TextView>(R.id.tvAmount).text = "$amount ₽"
        findViewById<TextView>(R.id.tvTerminal).text = "Терминал: $code"

        successPlayer = MediaPlayer.create(this, R.raw.success)
        failPlayer = MediaPlayer.create(this, R.raw.fail)

        val payload = "yumani://pay?terminal=$code&amount=$amount"
        val size = 512
        val matrix = QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (matrix[x, y]) 0xFF1A1A1D.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        findViewById<ImageView>(R.id.ivQr).setImageBitmap(bmp)
    }

    fun onPaymentResult(success: Boolean) {
        if (success) {
            successPlayer?.start()
            findViewById<TextView>(R.id.tvStatus).text = "оплата прошла ✓"
        } else {
            failPlayer?.start()
            findViewById<TextView>(R.id.tvStatus).text = "оплата не прошла"
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        // блокируем выход во время оплаты
    }

    override fun onDestroy() {
        super.onDestroy()
        successPlayer?.release()
        failPlayer?.release()
    }
}
