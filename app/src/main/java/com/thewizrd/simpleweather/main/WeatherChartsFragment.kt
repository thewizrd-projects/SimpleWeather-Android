package com.thewizrd.simpleweather.main

import android.graphics.Outline
import android.os.Bundle
import android.view.*
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.CallSuper
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.util.ObjectsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.thewizrd.shared_resources.Constants
import com.thewizrd.shared_resources.controls.*
import com.thewizrd.shared_resources.helpers.ContextUtils
import com.thewizrd.shared_resources.locationdata.LocationData
import com.thewizrd.shared_resources.utils.*
import com.thewizrd.shared_resources.utils.WeatherUtils.ErrorStatus
import com.thewizrd.shared_resources.weatherdata.WeatherAPI
import com.thewizrd.shared_resources.weatherdata.WeatherDataLoader
import com.thewizrd.shared_resources.weatherdata.WeatherRequest
import com.thewizrd.simpleweather.R
import com.thewizrd.simpleweather.adapters.ChartsItemAdapter
import com.thewizrd.simpleweather.controls.viewmodels.ChartsViewModel
import com.thewizrd.simpleweather.databinding.FragmentWeatherListBinding
import com.thewizrd.simpleweather.fragments.ToolbarFragment
import com.thewizrd.simpleweather.snackbar.Snackbar
import com.thewizrd.simpleweather.snackbar.SnackbarManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class WeatherChartsFragment : ToolbarFragment() {
    private lateinit var weatherView: WeatherNowViewModel
    private lateinit var chartsView: ChartsViewModel
    private var location: LocationData? = null

    private lateinit var binding: FragmentWeatherListBinding
    private lateinit var adapter: ChartsItemAdapter

    private lateinit var args: WeatherChartsFragmentArgs

    init {
        arguments = Bundle()
    }

    companion object {
        fun newInstance(locData: LocationData): WeatherChartsFragment {
            val fragment = WeatherChartsFragment()
            fragment.location = locData
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AnalyticsLogger.logEvent("WeatherChartsFragment: onCreate")

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(Constants.KEY_DATA)) {
                location = JSONParser.deserializer(savedInstanceState.getString(Constants.KEY_DATA), LocationData::class.java)
            }
        } else {
            if (args.data != null) {
                location = JSONParser.deserializer(args.data, LocationData::class.java)
            }
        }

        if (location == null)
            location = Settings.getHomeData()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val root = super.onCreateView(inflater, container, savedInstanceState) as ViewGroup?
        // Use this to return your custom view for this Fragment
        binding = FragmentWeatherListBinding.inflate(inflater, root, true)
        binding.lifecycleOwner = viewLifecycleOwner

        // Setup Actionbar
        val context = binding.root.context
        val navIcon = DrawableCompat.wrap(ContextCompat.getDrawable(context, R.drawable.ic_arrow_back_white_24dp)!!)
        DrawableCompat.setTint(navIcon, ContextCompat.getColor(context, R.color.invButtonColorText))
        toolbar.navigationIcon = navIcon

        toolbar.setNavigationOnClickListener { v ->
            Navigation.findNavController(v).navigateUp()
        }

        binding.locationHeader.clipToOutline = false
        binding.locationHeader.outlineProvider = object : ViewOutlineProvider() {
            val elevation = context.resources.getDimensionPixelSize(R.dimen.appbar_elevation)
            override fun getOutline(view: View, outline: Outline) {
                outline.setRect(0, view.height - elevation, view.width, view.height)
            }
        }

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the binding.recyclerView
        binding.recyclerView.setHasFixedSize(true)
        // use a linear layout manager
        binding.recyclerView.layoutManager = LinearLayoutManager(appCompatActivity)

        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                updateHeaderElevation()
            }
        })

        binding.recyclerView.adapter = ChartsItemAdapter().also {
            adapter = it
        }
        return root
    }

    private fun updateHeaderElevation() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.Main) {
            if (binding.recyclerView.computeVerticalScrollOffset() > 0) {
                binding.locationHeader.elevation = ContextUtils.dpToPx(requireContext(), 4f)
            } else {
                binding.locationHeader.elevation = 0f
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val vmProvider = ViewModelProvider(appCompatActivity)
        weatherView = vmProvider.get(WeatherNowViewModel::class.java)
        chartsView = ViewModelProvider(this).get(ChartsViewModel::class.java)

        args = WeatherChartsFragmentArgs.fromBundle(requireArguments())

        binding.locationHeader.viewTreeObserver.addOnPreDrawListener {
            if (isViewAlive) {
                val layoutParams = binding.recyclerView.layoutParams as MarginLayoutParams
                layoutParams.topMargin = binding.locationHeader.height
                binding.recyclerView.layoutParams = layoutParams
            }
            true
        }

        binding.progressBar.visibility = View.VISIBLE

        chartsView.getGraphModelData().observe(viewLifecycleOwner, {
            adapter.submitList(it)
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) {
            AnalyticsLogger.logEvent("WeatherChartsFragment: onResume")
            initialize()
        }
    }

    override fun onPause() {
        AnalyticsLogger.logEvent("WeatherChartsFragment: onPause")
        super.onPause()
    }

    override fun getTitle(): Int {
        return R.string.label_forecast
    }

    // Initialize views here
    @CallSuper
    protected fun initialize() {
        if (!weatherView.isValid || location != null && !ObjectsCompat.equals(location!!.query, weatherView.query)) {
            WeatherDataLoader(location!!)
                    .loadWeatherData(WeatherRequest.Builder()
                            .forceLoadSavedData()
                            .setErrorListener { wEx ->
                                when (wEx.errorStatus) {
                                    ErrorStatus.NETWORKERROR, ErrorStatus.NOWEATHER -> {
                                        // Show error message and prompt to refresh
                                        showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                                    }
                                    ErrorStatus.QUERYNOTFOUND -> {
                                        if (WeatherAPI.NWS == Settings.getAPI()) {
                                            showSnackbar(Snackbar.make(R.string.error_message_weather_us_only, Snackbar.Duration.LONG), null)
                                            return@setErrorListener
                                        }
                                        // Show error message
                                        showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                                    }
                                    else -> {
                                        // Show error message
                                        showSnackbar(Snackbar.make(wEx.message, Snackbar.Duration.LONG), null)
                                    }
                                }
                            }
                            .build())
                    .addOnSuccessListener { weather ->
                        if (isAlive) {
                            weatherView.updateView(weather)
                            chartsView.updateForecasts(location!!)
                            binding.locationName.text = weatherView.location
                        }
                    }
        } else {
            chartsView.updateForecasts(location!!)
            binding.locationName.text = weatherView.location
        }

        binding.progressBar.visibility = View.GONE
    }

    override fun onSaveInstanceState(outState: Bundle) {
        // Save data
        outState.putString(Constants.KEY_DATA, JSONParser.serializer(location, LocationData::class.java))

        super.onSaveInstanceState(outState)
    }

    override fun updateWindowColors() {
        super.updateWindowColors()

        var color = ContextUtils.getColor(appCompatActivity, android.R.attr.colorBackground)
        if (Settings.getUserThemeMode() == UserThemeMode.AMOLED_DARK) {
            color = Colors.BLACK
        }
        binding.locationHeader.setCardBackgroundColor(color)
        binding.recyclerView.setBackgroundColor(color)
    }

    override fun createSnackManager(): SnackbarManager {
        return SnackbarManager(binding.root).also {
            it.setSwipeDismissEnabled(true)
            it.setAnimationMode(BaseTransientBottomBar.ANIMATION_MODE_FADE)
        }
    }
}