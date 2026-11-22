package com.mykhailo.ytwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView status;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Color.BLACK);
        int p = (int) (28 * getResources().getDisplayMetrics().density);
        l.setPadding(p, p, p, p);

        TextView t = new TextView(this);
        t.setText("Mono Music Widget");
        t.setTextColor(Color.WHITE);
        t.setTextSize(24);
        t.setGravity(Gravity.CENTER);

        status = new TextView(this);
        status.setTextColor(0xFFBBBBBB);
        status.setTextSize(15);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, p / 2, 0, p / 2);

        Button btn = new Button(this);
        btn.setText("Дати доступ до медіа");
        btn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        TextView hint = new TextView(this);
        hint.setTextColor(0xFF888888);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        hint.setPadding(0, p / 2, 0, 0);
        hint.setText("Далі: довге натискання на робочому столі → Віджети → Mono Music.\n" +
            "Доступ потрібен лише щоб читати, що грає в YouTube Music. Інтернет застосунку не потрібен.");

        l.addView(t); l.addView(status); l.addView(btn); l.addView(hint);
        setContentView(l);
    }

    @Override protected void onResume() {
        super.onResume();
        boolean ok = Media.hasAccess(this);
        status.setText(ok ? "✓ Доступ є" : "✗ Доступу ще немає");
        MusicWidget.renderAll(this, Media.find(this));
    }
}
