package ru.yumani.terminal;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements TerminalServer.Listener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (TerminalState.code.isEmpty()) {
            TerminalState.code = String.format("%06d", new Random().nextInt(1000000));
        }
        ((TextView) findViewById(R.id.tvCode)).setText(TerminalState.code);
        TerminalServer.reset();
        TerminalServer.start(this);
    }

    @Override
    public void onAmount(String amount) {
        runOnUiThread(() -> startActivity(
                PaymentActivity.intent(this, amount)));
    }

    @Override
    public void onCancel() {
        runOnUiThread(() -> { /* экран оплаты уже закрыт или открыт — он сам отработает */ });
    }

    @Override
    public void onClient(boolean connected) {
        runOnUiThread(() -> {
            TextView st = findViewById(R.id.tvStatus);
            if (st != null) st.setText(connected ? "пульт подключён" : "ожидание пульта…");
        });
    }

    @Override
    protected void onDestroy() {
        TerminalServer.stop();
        super.onDestroy();
    }
}
