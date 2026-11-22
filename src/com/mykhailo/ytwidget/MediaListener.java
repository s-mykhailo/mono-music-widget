package com.mykhailo.ytwidget;

import android.content.ComponentName;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import java.util.List;

public class MediaListener extends NotificationListenerService {
    private MediaSessionManager msm;
    private MediaController current;
    private final Handler h = new Handler(Looper.getMainLooper());

    private final MediaController.Callback cb = new MediaController.Callback() {
        @Override public void onMetadataChanged(MediaMetadata m) { push(); }
        @Override public void onPlaybackStateChanged(PlaybackState s) { push(); }
        @Override public void onSessionDestroyed() { attach(null); push(); }
    };

    private final MediaSessionManager.OnActiveSessionsChangedListener sl = this::onSessions;

    @Override public void onListenerConnected() {
        msm = (MediaSessionManager) getSystemService(MEDIA_SESSION_SERVICE);
        ComponentName cn = new ComponentName(this, MediaListener.class);
        try {
            msm.addOnActiveSessionsChangedListener(sl, cn);
            onSessions(msm.getActiveSessions(cn));
        } catch (SecurityException ignored) { }
    }

    @Override public void onListenerDisconnected() {
        if (msm != null) msm.removeOnActiveSessionsChangedListener(sl);
        attach(null);
    }

    private void onSessions(List<MediaController> list) {
        MediaController found = null;
        if (list != null) for (MediaController mc : list)
            if (Media.YTM.equals(mc.getPackageName())) { found = mc; break; }
        attach(found);
        push();
    }

    private void attach(MediaController mc) {
        if (current != null) current.unregisterCallback(cb);
        current = mc;
        if (mc != null) mc.registerCallback(cb, h);
    }

    // debounce: metadata + state often arrive together
    private final Runnable render = () -> MusicWidget.renderAll(this, current);
    private void push() { h.removeCallbacks(render); h.postDelayed(render, 120); }
}
