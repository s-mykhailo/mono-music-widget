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
import android.os.SystemClock;
import android.view.KeyEvent;
import android.widget.RemoteViews;

public class MusicWidget extends AppWidgetProvider {
    static final String A_PREV = "ytw.PREV", A_PLAY = "ytw.PLAY", A_NEXT = "ytw.NEXT";

    private static String lastArtKey;
    private static Bitmap lastArt;

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
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        int[] ids = m.getAppWidgetIds(new ComponentName(c, MusicWidget.class));
        if (ids.length == 0) return;
        RemoteViews v = build(c, mc);
        m.updateAppWidget(ids, v);
    }

    private static PendingIntent bc(Context c, String action, int rq) {
        Intent i = new Intent(c, MusicWidget.class).setAction(action);
        return PendingIntent.getBroadcast(c, rq, i, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
    }

    private static RemoteViews build(Context c, MediaController mc) {
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

        MediaMetadata md = mc == null ? null : mc.getMetadata();
        if (md == null) {
            v.setTextViewText(R.id.title, "Нічого не грає");
            v.setTextViewText(R.id.artist, "YouTube Music");
            v.setImageViewBitmap(R.id.art, null);
            v.setImageViewResource(R.id.play, R.drawable.ic_play);
            return v;
        }

        CharSequence title = md.getText(MediaMetadata.METADATA_KEY_TITLE);
        CharSequence artist = md.getText(MediaMetadata.METADATA_KEY_ARTIST);
        if (artist == null) artist = md.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST);
        v.setTextViewText(R.id.title, title == null ? "" : title);
        v.setTextViewText(R.id.artist, artist == null ? "" : artist);
        v.setImageViewResource(R.id.play, Media.isPlaying(mc) ? R.drawable.ic_pause : R.drawable.ic_play);

        Bitmap src = md.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART);
        if (src == null) src = md.getBitmap(MediaMetadata.METADATA_KEY_ART);
        if (src == null) src = md.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON);
        String key = title + "|" + artist + "|" + (src == null ? 0 : src.getGenerationId() + "x" + src.getWidth());
        if (!key.equals(lastArtKey)) {
            lastArtKey = key;
            lastArt = src == null ? null : mono(src);
        }
        v.setImageViewBitmap(R.id.art, lastArt);
        return v;
    }

    /** Square center-crop, grayscale with a bit more contrast, semi-transparent. */
    static Bitmap mono(Bitmap src) {
        final int S = 420;
        int w = src.getWidth(), h = src.getHeight(), side = Math.min(w, h);
        Rect from = new Rect((w - side) / 2, (h - side) / 2, (w - side) / 2 + side, (h - side) / 2 + side);
        Bitmap out = Bitmap.createBitmap(S, S, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(out);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0f);
        float k = 1.15f, off = -0.075f * 255f;
        ColorMatrix contrast = new ColorMatrix(new float[]{
            k,0,0,0,off, 0,k,0,0,off, 0,0,k,0,off, 0,0,0,1,0});
        cm.postConcat(contrast);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        p.setColorFilter(new ColorMatrixColorFilter(cm));
        p.setAlpha(200); // ~78% opacity -> wallpaper shows through
        Bitmap s = src.getConfig() == Bitmap.Config.HARDWARE ? src.copy(Bitmap.Config.ARGB_8888, false) : src;
        cv.drawBitmap(s, from, new Rect(0, 0, S, S), p);
        return out;
    }
}
