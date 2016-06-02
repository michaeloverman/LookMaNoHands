package tech.michaeloverman.android.mywatch;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.concurrent.TimeUnit;

/**
 * Created by overm on 5/31/2016.
 */
public class MyWatchConfigListenerService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient myGoogleApiClient;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        byte[] rawData = messageEvent.getData();
        DataMap keysToOverwrite = DataMap.fromByteArray(rawData);

        if (myGoogleApiClient == null) {
            myGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }
        if (!myGoogleApiClient.isConnected()) {
            ConnectionResult myConnectionResult = myGoogleApiClient.blockingConnect(30, TimeUnit.SECONDS);
        }
        MyWatchUtility.overwriteKeysInConfigDataMap(myGoogleApiClient, keysToOverwrite);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
