package com.mykhailo.ytwidget;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** Checks GitHub releases for a newer version. Called on demand only, no background polling. */
final class UpdateChecker {
    private static final String REPO = "s-mykhailo/mono-music-widget";

    interface Listener { void onUpdate(String version, String url); }

    static void check(Activity a, Listener l) {
        new Thread(() -> {
            try {
                HttpURLConnection c = (HttpURLConnection)
                    new URL("https://api.github.com/repos/" + REPO + "/releases/latest").openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                c.setRequestProperty("Accept", "application/vnd.github+json");
                StringBuilder sb = new StringBuilder();
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                JSONObject j = new JSONObject(sb.toString());
                String tag = j.optString("tag_name", "");
                String latest = tag.startsWith("v") ? tag.substring(1) : tag;
                String current = a.getPackageManager().getPackageInfo(a.getPackageName(), 0).versionName;
                String html = j.optString("html_url", "https://github.com/" + REPO + "/releases/latest");
                if (!latest.isEmpty() && !latest.equals(current)) {
                    new Handler(Looper.getMainLooper()).post(() -> l.onUpdate(latest, html));
                }
            } catch (Exception ignored) { }
        }).start();
    }
}
