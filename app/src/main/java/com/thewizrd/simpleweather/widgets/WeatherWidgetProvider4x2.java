package com.thewizrd.simpleweather.widgets;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.thewizrd.simpleweather.App;
import com.thewizrd.simpleweather.R;

import static com.thewizrd.simpleweather.widgets.WidgetType.Widget4x2;

public class WeatherWidgetProvider4x2 extends WeatherWidgetProvider {
    private static WeatherWidgetProvider4x2 sInstance;

    public static synchronized WeatherWidgetProvider4x2 getInstance() {
        if (sInstance == null)
            sInstance = new WeatherWidgetProvider4x2();

        return sInstance;
    }

    // Overrides
    @Override
    public WidgetType getWidgetType() {
        return Widget4x2;
    }

    @Override
    public int getWidgetLayoutId() {
        return R.layout.app_widget_4x2;
    }

    @Override
    protected String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public ComponentName getComponentName() {
        return new ComponentName(App.getInstance().getAppContext(), getClassName());
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent == null ? null : intent.getAction();

        if (Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // Update clock widget
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            ComponentName componentname = new ComponentName(context.getPackageName(), getClassName());
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentname);

            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_UPDATECLOCK)
                    .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));

            WeatherWidgetService.enqueueWork(context, new Intent(context, WeatherWidgetService.class)
                    .setAction(WeatherWidgetService.ACTION_UPDATEDATE)
                    .putExtra(EXTRA_WIDGET_IDS, appWidgetIds)
                    .putExtra(EXTRA_WIDGET_TYPE, getWidgetType().getValue()));
        } else if (ACTION_SHOWNEXTFORECAST.equals(action)) {
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            RemoteViews views = new RemoteViews(context.getPackageName(), getWidgetLayoutId());
            views.showNext(R.id.forecast_layout);
            int appWidgetId = intent.getIntExtra(WeatherWidgetProvider.EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
            appWidgetManager.updateAppWidget(appWidgetId, views);
        } else {
            super.onReceive(context, intent);
        }
    }
}
