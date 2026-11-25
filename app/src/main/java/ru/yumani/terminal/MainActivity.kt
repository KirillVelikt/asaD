package ru.yumani.terminal

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class PaymentActivity : AppCompatActivity() {

    private var amount: Int = 0
    private var code: String = ""
    private var successPlayer: MediaPlayer? = null
    private var failPlayer: MediaPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment)

        amount = intent.getIntExtra("amount", 0)
        code = intent.getStringExtra("code") ?: ""

        findViewById<TextView>(R.id.tvAmount).text = "$amount ₽"
        findViewById<TextView>(R.id.tvTerminal).text = "Терминал: $code"

        // звуки инициализируем лениво, при первом воспроизведении
        successPlayer = MediaPlayer.create(this, R.raw.success)
        failPlayer = MediaPlayer.create(this, R.raw.fail)
    }

    /** Вызывается, когда пульт подтвердил оплату */
    fun onPaymentResult(success: Boolean) {
        if (success) {
            successPlayer?.start()
            findViewById<TextView>(R.id.tvStatus).text = "оплата прошла ✓"
        } else {
            failPlayer?.start()
            findViewById<TextView>(R.id.tvStatus).text = "оплата не прошла"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        successPlayer?.release()
        failPlayer?.release()
    }
}
