package com.thewizrd.simpleweather.services

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.thewizrd.shared_resources.utils.Settings
import com.thewizrd.simpleweather.utils.PowerUtils
import com.thewizrd.simpleweather.widgets.WeatherWidgetService

class UpdaterUtils {
    companion object {
        @JvmStatic
        fun requestUpdateWidgets(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WidgetUpdaterWorker.ACTION_UPDATEWIDGETS))
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_UPDATEWIDGETS)
            }
        }

        @JvmStatic
        fun requestUpdateWeather(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterWorker.ACTION_UPDATEWEATHER))
            } else {
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_UPDATEWEATHER)
            }
        }

        @JvmStatic
        fun startAlarm(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_STARTALARM))
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_ENQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
            }
        }

        @JvmStatic
        fun cancelAlarm(context: Context) {
            // Cancel alarm if dependent features are turned off
            if (!WeatherWidgetService.widgetsExist(context) && !Settings.showOngoingNotification() && !Settings.useAlerts()) {
                if (PowerUtils.useForegroundService) {
                    ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                            .setAction(WeatherUpdaterService.ACTION_CANCELALARM))
                } else {
                    WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                    WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)
                }
            }
        }

        @JvmStatic
        fun updateAlarm(context: Context) {
            if (PowerUtils.useForegroundService) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_UPDATEALARM))
            } else {
                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
            }
        }

        @JvmStatic
        fun enableForegroundService(context: Context, enable: Boolean) {
            if (enable) {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_UPDATEALARM))

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_CANCELWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_CANCELWORK)
            } else {
                ContextCompat.startForegroundService(context, Intent(context, WeatherUpdaterService::class.java)
                        .setAction(WeatherUpdaterService.ACTION_CANCELALARM))

                WidgetUpdaterWorker.enqueueAction(context, WidgetUpdaterWorker.ACTION_REQUEUEWORK)
                WeatherUpdaterWorker.enqueueAction(context, WeatherUpdaterWorker.ACTION_ENQUEUEWORK)
            }
        }
    }
}