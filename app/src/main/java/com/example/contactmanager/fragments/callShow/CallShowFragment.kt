package com.example.contactmanager.fragments.callShow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.contactmanager.R
import com.example.contactmanager.databinding.FragmentCallShowBinding
import com.example.contactmanager.utils.Common.isValidClick
import com.example.contactmanager.utils.OnClickHandler
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Random


class CallShowFragment : Fragment(), OnClickHandler {
    private lateinit var binding: FragmentCallShowBinding
    private val random = Random()

    private val weatherTypes by lazy {
        listOf(
            getString(R.string.sunny),
            getString(R.string.cloudy),
            getString(R.string.rainy),
            getString(R.string.partly_cloudy),
            getString(R.string.thunderstorm)
        )
    }

    private val descriptions by lazy {
        listOf(
            getString(R.string.there_will_be_mostly_sunny_skies),
            getString(R.string.expect_overcast_skies_throughout),
            getString(R.string.heavy_rain_is_expected),
            getString(R.string.a_mix_of_sun_and_clouds_with),
            getString(R.string.stormy_weather_with_high_winds)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentCallShowBinding.inflate(inflater, container, false)
        initView()
        return binding.root
    }

    private fun initView() {
        binding.onClickHandler = this
        updateWeatherData()
    }

    private fun updateWeatherData() {
        val currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
        binding.tvTime.text = currentTime

        val temp = random.nextInt(15) + 20 // 20 to 35
        binding.tvWeatherCelsius.text = "${temp}°C"
        binding.tvCelsius.text = "${temp}°C"

        val weatherIndex = random.nextInt(weatherTypes.size)
        binding.tvWeatherType.text = weatherTypes[weatherIndex]
        binding.tvDescription.text = descriptions[weatherIndex]

        binding.tvAirQuality.text = (random.nextInt(100) + 50).toString()
        binding.tvWind.text = "${random.nextInt(20) + 5} km/h"
        binding.tvHumidity.text = "${random.nextInt(40) + 40}%"
        binding.tvVisibility.text = "${random.nextInt(10) + 2} km"
        binding.tvDewPoint.text = "${random.nextInt(10) + 15}°"
    }

    override fun onClick(view: View) {
        if (!isValidClick()) return
        when (view.id) {
            binding.cvRefresh.id -> {
                updateWeatherData()
            }
        }
    }

}