package com.mykhailo.ytwidget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.widget.RemoteViews;

public class MusicWidget extends AppWidgetProvider {
    static final String A_PREV = "ytw.PREV", A_PLAY = "ytw.PLAY", A_NEXT = "ytw.NEXT";

    // last known track, kept so the widget still shows something once YT Music stops or loses its session
    private static CharSequence lastTitle, lastArtist;
    private static boolean lastPlaying;
    private static String lastArtKey;
    private static Bitmap lastArtMono, lastArtColor;

    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        renderAll(c, Media.find(c));
    }

    @Override public void onReceive(Context c, Intent i) {
        super.onReceive(c, i);
        String a = i.getAction();
        if (a == null || !a.startsWith("ytw.")) return;
        MediaController mc = Media.find(c);
        if (mc == null) { openApp(c); return; }
        MediaController.TransportControls t = mc.getTransportControls();
        switch (a) {
            case A_PREV: t.skipToPrevious(); break;
            case A_NEXT: t.skipToNext(); break;
            case A_PLAY:
                if (Media.isPlaying(mc)) t.pause(); else t.play();
                break;
        }
    }

    private static void openApp(Context c) {
        Intent l = c.getPackageManager().getLaunchIntentForPackage(Media.YTM);
        if (l != null) { l.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); c.startActivity(l); }
    }

    static void renderAll(Context c, MediaController mc) {
        updateSnapshot(mc);
        renderWidgets(c, MusicWidget.class, true);
        renderWidgets(c, ColorMusicWidget.class, false);
    }

    private static void renderWidgets(Context c, Class<?> provider, boolean mono) {
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, provider));
        if (ids.length > 0) m.updateAppWidget(ids, build(c, mono));
    }

    private static PendingIntent bc(Context c, String action, int rq) {
        Intent i = new Intent(c, MusicWidget.class).setAction(action);
        return PendingIntent.getBroadcast(c, rq, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Refreshes the remembered track/art; a missing session or missing art never clears what's already remembered. */
    private static void updateSnapshot(MediaController mc) {
        lastPlaying = Media.isPlaying(mc);
        MediaMetadata md = mc == null ? null : mc.getMetadata();
        if (md == null) return;

        CharSequence title = md.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence artist = md.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (artist == null) artist = md.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        lastTitle = title;
        lastArtist = artist;

        Bitmap src = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (src == null) src = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (src == null) src = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        String key = title + "|" + artist + "|" + (src == null ? 0 : src.getGenerationId() + "x" + src.getWidth());
        if (!key.equals(lastArtKey) && src != null) {
            lastArtKey = key;
            lastArtMono = mono(src);
            lastArtColor = color(src);
        }
    }

    private static RemoteViews build(Context c, boolean mono) {
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget);
        v.setOnClickPendingIntent(R.id.prev, bc(c, A_PREV, 1));
        v.setOnClickPendingIntent(R.id.play, bc(c, A_PLAY, 2));
        v.setOnClickPendingIntent(R.id.next, bc(c, A_NEXT, 3));

        // tap on cover -> open YT Music (or this app if no access yet)
        Intent open;
        if (!Media.hasAccess(c)) open = new Intent(c, MainActivity.class);
        else {
            open = c.getPackageManager().getLaunchIntentForPackage(Media.YTM);
            if (open == null) open = new Intent(c, MainActivity.class);
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        v.setOnClickPendingIntent(R.id.art, PendingIntent.getActivity(c, 4, open,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT));

        if (!Media.hasAccess(c)) {
            v.setTextViewText(R.id.title, "Потрібен доступ");
            v.setTextViewText(R.id.artist, "Натисни, щоб увімкнути");
            v.setImageViewBitmap(R.id.art, null);
            return v;
        }

        if (lastTitle == null) {
            v.setTextViewText(R.id.title, "Нічого не грає");
            v.setTextViewText(R.id.artist, "YouTube Music");
            v.setImageViewBitmap(R.id.art, null);
            v.setImageViewResource(R.id.play, R.drawable.ic_play);
            return v;
        }

        v.setTextViewText(R.id.title, lastTitle);
        v.setTextViewText(R.id.artist, lastArtist == null ? "" : lastArtist);
        v.setImageViewResource(R.id.play, lastPlaying ? R.drawable.ic_pause : R.drawable.ic_play);
        v.setImageViewBitmap(R.id.art, mono ? lastArtMono : lastArtColor);
        return v;
    }

    /** Square center-crop, grayscale with a bit more contrast, semi-transparent. */
    static Bitmap mono(Bitmap src) {
        float k = 1.15f, off = -0.075f * 255f;
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        cm.postConcat(new ColorMatrix(new float[]{
            k,0,0,0,off, 0,k,0,0,off, 0,0,k,0,off, 0,0,0,1,0}));
        return transform(src, cm);
    }

    /** Square center-crop, full color with the same contrast boost. */
    static Bitmap color(Bitmap src) {
        float k = 1.1f, off = -0.05f * 255f;
        ColorMatrix cm = new ColorMatrix(new float[]{
            k,0,0,0,off, 0,k,0,0,off, 0,0,k,0,off, 0,0,0,1,0});
        return transform(src, cm);
    }

    private static Bitmap transform(Bitmap src, ColorMatrix cm) {
        final int S = 420;
        int w = src.getWidth(), h = src.getHeight(), side = Math.min(w, h);
        Rect from = new Rect((w - side) / 2, (h - side) / 2, (w - side) / 2 + side, (h - side) / 2 + side);
        Bitmap out = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        p.setColorFilter(new ColorMatrixColorFilter(cm));
        p.setAlpha(200); // ~78% opacity -> wallpaper shows through
        Bitmap s = src.getConfig() == Bitmap.Config.HARDWARE ? src.copy(Bitmap.Config.ARGB_8888, false) : src;
        cv.drawBitmap(s, from, new Rect(0, 0, S, S), p);
        return out;
    }
}
