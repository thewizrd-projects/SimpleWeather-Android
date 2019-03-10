package com.thewizrd.simpleweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.jakewharton.threetenabp.AndroidThreeTen;
import com.thewizrd.shared_resources.AppState;
import com.thewizrd.shared_resources.ApplicationLib;
import com.thewizrd.shared_resources.AsyncTask;
import com.thewizrd.shared_resources.SimpleLibrary;
import com.thewizrd.shared_resources.controls.LocationQueryViewModel;
import com.thewizrd.shared_resources.locationdata.here.HERELocationProvider;
import com.thewizrd.shared_resources.utils.Logger;
import com.thewizrd.shared_resources.utils.Settings;
import com.thewizrd.shared_resources.utils.WeatherException;
import com.thewizrd.shared_resources.weatherdata.LocationData;
import com.thewizrd.shared_resources.weatherdata.Weather;
import com.thewizrd.shared_resources.weatherdata.WeatherAPI;
import com.thewizrd.shared_resources.weatherdata.WeatherManager;
import com.thewizrd.simpleweather.widgets.WidgetUtils;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertTrue;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @Before
    public void init() {
        // Context of the app under test.
        final Context appContext = InstrumentationRegistry.getTargetContext();

        ApplicationLib app = new ApplicationLib() {
            @Override
            public Context getAppContext() {
                return appContext.getApplicationContext();
            }

            @Override
            public SharedPreferences getPreferences() {
                return PreferenceManager.getDefaultSharedPreferences(getAppContext());
            }

            @Override
            public SharedPreferences.OnSharedPreferenceChangeListener getSharedPreferenceListener() {
                return null;
            }

            @Override
            public AppState getAppState() {
                return null;
            }

            @Override
            public boolean isPhone() {
                return true;
            }
        };

        SimpleLibrary.init(app);
        AndroidThreeTen.init(appContext);

        // Start logger
        Logger.init(appContext);
    }

    @Test
    public void updateWidgetTest() {
        WidgetUtils.addWidgetId("NewYork", 10);
        WidgetUtils.addWidgetId("NewYork", 11);
        WidgetUtils.addWidgetId("NewYork", 12);
        WidgetUtils.addWidgetId("NewYork", 13);
        WidgetUtils.addWidgetId("NewYork", 14);
        WidgetUtils.addWidgetId("NewYork", 15);
        WidgetUtils.addWidgetId("NewYork", 16);
        WidgetUtils.addWidgetId("NewYork", 17);

        LocationData loc = new LocationData();
        loc.setQuery("OldYork");

        WidgetUtils.updateWidgetIds("NewYork", loc);
    }

    @Test
    public void getWeatherTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        // Need to get FULL location data for HERE API
        // Data provided is incomplete
        if (WeatherAPI.HERE.equals(Settings.getAPI())
                && loc.getLocationLat() == -1 && loc.getLocationLong() == -1
                && loc.getLocationTZLong() == null) {
            final LocationQueryViewModel query_vm = loc;
            loc = new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
                @Override
                public LocationQueryViewModel call() throws Exception {
                    return new HERELocationProvider().getLocationfromLocID(query_vm.getLocationQuery());
                }
            });
        }

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void updateLocationQueryTest() throws WeatherException {
        WeatherManager wm = WeatherManager.getInstance();
        Settings.setAPI(WeatherAPI.HERE);
        wm.updateAPI();

        Collection<LocationQueryViewModel> collection = wm.getLocations("Houston, Texas");
        List<LocationQueryViewModel> locs = new ArrayList<>(collection);
        LocationQueryViewModel loc = locs.get(0);

        // Need to get FULL location data for HERE API
        // Data provided is incomplete
        if (WeatherAPI.HERE.equals(Settings.getAPI())
                && loc.getLocationLat() == -1 && loc.getLocationLong() == -1
                && loc.getLocationTZLong() == null) {
            final LocationQueryViewModel query_vm = loc;
            loc = new AsyncTask<LocationQueryViewModel>().await(new Callable<LocationQueryViewModel>() {
                @Override
                public LocationQueryViewModel call() throws Exception {
                    return new HERELocationProvider().getLocationfromLocID(query_vm.getLocationQuery());
                }
            });
        }

        LocationData locationData = new LocationData(loc);
        Weather weather = wm.getWeather(locationData);

        Settings.setAPI(WeatherAPI.YAHOO);
        wm.updateAPI();

        if ((weather != null && !weather.getSource().equals(Settings.getAPI()))
                || (weather == null && locationData != null && !locationData.getSource().equals(Settings.getAPI()))) {
            // Update location query and source for new API
            String oldKey = locationData.getQuery();

            if (weather != null)
                locationData.setQuery(wm.updateLocationQuery(weather));
            else
                locationData.setQuery(wm.updateLocationQuery(locationData));

            locationData.setSource(Settings.getAPI());
        }

        weather = wm.getWeather(locationData);
        assertTrue(weather != null && weather.isValid());
    }

    @Test
    public void widgetCleanupTest() {
        WidgetUtils.cleanupWidgetData();
        WidgetUtils.cleanupWidgetIds();
    }

    @Test
    public void notificationTest() {
        /*
        LocationQueryViewModel vm = new LocationQueryViewModel();
        vm.setLocationCountry("US");
        vm.setLocationName("New York, NY");
        vm.setLocationQuery("11413");
        vm.setLocationTZLong("America/New_York");
        List<WeatherAlert> la = new ArrayList<>();
        WeatherAlert alert = new WeatherAlert();
        alert.setAttribution("Attribution");
        alert.setDate(ZonedDateTime.now(ZoneOffset.UTC));
        alert.setExpiresDate(ZonedDateTime.now(ZoneOffset.UTC).plusDays(5));
        alert.setMessage("Message");
        alert.setTitle("Title");
        alert.setType(WeatherAlertType.HIGHWIND);
        alert.setNotified(false);
        la.add(alert);
        WeatherAlertNotificationBuilder.createNotifications(new LocationData(vm), la);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }
        */
    }
}
