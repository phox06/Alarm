package com.example.alarm

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import androidx.fragment.app.Fragment

class GlobeSelectionFragment : Fragment(R.layout.fragment_globe_selection) {

    private val cityDataList = listOf(
        CityData("Baker Island", "Etc/GMT+12"), CityData("Midway", "Pacific/Midway"),
        CityData("Honolulu", "Pacific/Honolulu"), CityData("Anchorage", "America/Anchorage"),
        CityData("Los Angeles", "America/Los_Angeles"), CityData("Vancouver", "America/Vancouver"),
        CityData("Denver", "America/Denver"), CityData("Chicago", "America/Chicago"),
        CityData("Mexico City", "America/Mexico_City"), CityData("New York", "America/New_York"),
        CityData("Toronto", "America/Toronto"), CityData("Bogotá", "America/Bogota"),
        CityData("Lima", "America/Lima"), CityData("Caracas", "America/Caracas"),
        CityData("Halifax", "America/Halifax"), CityData("Buenos Aires", "America/Argentina/Buenos_Aires"),
        CityData("São Paulo", "America/Sao_Paulo"), CityData("Fernando de Noronha", "America/Noronha"),
        CityData("Cape Verde", "Atlantic/Cape_Verde"), CityData("Azores", "Atlantic/Azores"),
        CityData("London", "Europe/London"), CityData("Reykjavik", "Atlantic/Reykjavik"),
        CityData("Accra", "Africa/Accra"), CityData("Paris", "Europe/Paris"),
        CityData("Berlin", "Europe/Berlin"), CityData("Lagos", "Africa/Lagos"),
        CityData("Cairo", "Africa/Cairo"), CityData("Johannesburg", "Africa/Johannesburg"),
        CityData("Athens", "Europe/Athens"), CityData("Moscow", "Europe/Moscow"),
        CityData("Istanbul", "Europe/Istanbul"), CityData("Riyadh", "Asia/Riyadh"),
        CityData("Dubai", "Asia/Dubai"), CityData("Kabul", "Asia/Kabul"),
        CityData("Karachi", "Asia/Karachi"), CityData("Mumbai", "Asia/Kolkata"),
        CityData("Kathmandu", "Asia/Kathmandu"), CityData("Dhaka", "Asia/Dhaka"),
        CityData("Almaty", "Asia/Almaty"), CityData("Ho Chi Minh", "Asia/Ho_Chi_Minh"),
        CityData("Bangkok", "Asia/Bangkok"), CityData("Jakarta", "Asia/Jakarta"),
        CityData("Singapore", "Asia/Singapore"), CityData("Beijing", "Asia/Shanghai"),
        CityData("Perth", "Australia/Perth"), CityData("Tokyo", "Asia/Tokyo"),
        CityData("Seoul", "Asia/Seoul"), CityData("Sydney", "Australia/Sydney"),
        CityData("Vladivostok", "Asia/Vladivostok"), CityData("Noumea", "Pacific/Noumea"),
        CityData("Auckland", "Pacific/Auckland"), CityData("Chatham Islands", "Pacific/Chatham"),
        CityData("Apia", "Pacific/Apia"), CityData("Kiritimati", "Pacific/Kiritimati")
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupWebView(view)
        setupSearchBar(view)
    }

    private fun setupWebView(view: View) {
        val webView: WebView = view.findViewById(R.id.globeWebView)

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }

        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null)
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(WebAppInterface(), "Android")
        webView.loadUrl("file:///android_asset/globe.html")
    }

    private fun setupSearchBar(view: View) {
        val searchAutoComplete: AutoCompleteTextView = view.findViewById(R.id.searchAutoComplete)

        val cityNames = cityDataList.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, cityNames)
        searchAutoComplete.setAdapter(adapter)

        searchAutoComplete.setOnItemClickListener { _, _, position, _ ->
            val selectedName = adapter.getItem(position)
            val selectedCity = cityDataList.find { it.name == selectedName }

            if (selectedCity != null) {
                sendResultAndClose(selectedCity.timeZoneId)
            }
        }
    }

    private fun sendResultAndClose(timeZoneId: String) {
        val result = Bundle().apply { putString("timeZoneId", timeZoneId) }
        parentFragmentManager.setFragmentResult("timezoneRequest", result)
        parentFragmentManager.popBackStack()
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun onCitySelected(cityName: String, timeZoneId: String) {
            activity?.runOnUiThread {
                sendResultAndClose(timeZoneId)
            }
        }
    }

    data class CityData(val name: String, val timeZoneId: String)
}
