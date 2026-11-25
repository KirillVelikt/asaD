package ru.yumani.terminal;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class SuccessActivity extends AppCompatActivity {

    public static Intent intent(Context c, boolean ok) {
        Intent i = new Intent(c, SuccessActivity.class);
        i.putExtra("ok", ok);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_success);

        boolean ok = getIntent().getBooleanExtra("ok", true);
        String amount = TerminalState.amount.replace('.', ',') + " ₽";

        findViewById(R.id.root).setBackgroundResource(
                ok ? R.drawable.bg_success : R.drawable.bg_fail);
        ((TextView) findViewById(R.id.tvTitle)).setText(
                ok ? "ОПЛАТА ПРИНЯТА" : "ОТКАЗ");
        ((TextView) findViewById(R.id.tvAmount)).setText(amount);
        ((TextView) findViewById(R.id.tvRrn)).setText(
                ok ? "RRN: " + TerminalState.rrn + "  •  чек отправлен"
                   : "операция отклонена");

        play(ok);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent back = new Intent(this, MainActivity.class);
            back.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(back);
            finish();
        }, 3500);
    }

    private void play(boolean ok) {
        try {
            int res = ok ? R.raw.success : R.raw.fail;
            MediaPlayer.create(this, res).start();
        } catch (Exception ignored) {}
        if (!ok) {
            try {
                Uri u = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                android.media.RingtoneManager.getRingtone(this, u).play();
            } catch (Exception ignored) {}
        }
    }
}
