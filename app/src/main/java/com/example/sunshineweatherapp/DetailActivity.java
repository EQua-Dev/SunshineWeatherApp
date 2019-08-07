package com.example.sunshineweatherapp;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ShareCompat;
import androidx.databinding.DataBindingUtil;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.sunshineweatherapp.data.WeatherContract;
import com.example.sunshineweatherapp.databinding.ActivityDetailBinding;
import com.example.sunshineweatherapp.utilities.SunshineDateUtils;
import com.example.sunshineweatherapp.utilities.SunshineWeatherUtils;

public class DetailActivity extends AppCompatActivity implements androidx.loader.app.LoaderManager.LoaderCallbacks<Cursor> {

    private static final String FORECAST_SHARE_HASHTAG = "#SunshineApp";

//    String array containing the names of the desired data columns from the ContentProvider

    public static final String[] WEATHER_DETAIL_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_HUMIDITY,
            WeatherContract.WeatherEntry.COLUMN_PRESSURE,
            WeatherContract.WeatherEntry.COLUMN_WIND_SPEED,
            WeatherContract.WeatherEntry.COLUMN_DEGREES,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID
    };

//    Constant int values representing each column name's position above

    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_HUMIDITY = 3;
    public static final int INDEX_WEATHER_PRESSURE = 4;
    public static final int INDEX_WEATHER_WIND_SPEED = 5;
    public static final int INDEX_WEATHER_DEGREES = 6;
    public static final int INDEX_WEATHER_CONDITION_ID = 7;

    //    constant int that identifies our loader used in DetailActivity
    private static final int ID_DETAIL_LOADER = 353;

    private String mForecastSummary;

    private Uri mUri;

    private ActivityDetailBinding mDetailBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

//            COMPLETED (6) Instantiate mDetailBinding using DataBindingUtil
        mDetailBinding = DataBindingUtil.setContentView(this, R.layout.activity_detail);


//        getData used to get a reference to the URI passed with this Activity's Intent
        mUri = getIntent().getData();

//        Throws a NullPointerException if that URI is null
        if (mUri == null) throw new NullPointerException("URI for DetailActivity caannot be null");

//        Initializes the loader for DetailActivity
//        Connects our Activity into the loader lifecycle
        getSupportLoaderManager().initLoader(ID_DETAIL_LOADER, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

//        Gets the ID of the clicked item
        int id = item.getItemId();

//        Launches the SettingsActivity activity when settings is clicked
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }

        if (id == R.id.action_share) {
            Intent shareIntent = createShareForecastIntent();
            startActivity(shareIntent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Intent createShareForecastIntent() {
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(mForecastSummary + FORECAST_SHARE_HASHTAG)
                .getIntent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return shareIntent;
    }



    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int loaderId, Bundle loaderArgs) {

        switch (loaderId) {

//            returns the appropriate CursorLoader if the loader requested is our detail loader
            case ID_DETAIL_LOADER:
                return new CursorLoader(this,
                        mUri, WEATHER_DETAIL_PROJECTION,
                        null, null, null);

            default:
                throw new RuntimeException("Loader Not Implemented: " + loaderId);
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {


//        checks that the cursor has valid data before performing any operation
        boolean cursorHasValidData = false;
        if (data != null && data.moveToFirst()) {
//            continues on to bind the data to the UI if the have valid data
            cursorHasValidData = true;
        }
        if (!cursorHasValidData) {
//            does nothing; no data to display
            return;
        }


//        COMPLETED (7) Display the weather icon using mDetailBinding
        /****************
         * Weather Icon *
         ****************/
        /* Read weather condition ID from the cursor (ID provided by Open Weather Map) */
        int weatherId = data.getInt(INDEX_WEATHER_CONDITION_ID);
        /* Use our utility method to determine the resource ID for the proper art */
        int weatherImageId = SunshineWeatherUtils.getLargeArtResourceIdForWeatherCondition(weatherId);

        /* Set the resource ID on the icon to display the art */
        mDetailBinding.primaryInfo.weatherIcon.setImageResource(weatherImageId);

//        displays a readable data string

        long localDateMidnightGmt = data.getLong(INDEX_WEATHER_DATE);
        String dateText = SunshineDateUtils.getFriendlyDateString(this, localDateMidnightGmt, true);

//        COMPLETED (8) Use mDetailBinding to display the date
        mDetailBinding.primaryInfo.date.setText(dateText);


        /************
         * Pressure *
         ************/
        /* Read pressure from the cursor */
        float pressure = data.getFloat(INDEX_WEATHER_PRESSURE);  /***********************
         * Weather Description *
         ***********************/
        /* Use the weatherId to obtain the proper description */
        String description = SunshineWeatherUtils.getStringForWeatherCondition(this, weatherId);

//      COMPLETED (15) Create the content description for the description for a11y
        /* Create the accessibility (a11y) String from the weather description */
        String descriptionA11y = getString(R.string.a11y_forecast, description);

//      COMPLETED (9) Use mDetailBinding to display the description and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.primaryInfo.weatherDescription.setText(description);
        mDetailBinding.primaryInfo.weatherDescription.setContentDescription(descriptionA11y);

//      COMPLETED (16) Set the content description of the icon to the same as the weather description a11y text
        /* Set the content description on the weather image (for accessibility purposes) */
        mDetailBinding.primaryInfo.weatherIcon.setContentDescription(descriptionA11y);

        /**************************
         * High (max) temperature *
         **************************/
        /* Read high temperature from the cursor (in degrees celsius) */
        double highInCelsius = data.getDouble(INDEX_WEATHER_MAX_TEMP);
        /*
         * If the user's preference for weather is fahrenheit, formatTemperature will convert
         * the temperature. This method will also append either °C or °F to the temperature
         * String.
         */
        String highString = SunshineWeatherUtils.formatTemperature(this, highInCelsius);

//      COMPLETED (17) Create the content description for the high temperature for a11y
        /* Create the accessibility (a11y) String from the weather description */
        String highA11y = getString(R.string.a11y_high_temp, highString);

//      COMPLETED (10) Use mDetailBinding to display the high temperature and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.primaryInfo.highTemperature.setText(highString);
        mDetailBinding.primaryInfo.highTemperature.setContentDescription(highA11y);

        /*************************
         * Low (min) temperature *
         *************************/
        /* Read low temperature from the cursor (in degrees celsius) */
        double lowInCelsius = data.getDouble(INDEX_WEATHER_MIN_TEMP);
        /*
         * If the user's preference for weather is fahrenheit, formatTemperature will convert
         * the temperature. This method will also append either °C or °F to the temperature
         * String.
         */
        String lowString = SunshineWeatherUtils.formatTemperature(this, lowInCelsius);

//      COMPLETED (18) Create the content description for the low temperature for a11y
        String lowA11y = getString(R.string.a11y_low_temp, lowString);

//      COMPLETED (11) Use mDetailBinding to display the low temperature and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.primaryInfo.lowTemperature.setText(lowString);
        mDetailBinding.primaryInfo.lowTemperature.setContentDescription(lowA11y);

        /************
         * Humidity *
         ************/
        /* Read humidity from the cursor */
        float humidity = data.getFloat(INDEX_WEATHER_HUMIDITY);
        String humidityString = getString(R.string.format_humidity, humidity);

//      COMPLETED (20) Create the content description for the humidity for a11y
        String humidityA11y = getString(R.string.a11y_humidity, humidityString);

//      COMPLETED (12) Use mDetailBinding to display the humidity and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.extraDetails.humidity.setText(humidityString);
        mDetailBinding.extraDetails.humidity.setContentDescription(humidityA11y);

//      COMPLETED (19) Set the content description of the humidity label to the humidity a11y String
        mDetailBinding.extraDetails.humidityLabel.setContentDescription(humidityA11y);

        /****************************
         * Wind speed and direction *
         ****************************/
        /* Read wind speed (in MPH) and direction (in compass degrees) from the cursor  */
        float windSpeed = data.getFloat(INDEX_WEATHER_WIND_SPEED);
        float windDirection = data.getFloat(INDEX_WEATHER_DEGREES);
        String windString = SunshineWeatherUtils.getFormattedWind(this, windSpeed, windDirection);

//      COMPLETED (21) Create the content description for the wind for a11y
        String windA11y = getString(R.string.a11y_wind, windString);

//      COMPLETED (13) Use mDetailBinding to display the wind and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.extraDetails.windMeasurement.setText(windString);
        mDetailBinding.extraDetails.windMeasurement.setContentDescription(windA11y);

        //      COMPLETED (22) Set the content description of the wind label to the wind a11y String
        mDetailBinding.extraDetails.windLabel.setContentDescription(windA11y);



        /*
         * Format the pressure text using string resources. The reason we directly access
         * resources using getString rather than using a method from SunshineWeatherUtils as
         * we have for other data displayed in this Activity is because there is no
         * additional logic that needs to be considered in order to properly display the
         * pressure.
         */
        String pressureString = getString(R.string.format_pressure, pressure);

//      COMPLETED (23) Create the content description for the pressure for a11y
        String pressureA11y = getString(R.string.a11y_pressure, pressureString);

//      COMPLETED (14) Use mDetailBinding to display the pressure and set the content description
        /* Set the text and content description (for accessibility purposes) */
        mDetailBinding.extraDetails.pressure.setText(pressureString);
        mDetailBinding.extraDetails.pressure.setContentDescription(pressureA11y);

//      COMPLETED (24) Set the content description of the pressure label to the pressure a11y String
        mDetailBinding.extraDetails.pressureLabel.setContentDescription(pressureA11y);

        /* Store the forecast summary String in our forecast summary field to share later */
        mForecastSummary = String.format("%s - %s - %s/%s",
                dateText, description, highString, lowString);
    }
}


        /***********************

//        displays the weather description using SunshineWeatherUtils
//        reads the weather condition ID from the cursor from the ID provided by Open Weather Map
        int weatherId = data.getInt(INDEX_WEATHER_CONDITION_ID);

//        obtains the proper description using the weatherId
        String description = SunshineWeatherUtils.getStringForWeatherCondition(this, weatherId);

//        sets the text weather id
        mDescriptionView.setText(description);

//        Reads high temperature in degrees celsius
        double highInCelsius = data.getDouble(INDEX_WEATHER_MAX_TEMP);

        String highString = SunshineWeatherUtils.formatTemperature(this, highInCelsius);

//        sets the text for high temperature
        mHighTemperatureView.setText(highString);

        double lowInCelsius = data.getDouble(INDEX_WEATHER_MIN_TEMP);

        String lowString = SunshineWeatherUtils.formatTemperature(this, lowInCelsius);

//        sets the text for low temperature
        mLowTemperatureView.setText(lowString);

//        Reads humidity from the cursor
        float humidity = data.getFloat(INDEX_WEATHER_HUMIDITY);
        String humidityString = getString(R.string.format_humidity, humidity);

//        Sets the text for humidity
        mHumidityView.setText(humidityString);

//        Reads wind speed in MPH and direction in compass degrees from the cursor
        float windSpeed = data.getFloat(INDEX_WEATHER_WIND_SPEED);
        float windDirection = data.getFloat(INDEX_WEATHER_DEGREES);
        String windString = SunshineWeatherUtils.getFormattedWind(this, windSpeed, windDirection);

//        sets the text for the wind speed and direction
        mWindView.setText(windString);

//        Reads pressure from the cursor
        float pressure = data.getFloat(INDEX_WEATHER_PRESSURE);

        String pressureString = getString(R.string.format_pressure, pressure);

//        sets the text for pressure
        mPressureView.setText(pressureString);

//        stores the forecast String in our forecast summary field to share later
        mForecastSummary = String.format("%s - %s - %s/%s",
                dateText,description,highString,lowString);


    private Intent createShareForecastIntent(){
        Intent shareIntent = ShareCompat.IntentBuilder.from(this)
                .setType("text/plain")
                .setText(mForecastSummary +FORECAST_SHARE_HASHTAG)
                .getIntent();
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        return shareIntent;
    }
}
         **/