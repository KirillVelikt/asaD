package ru.yumani.terminal;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import java.util.HashMap;

public class PaymentActivity extends AppCompatActivity {

    public static Intent intent(Context c, String amount) {
        Intent i = new Intent(c, PaymentActivity.class);
        i.putExtra("amount", amount);
        return i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment);

        String amount = getIntent().getStringExtra("amount");
        if (amount == null || amount.isEmpty()) amount = "0.00";
        String shown = amount.replace('.', ',') + " ₽";
        ((TextView) findViewById(R.id.tvAmount)).setText(shown);

        try {
            ImageView iv = findViewById(R.id.ivQr);
            iv.setImageBitmap(makeQr("https://yumani.demo/pay/" + TerminalState.code + "?amount=" + amount));
        } catch (Exception ignored) {}

        findViewById(R.id.tvClose).setOnClickListener(v -> {
            TerminalServer.reset();
            finish();
        });
        findViewById(R.id.btnSmile).setOnClickListener(v -> success());

        setupNfc();
    }

    private Bitmap makeQr(String text) {
        QRCodeWriter w = new QRCodeWriter();
        try {
            java.util.Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 1);
            com.google.zxing.common.BitMatrix m =
                    w.encode(text, BarcodeFormat.QR_CODE, 640, 640, hints);
            Bitmap bmp = Bitmap.createBitmap(m.getWidth(), m.getHeight(), Bitmap.Config.RGB_565);
            for (int x = 0; x < m.getWidth(); x++)
                for (int y = 0; y < m.getHeight(); y++)
                    bmp.setPixel(x, y, m.get(x, y) ? Color.BLACK : Color.WHITE);
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    private void setupNfc() {
        NfcAdapter nfc = NfcAdapter.getDefaultAdapter(this);
        if (nfc == null) {
            Toast.makeText(this, "NFC недоступен — оплата кнопкой или QR", Toast.LENGTH_LONG).show();
            return;
        }
        nfc.enableReaderMode(this, tag -> runOnUiThread(() -> onTag(tag)),
                NfcAdapter.FLAG_READER_NFC_A | NfcAdapter.FLAG_READER_NFC_B
                        | NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK, null);
    }

    private void onTag(Tag tag) {
        byte[] uid = tag.getId();
        boolean looksLikeCard = uid != null && (uid.length == 4 || uid.length == 7 || uid.length == 10);
        if (looksLikeCard) success(); else declined();
    }

    private void success() {
        TerminalServer.markPaid();
        startActivity(SuccessActivity.intent(this, true));
        finish();
    }

    private void declined() {
        TerminalServer.markDeclined();
        startActivity(SuccessActivity.intent(this, false));
        finish();
    }

    @Override
    public void onBackPressed() {
        TerminalServer.reset();
        super.onBackPressed();
    }
}
