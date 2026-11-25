package ru.yumani.terminal;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {
    private MediaPlayer player;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        try {
            player = MediaPlayer.create(this, R.raw.jingle);
            if (player != null) player.start();
        } catch (Exception ignored) {}
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }, 2600);
    }

    @Override
    protected void onDestroy() {
        if (player != null) { try { player.release(); } catch (Exception ignored) {} }
        super.onDestroy();
    }
}
