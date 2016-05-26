package tech.michaeloverman.android.mywatch;

import android.app.Activity;
import android.content.ComponentName;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.companion.WatchFaceCompanion;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Michael on 5/26/2016.
 */
public class MyWatchConfigActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        ResultCallback<DataApi.DataItemResult> {

    private String watchFacePeerId;
    private ComponentName mComponentName;
    private GoogleApiClient myGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mywatch_config);
        TextView title = (TextView) findViewById(R.id.title);
        watchFacePeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mComponentName = getIntent().getParcelableExtra(WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);

        myGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

    }

    @Override
    protected void onStart() {
        super.onStart();
        myGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (myGoogleApiClient != null && myGoogleApiClient.isConnected()) {
            myGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (watchFacePeerId != null) {
            // do these things
        } else {
            // Do this if there's no connection
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {

    }
}
