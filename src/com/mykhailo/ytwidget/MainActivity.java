package com.mykhailo.ytwidget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String CHANNEL_URL = "https://t.me/s_mykhailo_dev";
    private static final String AUTHOR_URL = "https://t.me/savchenko_mykhailo";
    private static final String REPO_URL = "https://github.com/s-mykhailo/mono-music-widget";

    private TextView status, update;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setGravity(Gravity.CENTER);
        l.setBackgroundColor(Color.BLACK);
        int p = dp(28);
        l.setPadding(p, p, p, p);

        TextView t = text("Mono Music Widget", 24, Color.WHITE);

        status = text("", 15, 0xFFBBBBBB);
        status.setPadding(0, p / 2, 0, p / 2);

        Button btn = new Button(this);
        btn.setText("Дати доступ до медіа");
        btn.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));

        TextView hint = text("Далі: довге натискання на робочому столі → Віджети → Mono Music " +
            "(або Mono Music Color для кольорового варіанта).\n" +
            "Доступ потрібен лише щоб читати, що грає в YouTube Music.", 13, 0xFF888888);
        hint.setPadding(0, p / 2, 0, 0);

        update = text("", 13, 0xFF4FC3F7);
        update.setPadding(0, p / 2, 0, 0);
        update.setVisibility(View.GONE);

        TextView repo = link("GitHub: s-mykhailo/mono-music-widget", REPO_URL);
        TextView channel = link("Канал: @s_mykhailo_dev", CHANNEL_URL);
        TextView author = link("Автор: @savchenko_mykhailo", AUTHOR_URL);

        l.addView(t);
        l.addView(status);
        l.addView(btn);
        l.addView(hint);
        l.addView(update);
        l.addView(space(p));
        l.addView(repo);
        l.addView(channel);
        l.addView(author);
        setContentView(l);
    }

    @Override protected void onResume() {
        super.onResume();
        boolean ok = Media.hasAccess(this);
        status.setText(ok ? "✓ Доступ є" : "✗ Доступу ще немає");
        MusicWidget.renderAll(this, Media.find(this));
        UpdateChecker.check(this, (version, url) -> {
            update.setText("Є новіша версія " + version + " - натисни, щоб завантажити");
            update.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
            update.setVisibility(View.VISIBLE);
        });
    }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private TextView text(String s, int sp, int color) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        tv.setGravity(Gravity.CENTER);
        return tv;
    }

    private TextView link(String s, String url) {
        TextView tv = text(s, 12, 0xFF8AB4F8);
        tv.setPadding(0, dp(4), 0, 0);
        tv.setOnClickListener(v -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))));
        return tv;
    }

    private View space(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, h));
        return v;
    }
}
