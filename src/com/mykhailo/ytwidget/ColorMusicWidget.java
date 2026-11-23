package com.mykhailo.ytwidget;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

/** Same widget as MusicWidget, rendered in full color instead of grayscale. */
public class ColorMusicWidget extends AppWidgetProvider {
    @Override public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        MusicWidget.renderAll(c, Media.find(c));
    }
}
