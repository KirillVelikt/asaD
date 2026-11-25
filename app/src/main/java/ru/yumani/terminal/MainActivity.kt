package ru.yumani.terminal

import android.content.Intent
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

object TerminalState {
    var code: String = ""
    @Volatile var status: String = "waiting" // waiting | active | success | fail
    @Volatile var amount: Int = 0
}

class MainActivity : AppCompatActivity() {
    private lateinit var tvCode: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvHint: TextView
    private var soundPool: SoundPool? = null
    private var sndJingle = 0
    private val exec = Executors.newCachedThreadPool()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        tvCode = findViewById(R.id.tvCode)
        tvStatus = findViewById(R.id.tvStatus)
        tvHint = findViewById(R.id.tvHint)

        if (TerminalState.code.isEmpty()) TerminalState.code = (100000..999999).random().toString()
        tvCode.text = TerminalState.code
        tvStatus.text = "ожидание пульта…"
        tvHint.text = "введите этот код в пульте на ПК,\nчтобы подключиться"

        initSounds()
        soundPool?.play(sndJingle, 0.8f, 0.8f, 1, 0, 1f)
        exec.execute { beaconLoop() }
        exec.execute { serverLoop() }
    }

    override fun onResume() {
        super.onResume()
        when (TerminalState.status) {
            "waiting" -> {
                tvStatus.text = "ожидание пульта…"
                tvCode.text = TerminalState.code
                tvCode.visibility = TextView.VISIBLE
                tvHint.visibility = TextView.VISIBLE
            }
            "active" -> { tvStatus.text = "идёт оплата…"; tvHint.visibility = TextView.GONE }
            "success" -> { tvStatus.text = "оплата прошла ✓"; tvCode.visibility = TextView.VISIBLE }
            "fail" -> { tvStatus.text = "оплата не прошла"; tvCode.visibility = TextView.VISIBLE }
        }
    }

    private fun initSounds() {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        sndJingle = soundPool!!.load(this, R.raw.jingle, 1)
    }

    private fun beaconLoop() {
        try {
            val ds = DatagramSocket()
            ds.broadcast = true
            while (true) {
                val msg = JSONObject().put("code", TerminalState.code).put("port", 8090).toString()
                val data = msg.toByteArray()
                ds.send(DatagramPacket(data, data.size, InetAddress.getByName("255.255.255.255"), 45665))
                Thread.sleep(2000)
            }
        } catch (_: Exception) { }
    }

    private fun serverLoop() {
        try {
            val ss = ServerSocket(8090)
            while (true) {
                val s = ss.accept()
                exec.execute { handle(s) }
            }
        } catch (_: Exception) { }
    }

    private fun handle(s: Socket) {
        try {
            s.soTimeout = 3000
            val line = BufferedReader(InputStreamReader(s.getInputStream())).readLine() ?: ""
            var response = """{"status":"error"}"""
            when {
                line.contains("\"ping\"") ->
                    response = JSONObject().put("code", TerminalState.code).put("status", "ok").toString()
                line.contains("\"pay\"") -> {
                    val amount = JSONObject(line).optInt("amount", 0)
                    TerminalState.amount = amount
                    TerminalState.status = "active"
                    runOnUiThread {
                        startActivity(Intent(this, PaymentActivity::class.java).putExtra("amount", amount))
                    }
                    response = """{"status":"started"}"""
                }
                line.contains("\"result\"") ->
                    response = JSONObject().put("result", TerminalState.status).toString()
                line.contains("\"close\"") -> {
                    TerminalState.status = "waiting"
                    response = """{"status":"closed"}"""
                }
            }
            s.getOutputStream().write(response.toByteArray())
            s.getOutputStream().flush()
            s.close()
        } catch (_: Exception) { }
    }
}
