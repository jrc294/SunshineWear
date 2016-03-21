package com.example.android.sunshine.app;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p/>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class SendDataToWearableService extends IntentService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient mGoogleApiClient;
    public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();

    public SendDataToWearableService() {
        super("SendDataToWearableService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mGoogleApiClient.connect();

    }

    @Override
    public void onConnected(Bundle bundle) {
        if (mGoogleApiClient.isConnected()) {
            String locationQuery = Utility.getPreferredLocation(this);
            Uri weatherUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(locationQuery, System.currentTimeMillis());
            Cursor cursor = getContentResolver().query(weatherUri, SunshineSyncAdapter.NOTIFY_WEATHER_PROJECTION, null, null, null);
            if (cursor.moveToFirst()) {
                double high = cursor.getDouble(SunshineSyncAdapter.INDEX_MAX_TEMP);
                double low = cursor.getDouble(SunshineSyncAdapter.INDEX_MIN_TEMP);
                int weatherId = cursor.getInt(SunshineSyncAdapter.INDEX_WEATHER_ID);
                int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
                Bitmap bmpIcon = BitmapFactory.decodeResource(getResources(), iconId);
                Asset asset = createAssetFromBitmap(bmpIcon);
                PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/temperature");
                putDataMapRequest.getDataMap().putString("high-temp", Utility.formatTemperature(this, high));
                putDataMapRequest.getDataMap().putString("low-temp", Utility.formatTemperature(this, low));
                putDataMapRequest.getDataMap().putAsset("weather-icon", asset);
                putDataMapRequest.getDataMap().putLong("time", System.currentTimeMillis());
                PutDataRequest request = putDataMapRequest.asPutDataRequest().setUrgent();
                Wearable.DataApi.putDataItem(mGoogleApiClient, request).setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        if (!dataItemResult.getStatus().isSuccess()) {
                            Log.i(LOG_TAG, "Data sent error");
                        } else {
                            Log.i(LOG_TAG, "Data sent ok");
                        }
                    }
                });
            }
        }
    }

    private static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
