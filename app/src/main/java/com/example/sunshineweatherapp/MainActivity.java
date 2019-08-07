package com.example.sunshineweatherapp;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sunshineweatherapp.data.SunshinePreferences;
import com.example.sunshineweatherapp.data.WeatherContract;
import com.example.sunshineweatherapp.sync.SunshineSyncUtils;

public class MainActivity extends AppCompatActivity implements
        ForecastAdapter.ForecastAdapterOnClickHandler,
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = MainActivity.class.getSimpleName();

//    Creates a String array containing the names of the desired data columns from our ContentProvider
    /*
     * The columns of data that we are interested in displaying within our MainActivity's list of
     * weather data.
     */
    public static final String[] MAIN_FORECAST_PROJECTION = {
            WeatherContract.WeatherEntry.COLUMN_DATE,
            WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
            WeatherContract.WeatherEntry.COLUMN_MIN_TEMP,
            WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,};

//    Creates constant int values representing each column name's position above
    /*
     * We store the indices of the values in the array of Strings above to more quickly be able to
     * access the data from our query. If the order of the Strings above changes, these indices
     * must be adjusted to match the order of the Strings.
     */
    public static final int INDEX_WEATHER_DATE = 0;
    public static final int INDEX_WEATHER_MAX_TEMP = 1;
    public static final int INDEX_WEATHER_MIN_TEMP = 2;
    public static final int INDEX_WEATHER_CONDITION_ID = 3;

    private static final int FORECAST_LOADER_ID = 44;

    private RecyclerView mRecyclerView;
    private ForecastAdapter mForecastAdapter;
    private int mPosition = RecyclerView.NO_POSITION;
    private ProgressBar mPb_loading;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forecast);
        getSupportActionBar().setElevation(0f);


        mRecyclerView = findViewById(R.id.recyclerview_forecast);


//        Invokes LinearLinearManager on the RecyclerView_forecast
        LinearLayoutManager layoutManager
                = new LinearLayoutManager(this, RecyclerView.VERTICAL, false);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);
//        Assigns the ForecastAdapter to be loaded into the MainActivity
        mForecastAdapter = new ForecastAdapter(this, this);

        mRecyclerView.setAdapter(mForecastAdapter);


        mPb_loading = findViewById(R.id.pb_loading);

        showLoading();

//        Ensures a loader is initialized and active when the activity/fragment is started

        getLoaderManager().initLoader(FORECAST_LOADER_ID,null,this);

//        calls SunshineSyncUtils's startImmediateSync method on the main thread
        SunshineSyncUtils.initialize(this);
        }


    //    In this method, when the data is shown, the error message in made invisible and the layout containing the data is made visible
    public void showWeatherDataView() {
        mPb_loading.setVisibility(View.INVISIBLE);
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showLoading(){
//        hides the weather data
        mRecyclerView.setVisibility(View.INVISIBLE);
//        shows the loading indicator
        mPb_loading.setVisibility(View.VISIBLE);
    }

    //    This object provides an intent when  a particular weather detail is clicked
    @Override
    public void onClick(long date) {

//
        Intent weatherDetailIntent = new Intent(MainActivity.this, DetailActivity.class);

//                Passes the URI for the clicked date with the Intent
                Uri uriForDateClicked = WeatherContract.WeatherEntry.buildWeatherUriWithDate(date);
        weatherDetailIntent.setData(uriForDateClicked);
        startActivity(weatherDetailIntent);

    }


//    This method starts the AsyncTaskLoader thread for internet data collection
//    AsyncTaskLoader is an advancement to AsyncTask




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.forecast, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

//        Launches the map when the map menu item is clicked
        if (id == R.id.action_map) {
            openLocationInMap();
            return true;
        }
//        Launches the settings activity when the settings option is clicked
        if (id == R.id.action_settings){
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;

        }

        return super.onOptionsItemSelected(item);
    }

    //Gives the Uri and map address of the preferred location
    private void openLocationInMap() {

        String addressString = SunshinePreferences.getPreferredWeatherLocation(this);
        Uri geoLocation = Uri.parse("geo:0,0?q=" + addressString);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(geoLocation);

        if (getIntent().resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Log.d(TAG, "Couldn't Call" + geoLocation.toString() + ",  no receiving apps installed");

        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int loaderid, final Bundle bundle) {

        switch (loaderid) {
            case FORECAST_LOADER_ID:

                Uri forecastQueryUri = WeatherContract.WeatherEntry.CONTENT_URI;

                String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATE + " ASC";

                String selection = WeatherContract.WeatherEntry.getSqlSelectForTodayOnwards();

        return new CursorLoader(this,forecastQueryUri,
                MAIN_FORECAST_PROJECTION,selection,
                null,sortOrder);

        default:
            throw new RuntimeException("Loader Not Implemented: " + loaderid);
    }



        }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

            mForecastAdapter.swapCursor(data);

            if (mPosition == RecyclerView.NO_POSITION) mPosition = 0;

//            Smooth Scrolls the RecyclerView to mPosition
            mRecyclerView.smoothScrollToPosition(mPosition);

//            calls showWeatherDataView if the Cursor's size is not equal to 0
            if (data.getCount() != 0) showWeatherDataView();
        }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        //        calls mForecastAdapter's swapCursor method and pass null
        mForecastAdapter.swapCursor(null);
    }
}

