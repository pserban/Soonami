package com.example.android.soonami;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;

public class TsunamiAsyncTask extends AsyncTask<URL, Void, Event> {

    private final static String LOG_TAG = TsunamiAsyncTask.class.getSimpleName();

    private final MainActivity mMainActivity;

    public TsunamiAsyncTask(MainActivity activity) {
        mMainActivity = activity;
    }

    @Override
    protected Event doInBackground(URL... urls) {
        // Create URL object
        URL url = createUrl(MainActivity.USGS_REQUEST_URL);

        // Perform HTTP request to the URL and receive a JSON response back
        String jsonResponse = "";
        try {
            jsonResponse = makeHttpRequest(url);
        } catch (IOException e) {
            // TODO Handle the IOException
        }

        // Extract relevant fields from the JSON response and create an {@link Event} object
        Event earthquake = extractFeatureFromJSON(jsonResponse);

        // Return the {@link Event object as the result of the {@link TsunamiAsyncTask}
        return earthquake;
    }

    /**
     * Update the screen with the earthquake (which was the result of the
     * {@link TsunamiAsyncTask}).
     */
    @Override
    protected void onPostExecute(Event earthquake) {
        if (earthquake == null) {
            return;
        }

        mMainActivity.updateUi(earthquake);
    }

    /**
     * Returns new URL object from the given string URL.
     */
    private URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException exception) {
            Log.e(LOG_TAG, "Error with creating URL", exception);
            return null;
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return a String as the response.
     */
    private String makeHttpRequest(URL url) throws IOException {
        String jsonResponse = "";
        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.setReadTimeout(10000);
            urlConnection.setConnectTimeout(15000);
            urlConnection.connect();
            inputStream = urlConnection.getInputStream();
            jsonResponse = readFromStream(inputStream);
        } catch (IOException e) {
            // TODO: Handle the exception
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (inputStream != null) {
                // function must handle java.io.IOException here
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String which contains the
     * whole JSON response from the server.
     */
    private String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (inputStream != null) {
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (line != null) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Return an {@link Event} object by parsing out information
     * about the first earthquake from the input earthquakeJSON string.
     */
    private Event extractFeatureFromJSON(String earthquakeJSON) {
        try {
            JSONObject baseJsonResponse = new JSONObject(earthquakeJSON);
            JSONArray featureArray = baseJsonResponse.getJSONArray("features");

            // If there are results in the features array
            if (featureArray.length() > 0) {
                // Extract out the first feature (which is an earthquake)
                JSONObject firstFeature = featureArray.getJSONObject(0);
                JSONObject properties = firstFeature.getJSONObject("properties");

                // Extract out the title, time, and tsunami values
                String title = properties.getString("title");
                long time = properties.getLong("time");
                int tsunamiAlert = properties.getInt("tsunami");

                // Create a new {@link Event} object
                return new Event(title, time, tsunamiAlert);
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Problem parsing the earthquake JSON results", e);
        }
        return null;
    }
}
