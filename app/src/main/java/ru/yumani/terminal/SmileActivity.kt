package ru.yumani.terminal

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SmileActivity : AppCompatActivity() {
    private val req = 777

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_smile)
        findViewById<TextView>(R.id.tvSmileHint).text = "наведите камеру и улыбнитесь :)"
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, req)
        } else {
            processMock()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == req) {
            if (resultCode == Activity.RESULT_OK) {
                (data?.extras?.get("data") as? Bitmap)?.let {
                    findViewById<ImageView>(R.id.ivPhoto).setImageBitmap(it)
                }
            }
            processMock()
        }
    }

    private fun processMock() {
        findViewById<TextView>(R.id.tvSmileHint).text = "улыбка найдена ✓  идёт оплата…"
        Handler(Looper.getMainLooper()).postDelayed({
            TerminalState.status = "success"
            finish()
        }, 2000)
    }
}
