package com.mykhailo.ytwidget;

import android.content.ComponentName;
import android.content.Context;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import java.util.List;

final class Media {
    static final String YTM = "com.google.android.apps.youtube.music";

    static boolean hasAccess(Context c) {
        String s = android.provider.Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName());
    }

    /** YouTube Music session first; otherwise null. */
    static MediaController find(Context c) {
        if (!hasAccess(c)) return null;
        try {
            MediaSessionManager m = (MediaSessionManager) c.getSystemService(Context.MEDIA_SESSION_SERVICE);
            List<MediaController> list = m.getActiveSessions(new ComponentName(c, MediaListener.class));
            for (MediaController mc : list) if (YTM.equals(mc.getPackageName())) return mc;
        } catch (SecurityException ignored) { }
        return null;
    }

    static boolean isPlaying(MediaController mc) {
        PlaybackState s = mc == null ? null : mc.getPlaybackState();
        if (s == null) return false;
        int st = s.getState();
        return st == PlaybackState.STATE_PLAYING || st == PlaybackState.STATE_BUFFERING
            || st == PlaybackState.STATE_CONNECTING;
    }
}
