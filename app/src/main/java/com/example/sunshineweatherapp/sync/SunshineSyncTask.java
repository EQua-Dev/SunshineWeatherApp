package com.example.sunshineweatherapp.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;

import com.example.sunshineweatherapp.data.SunshinePreferences;
import com.example.sunshineweatherapp.data.WeatherContract;
import com.example.sunshineweatherapp.utilities.NetworkUtils;
import com.example.sunshineweatherapp.utilities.NotificationUtils;
import com.example.sunshineweatherapp.utilities.OpenWeatherJsonUtils;

import java.net.URL;

public class SunshineSyncTask {

    synchronized public static void syncWeather(Context context){

        try {     /*
         * The getUrl method will return the URL that we need to get the forecast JSON for the
         * weather. It will decide whether to create a URL based off of the latitude and
         * longitude or off of a simple location as a String.
         */
            URL weatherRequestUrl = NetworkUtils.getUrl(context);

            /* Uses the URL to retrieve the JSON */
            String jsonWeatherResponse = NetworkUtils.getResponseFromHttpUrl(weatherRequestUrl);

            /* Parse the JSON into a list of weather values */
            ContentValues[] weatherValues = OpenWeatherJsonUtils
                    .getWeatherContentValuesFromJson(context, jsonWeatherResponse);

            /*
             * In cases where our JSON contained an error code, getWeatherContentValuesFromJson
             * would have returned null. We need to check for those cases here to prevent any
             * NullPointerExceptions being thrown. We also have no reason to insert fresh data if
             * there isn't any to insert.
             */
            if (weatherValues != null && weatherValues.length != 0) {
                /* Get a handle on the ContentResolver to delete and insert data */
                ContentResolver sunshineContentResolver = context.getContentResolver();

//              If we have valid results, the old data is deleted and the new is inserted
                /* We delete old weather data because we don't need to keep multiple days' data */
                sunshineContentResolver.delete(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        null,
                        null);

                /* Insert our new weather data into Sunshine's ContentProvider */
                sunshineContentResolver.bulkInsert(
                        WeatherContract.WeatherEntry.CONTENT_URI,
                        weatherValues);
            }
//            COMPLETED (13) Check if notifications are enabled
            /*
             * Finally, after we insert data into the ContentProvider, determine whether or not
             * we should notify the user that the weather has been refreshed.
             */
            boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

            /*
             * If the last notification was shown was more than 1 day ago, we want to send
             * another notification to the user that the weather has been updated. Remember,
             * it's important that you shouldn't spam your users with notifications.
             */
            long timeSinceLastNotification = SunshinePreferences
                    .getEllapsedTimeSinceLastNotification(context);

            boolean oneDayPassedSinceLastNotification = false;

//              COMPLETED (14) Check if a day has passed since the last notification
            if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                oneDayPassedSinceLastNotification = true;
            }

            /*
             * We only want to show the notification if the user wants them shown and we
             * haven't shown a notification in the past day.
             */
//              COMPLETED (15) If more than a day have passed and notifications are enabled, notify the user
            if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                NotificationUtils.notifyUserOfNewWeather(context);
            }

            /* If the code reaches this point, we have successfully performed our sync */

        } catch (Exception e) {
            /* Server probably invalid */
            e.printStackTrace();
        }
    }

}