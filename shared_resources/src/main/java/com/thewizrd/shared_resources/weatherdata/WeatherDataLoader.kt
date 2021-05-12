package com.thewizrd.shared_resources.weatherdata

import android.content.Intent
import android.util.Log
import androidx.core.util.ObjectsCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.errorprone.annotations.CanIgnoreReturnValue
import com.ibm.icu.util.ULocale
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.SimpleLibrary
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.coroutines.coroutineContext

class WeatherDataLoader(private val location: LocationData) {
    companion object {
        private const val TAG = "WeatherDataLoader"
    }

    private var weather: Weather? = null
    private var weatherAlerts: Collection<WeatherAlert>? = null
    private val wm = WeatherManager.instance

    private val mLocalBroadcastManager = LocalBroadcastManager.getInstance(SimpleLibrary.getInstance().app.appContext)
    private val settingsMgr = SimpleLibrary.getInstance().app.settingsManager

    suspend fun loadWeatherData(request: WeatherRequest): Weather? {
        val result = getWeatherResult(request)
        return result.weather
    }

    suspend fun loadWeatherResult(request: WeatherRequest): WeatherResult {
        return getWeatherResult(request)
    }

    private suspend fun getWeatherResult(request: WeatherRequest): WeatherResult {
        var result: WeatherResult? = null

        try {
            if (request.isForceLoadSavedData) {
                loadSavedWeatherData(request, true)
            } else {
                if (request.isForceRefresh) {
                    result = getWeatherData(request)
                } else {
                    if (!isDataValid(false)) {
                        result = _loadWeatherData(request)
                    }
                }
            }
            checkForOutdatedObservation(request)
        } catch (wEx: WeatherException) {
            if (request.errorListener != null) {
                request.errorListener.onWeatherError(wEx)
            } else {
                throw wEx
            }
        }

        Logger.writeLine(Log.DEBUG, "%s: Weather data for %s is valid = %s", TAG,
                location.toString(), weather?.isValid ?: "null")

        return result ?: WeatherResult.create(weather, false)
    }

    suspend fun loadWeatherAlerts(loadSavedData: Boolean): Collection<WeatherAlert>? {
        if (wm.supportsAlerts()) {
            if (wm.needsExternalAlertData()) {
                if (!loadSavedData) {
                    weatherAlerts = wm.getAlerts(location)
                }
            }

            if (weatherAlerts == null) {
                weatherAlerts = settingsMgr.getWeatherAlertData(location.query)
            }

            if (!loadSavedData) {
                saveWeatherAlerts()
            }
        }

        return weatherAlerts
    }

    @CanIgnoreReturnValue
    @Throws(WeatherException::class)
    private suspend fun getWeatherData(request: WeatherRequest): WeatherResult {
        var wEx: WeatherException? = null
        var loadedSavedData = false
        var loadedSavedAlertData = false

        // Try to get weather from provider API
        try {
            coroutineContext.ensureActive()

            if (!wm.isRegionSupported(location.countryCode)) {
                // If location data hasn't been updated, try loading weather from the previous provider
                if (!location.weatherSource.isNullOrBlank()) {
                    val provider = WeatherManager.getProvider(location.weatherSource)
                    if (provider.isRegionSupported(location.countryCode)) {
                        weather =
                            WeatherManager.getProvider(location.weatherSource).getWeather(location)
                    }
                }

                // Nothing to fallback on; error out
                throw WeatherException(ErrorStatus.QUERYNOTFOUND)
            } else {
                // Load weather from provider
                weather = wm.getWeather(location)
            }
        } catch (weatherEx: WeatherException) {
            wEx = weatherEx
            weather = null
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error getting weather data")
            weather = null
        }

        if (request.isLoadAlerts && weather != null && wm.supportsAlerts()) {
            if (wm.needsExternalAlertData()) {
                weather!!.weatherAlerts = wm.getAlerts(location)
            }

            if (weather!!.weatherAlerts == null) {
                weather!!.weatherAlerts = settingsMgr.getWeatherAlertData(location.query).also {
                    weatherAlerts = it
                }
                loadedSavedAlertData = true
            }
        }

        if (request.isShouldSaveData) {
            // Load old data if available and we can't get new data
            if (weather == null) {
                loadedSavedData = loadSavedWeatherData(request, true)
                loadedSavedAlertData = loadedSavedData
            } else {
                // Handle upgrades
                if (location.name.isNullOrBlank() || location.tzLong.isNullOrBlank()) {
                    location.name = weather!!.location.name
                    location.tzLong = weather!!.location.tzLong

                    if (SimpleLibrary.getInstance().app.isPhone)
                        settingsMgr.updateLocation(location)
                    else
                        settingsMgr.saveHomeData(location)
                }
                if (location.latitude == 0.0 && location.longitude == 0.0 && weather?.location?.latitude != null && weather?.location?.longitude != null) {
                    location.latitude = weather!!.location.latitude.toDouble()
                    location.longitude = weather!!.location.longitude.toDouble()

                    if (SimpleLibrary.getInstance().app.isPhone)
                        settingsMgr.updateLocation(location)
                    else
                        settingsMgr.saveHomeData(location)
                }
                if (location.locationSource.isNullOrBlank()) {
                    location.locationSource = wm.getLocationProvider().getLocationAPI()
                    if (SimpleLibrary.getInstance().app.isPhone)
                        settingsMgr.updateLocation(location)
                    else
                        settingsMgr.saveHomeData(location)
                }

                if (!loadedSavedData) {
                    saveWeatherData()
                    saveWeatherForecasts()
                }

                if ((request.isLoadAlerts || weather?.weatherAlerts != null) && wm.supportsAlerts()) {
                    weatherAlerts = weather!!.weatherAlerts
                    if (!loadedSavedAlertData) {
                        saveWeatherAlerts()
                    }
                }
            }
        }

        // Throw exception if we're unable to get any weather data
        if (weather == null && wEx != null) {
            throw wEx
        } else if (weather == null && wEx == null) {
            throw WeatherException(ErrorStatus.NOWEATHER)
        } else if (weather != null && wEx != null && loadedSavedData) {
            throw wEx
        }

        return WeatherResult.create(weather, !loadedSavedData)
    }

    @CanIgnoreReturnValue
    @Throws(WeatherException::class)
    private suspend fun _loadWeatherData(request: WeatherRequest): WeatherResult {
        /*
         * If unable to retrieve saved data, data is old, or units don't match
         * Refresh weather data
         */

        Logger.writeLine(Log.DEBUG, "%s: Loading weather data for %s", TAG, location.toString())

        val gotData = loadSavedWeatherData(request)

        return if (!gotData) {
            if (request.isShouldSaveData) {
                Logger.writeLine(Log.DEBUG, "%s: Saved weather data invalid for %s", TAG, location.toString())
                Logger.writeLine(Log.DEBUG, "%s: Retrieving data from weather provider", TAG)

                if (weather != null && weather!!.source != settingsMgr.getAPI()
                    || weather == null && location.weatherSource != settingsMgr.getAPI()) {
                    // Only update location data if location region is supported by new API
                    // If not don't update so we can use fallback (previously used API)
                    if (wm.isRegionSupported(location.countryCode)) {
                        // Update location query and source for new API
                        val oldKey = location.query

                        if (weather != null)
                            location.query = wm.updateLocationQuery(weather!!)
                        else
                            location.query = wm.updateLocationQuery(location)

                        location.weatherSource = settingsMgr.getAPI()

                        // Update database as well
                        if (SimpleLibrary.getInstance().app.isPhone) {
                            if (location.locationType == LocationType.GPS) {
                                settingsMgr.saveLastGPSLocData(location)
                                mLocalBroadcastManager.sendBroadcast(Intent(CommonActions.ACTION_WEATHER_SENDLOCATIONUPDATE))
                            } else {
                                settingsMgr.updateLocationWithKey(location, oldKey)
                                mLocalBroadcastManager.sendBroadcast(
                                        Intent(CommonActions.ACTION_WEATHER_UPDATEWIDGETLOCATION)
                                                .putExtra(Constants.WIDGETKEY_OLDKEY, oldKey)
                                                .putExtra(Constants.WIDGETKEY_LOCATION, JSONParser.serializer(location, LocationData::class.java)))
                            }
                        } else {
                            settingsMgr.saveHomeData(location)
                        }
                    }
                }
            }

            getWeatherData(request)
        } else {
            WeatherResult.create(weather, false)
        }
    }

    @Throws(WeatherException::class)
    private suspend fun loadSavedWeatherData(request: WeatherRequest, _override: Boolean = false
    ): Boolean {
        // Load weather data
        try {
            coroutineContext.ensureActive()

            weather = settingsMgr.getWeatherData(location.query)

            if (request.isLoadAlerts && weather != null && wm.supportsAlerts())
                weather!!.weatherAlerts = settingsMgr.getWeatherAlertData(location.query).also {
                    weatherAlerts = it
                }

            coroutineContext.ensureActive()

            if (request.isLoadForecasts && weather != null) {
                val forecasts = settingsMgr.getWeatherForecastData(location.query)
                val hrForecasts = settingsMgr.getHourlyWeatherForecastData(location.query)

                if (forecasts != null) {
                    weather!!.forecast = forecasts.forecast
                    weather!!.txtForecast = forecasts.txtForecast
                }
                weather!!.hrForecast = hrForecasts
            }

            coroutineContext.ensureActive()

            if (_override && weather == null) {
                // If weather is still unavailable try manually searching for it
                weather = settingsMgr.getWeatherDataByCoordinate(location)

                coroutineContext.ensureActive()

                if (request.isLoadAlerts && weather != null && wm.supportsAlerts())
                    weather!!.weatherAlerts = settingsMgr.getWeatherAlertData(location.query).also {
                        weatherAlerts = it
                    }

                coroutineContext.ensureActive()

                if (request.isLoadForecasts && weather != null) {
                    val forecasts = settingsMgr.getWeatherForecastData(location.query)
                    val hrForecasts = settingsMgr.getHourlyWeatherForecastData(location.query)

                    if (forecasts != null) {
                        weather!!.forecast = forecasts.forecast
                        weather!!.txtForecast = forecasts.txtForecast
                    }
                    weather!!.hrForecast = hrForecasts
                }
            }
        } catch (ex: Exception) {
            Logger.writeLine(Log.ERROR, ex, "WeatherDataLoader: error loading saved weather data")
            weather = null
            throw WeatherException(ErrorStatus.NOWEATHER)
        }

        return isDataValid(_override)
    }

    private fun checkForOutdatedObservation(request: WeatherRequest) {
        if (weather != null) {
            // Check for outdated observation
            val now = ZonedDateTime.now().withZoneSameInstant(location.tzOffset)
            val duraMins = if (weather?.condition?.observationTime == null) 61 else Duration.between(weather!!.condition.observationTime, now).toMinutes()
            if (duraMins > 60) {
                val interval = WeatherManager.getProvider(weather!!.source).getHourlyForecastInterval()

                val nowHour = now.truncatedTo(ChronoUnit.HOURS)
                var hrf = settingsMgr.getFirstHourlyForecastDataByDate(location.query, nowHour)
                if (hrf == null || Duration.between(now, hrf.date).toHours() > interval * 0.5) {
                    val prevHrf = settingsMgr.getFirstHourlyForecastDataByDate(location.query, nowHour.minusHours(interval.toLong()))
                    if (prevHrf != null) hrf = prevHrf
                }

                if (hrf != null) {
                    weather!!.condition.weather = hrf.getCondition()
                    weather!!.condition.icon = hrf.getIcon()

                    weather!!.condition.tempF = hrf.getHighF()
                    weather!!.condition.tempC = hrf.getHighC()

                    weather!!.condition.windMph = hrf.windMph
                    weather!!.condition.windKph = hrf.windKph
                    weather!!.condition.windDegrees = hrf.windDegrees

                    if (hrf.windMph != null) {
                        weather!!.condition.beaufort = Beaufort(getBeaufortScale(Math.round(hrf.windMph)).value)
                    }
                    weather!!.condition.feelslikeF = hrf.getExtras()?.feelslikeF
                    weather!!.condition.feelslikeC = hrf.getExtras()?.feelslikeC
                    weather!!.condition.uv = if (hrf.getExtras()?.uvIndex ?: -1f >= 0) UV(hrf.getExtras().uvIndex) else null

                    weather!!.condition.observationTime = hrf.date

                    if (duraMins > 60 * 6 || weather?.condition?.highF == null || weather!!.condition.highF == weather!!.condition.lowF) {
                        val fcasts = settingsMgr.getWeatherForecastData(location.query)
                        val fcast = fcasts?.forecast?.find { input -> input != null && input.date.toLocalDate().isEqual(now.toLocalDate()) }

                        if (fcast != null) {
                            weather!!.condition.highF = fcast.getHighF()
                            weather!!.condition.highC = fcast.getHighC()
                            weather!!.condition.lowF = fcast.lowF
                            weather!!.condition.lowC = fcast.lowC
                        } else {
                            weather!!.condition.highF = 0f
                            weather!!.condition.highC = 0f
                            weather!!.condition.lowF = 0f
                            weather!!.condition.lowC = 0f
                        }
                    }

                    weather!!.atmosphere.dewpointF = hrf.getExtras()?.dewpointF
                    weather!!.atmosphere.dewpointC = hrf.getExtras()?.dewpointC
                    weather!!.atmosphere.humidity = hrf.getExtras()?.humidity
                    weather!!.atmosphere.pressureTrend = null
                    weather!!.atmosphere.pressureIn = hrf.getExtras()?.pressureIn
                    weather!!.atmosphere.pressureMb = hrf.getExtras()?.pressureMb
                    weather!!.atmosphere.visibilityMi = hrf.getExtras()?.visibilityMi
                    weather!!.atmosphere.visibilityKm = hrf.getExtras()?.visibilityKm

                    if (weather!!.precipitation != null) {
                        weather!!.precipitation.pop = hrf.getExtras()?.pop
                        weather!!.precipitation.cloudiness = hrf.getExtras()?.cloudiness
                        weather!!.precipitation.qpfRainIn = if (hrf.getExtras()?.qpfRainIn ?: -1f >= 0) hrf.getExtras().qpfRainIn else 0.0f
                        weather!!.precipitation.qpfRainMm = if (hrf.getExtras()?.qpfRainMm ?: -1f >= 0) hrf.getExtras().qpfRainMm else 0.0f
                        weather!!.precipitation.qpfSnowIn = if (hrf.getExtras()?.qpfSnowIn ?: -1f >= 0) hrf.getExtras().qpfSnowIn else 0.0f
                        weather!!.precipitation.qpfSnowCm = if (hrf.getExtras()?.qpfSnowCm ?: -1f >= 0) hrf.getExtras().qpfSnowCm else 0.0f
                    }

                    if (request.isShouldSaveData) {
                        settingsMgr.saveWeatherData(weather)
                    }
                }
            }

            // Check for outdated forecasts
            if (!weather?.forecast.isNullOrEmpty()) {
                weather!!.forecast.removeIf { input -> input == null || input.date.truncatedTo(ChronoUnit.DAYS).isBefore(now.toLocalDateTime().truncatedTo(ChronoUnit.DAYS)) }
            }

            if (!weather?.hrForecast.isNullOrEmpty()) {
                weather!!.hrForecast.removeIf { input -> input == null || input.date.truncatedTo(ChronoUnit.HOURS).isBefore(now.truncatedTo(ChronoUnit.HOURS)) }
            }
        }
    }

    private fun isDataValid(_override: Boolean): Boolean {
        val currentLocale = ULocale.forLocale(LocaleUtils.getLocale())
        val locale = wm.localeToLangCode(currentLocale.language, currentLocale.toLanguageTag())

        val API = settingsMgr.getAPI()
        var isInvalid = weather == null || !weather!!.isValid
        if (!isInvalid && !ObjectsCompat.equals(weather!!.source, API)) {
            // Don't mark data as invalid if region is not supported
            // This is so we can use the fallback, if location data was not already modified
            if (wm.isRegionSupported(location.countryCode)) {
                isInvalid = true
            }
        }

        if (wm.supportsWeatherLocale() && !isInvalid)
            isInvalid = weather!!.locale != locale

        if (_override || isInvalid) return !isInvalid

        // TODO: make this a premium feature
        val ttl = if (WeatherAPI.HERE == wm.getWeatherAPI()) {
            settingsMgr.getRefreshInterval()
        } else {
            Math.max(weather!!.ttl, settingsMgr.getRefreshInterval())
        }

        // Check file age
        val updateTime = weather!!.updateTime

        val span = Duration.between(ZonedDateTime.now(), updateTime).abs()
        return span.toMinutes() < ttl
    }

    private suspend fun saveWeatherData() = withContext(Dispatchers.IO) {
        // Save location query
        weather!!.query = location.query

        settingsMgr.saveWeatherData(weather)

        if (!SimpleLibrary.getInstance().app.isPhone) {
            settingsMgr.setUpdateTime(weather!!.updateTime.withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
        }
    }

    private suspend fun saveWeatherAlerts() = withContext(Dispatchers.IO) {
        if (weatherAlerts != null) {
            // Check for previously saved alerts
            val previousAlerts = settingsMgr.getWeatherAlertData(location.query)

            if (previousAlerts.isNotEmpty()) {
                // If any previous alerts were flagged before as notified
                // make sure to set them here as such
                // bc notified flag gets reset when retrieving weatherdata
                for (alert in weatherAlerts!!) {
                    for (prevAlert in previousAlerts) {
                        if (prevAlert == alert && prevAlert.isNotified) {
                            alert.isNotified = prevAlert.isNotified
                            break
                        }
                    }
                }
            }

            settingsMgr.saveWeatherAlerts(location, weatherAlerts)
        }
    }

    private suspend fun saveWeatherForecasts() = withContext(Dispatchers.IO) {
        val forecasts = Forecasts(weather!!.query, weather!!.forecast, weather!!.txtForecast)
        settingsMgr.saveWeatherForecasts(forecasts)
        val hrForecasts = ArrayList<HourlyForecasts>()
        if (weather?.hrForecast != null) {
            hrForecasts.ensureCapacity(weather!!.hrForecast.size)
            for (f in weather!!.hrForecast) {
                hrForecasts.add(HourlyForecasts(weather!!.query, f!!))
            }
        }
        settingsMgr.saveWeatherForecasts(location.query, hrForecasts)
    }
}