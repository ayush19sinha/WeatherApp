package my.android.weatherapp

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.airbnb.lottie.LottieAnimationView
import my.android.weatherapp.Data.WeatherApp
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var maxTemp: TextView
    private lateinit var minTemp: TextView
    private lateinit var day: TextView
    private lateinit var weather: TextView
    private lateinit var humidity: TextView
    private lateinit var date: TextView
    private lateinit var windSpeed: TextView
    private lateinit var condition: TextView
    private lateinit var sunRise: TextView
    private lateinit var sunSet: TextView
    private lateinit var seaLevel: TextView
    private lateinit var temperature: TextView
    private lateinit var searchView: SearchView
    private lateinit var lottieAnimationView: LottieAnimationView

    private val retrofit by lazy {
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl("https://api.openweathermap.org/data/2.5/")
            .build()
    }

    private val apiService by lazy {
        retrofit.create(ApiInterface::class.java)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        lottieAnimationView = findViewById(R.id.lottieAnimationView)
        initializeViews()
        setupSearchView()
        fetchWeatherData("Ranchi")

        findViewById<View>(R.id.main).setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (!isPointInsideView(event.rawX, event.rawY, searchView)) {
                    searchView.clearFocus()
                }
            }
            false
        }
    }

    private fun initializeViews() {
        temperature = findViewById(R.id.temperature)
        maxTemp = findViewById(R.id.max_temp)
        minTemp = findViewById(R.id.min_temp)
        humidity = findViewById(R.id.humidity)
        seaLevel = findViewById(R.id.sea)
        sunSet = findViewById(R.id.sunset)
        sunRise = findViewById(R.id.sunrise)
        windSpeed = findViewById(R.id.wind_speed)
        day = findViewById(R.id.day)
        weather = findViewById(R.id.weather)
        date = findViewById(R.id.date)
        condition = findViewById(R.id.condition)
        searchView = findViewById(R.id.searchView)
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchWeatherData(it) }
                searchView.clearFocus()
                closeKeyboard()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return true
            }
        })
    }

    private fun fetchWeatherData(cityName: String) {
        val call = apiService.getWeatherData(cityName, "3f32a69a449f7e60d905754a72f86ccd", "metric")
        call.enqueue(object : Callback<WeatherApp> {
            override fun onResponse(call: Call<WeatherApp>, response: Response<WeatherApp>) {
                if (response.isSuccessful) {
                    response.body()?.let { updateUI(it, cityName) }
                } else {
                    Log.e("MainActivity", "Error: ${response.code()} ${response.message()}")
                }
            }

            override fun onFailure(call: Call<WeatherApp>, t: Throwable) {
                Log.e("MainActivity", "On Failure", t)
            }
        })
    }

    private fun updateUI(weatherData: WeatherApp, cityName: String) {
        runOnUiThread {
            temperature.text = "${weatherData.main.temp} °C"
            humidity.text = "${weatherData.main.humidity} %"
            windSpeed.text = "${weatherData.wind.speed} m/s"
            sunRise.text = timeFormat(weatherData.sys.sunrise.toLong())
            sunSet.text = timeFormat(weatherData.sys.sunset.toLong())
            seaLevel.text = "${weatherData.main.pressure} hPa"
            condition.text = weatherData.weather.firstOrNull()?.main ?: "Unknown"
            date.text = formatDate(System.currentTimeMillis())
            day.text = formatDayName(System.currentTimeMillis())
            weather.text = weatherData.weather.firstOrNull()?.main ?: "Unknown"
            findViewById<TextView>(R.id.city_name).text = cityName

            weatherData.weather.firstOrNull()?.main?.let { changeImagesAccordingToWeatherCondition(it) }
        }
    }

    private fun changeImagesAccordingToWeatherCondition(conditions: String) {
        when (conditions) {
            "Clear Sky", "Sunny", "Clear" -> {
                findViewById<View>(R.id.main).setBackgroundResource(R.drawable.sunny_background)
                lottieAnimationView.setAnimation(R.raw.sun)
            }
            "Partly Clouds", "Clouds", "Overcast", "Mist", "Foggy" -> {
                findViewById<View>(R.id.main).setBackgroundResource(R.drawable.alwinclouds)
                lottieAnimationView.setAnimation(R.raw.cloud)
            }
            "Light Rain", "Drizzle", "Moderate Rain", "Showers", "Heavy Rain", "Thunderstorm" -> {
                findViewById<View>(R.id.main).setBackgroundResource(R.drawable.rain_background1)
                lottieAnimationView.setAnimation(R.raw.rain)
            }
            "Light Snow", "Snow", "Moderate Snow", "Heavy Snow", "Blizzard" -> {
                findViewById<View>(R.id.main).setBackgroundResource(R.drawable.snow_background)
                lottieAnimationView.setAnimation(R.raw.snow)
            }
            else -> {
                findViewById<View>(R.id.main).setBackgroundResource(R.drawable.sunny_background)
                lottieAnimationView.setAnimation(R.raw.sun)
            }
        }
        lottieAnimationView.playAnimation()
    }

    private fun timeFormat(timeStamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timeStamp * 1000))
    }

    private fun formatDate(timeStamp: Long): String {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        return sdf.format(Date(timeStamp))
    }

    private fun formatDayName(timeStamp: Long): String {
        val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
        return sdf.format(Date(timeStamp))
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun isPointInsideView(x: Float, y: Float, view: View): Boolean {
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val viewX = location[0]
        val viewY = location[1]
        return x > viewX && x < (viewX + view.width) && y > viewY && y < (viewY + view.height)
    }
}
