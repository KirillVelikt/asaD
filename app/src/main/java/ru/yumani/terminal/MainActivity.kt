package ru.yumani.terminal

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val code = generateCode()
        findViewById<TextView>(R.id.tvCode).text = code
        findViewById<TextView>(R.id.tvStatus).text = "ожидание пульта…"

        // кнопка оплаты для теста (можно убрать потом)
        findViewById<android.widget.Button>(R.id.btnTest).setOnClickListener {
            startActivity(
                Intent(this, PaymentActivity::class.java)
                    .putExtra("amount", 198)
                    .putExtra("code", code)
            )
        }
    }

    private fun generateCode(): String {
        return (100000..999999).random().toString()
    }
}
