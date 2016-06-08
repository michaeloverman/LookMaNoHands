package tech.michaeloverman.android.mywatch;

import android.app.Activity;
import android.content.ComponentName;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.companion.WatchFaceCompanion;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.Switch;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Michael on 5/26/2016.
 */
public class MyWatchConfigActivity extends Activity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private int mHourColor;
    private int mMinuteColor;
    private int mSecondsColor;
    private int mFootpathColor;
    private boolean mShowSeconds;
    private boolean mShowStepCount;
    private boolean mShowFootpath;
    private boolean mShowDate;

    private String watchFacePeerId;
    private ComponentName mComponentName;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mywatch_config);

    //    TextView title = (TextView) findViewById(R.id.title);
        watchFacePeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mComponentName = getIntent().getParcelableExtra(WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        Button OKbutton = (Button) findViewById(R.id.okay_button);
        OKbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get details of each config // assign to variables
                mHourColor = getColorInt(((Spinner) findViewById(R.id.hours)).getSelectedItem().toString());
                mMinuteColor = getColorInt(((Spinner) findViewById(R.id.minutes)).getSelectedItem().toString());
                mSecondsColor = getColorInt(((Spinner) findViewById(R.id.seconds)).getSelectedItem().toString());
                mFootpathColor = getColorInt(((Spinner) findViewById(R.id.footpath)).getSelectedItem().toString());
                mShowSeconds = ((CheckBox) findViewById(R.id.show_seconds)).isChecked();
                mShowStepCount = ((Switch) findViewById(R.id.show_stepcount)).isChecked();
                mShowFootpath = ((Switch) findViewById(R.id.show_footpath)).isChecked();
                mShowDate = ((Switch) findViewById(R.id.show_date)).isChecked();

                System.out.println("data retrieved, sending to wear");

                sendParamsAndFinish();
            }
        });

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendParamsAndFinish();
            }
        });
    }

    private int getColorInt(String colorString) {
        switch (colorString) {
            case "Red":
                return Color.RED;
            case "Orange":
                return 0;
            case "Yellow":
                return Color.YELLOW;
            case "Green":
                return Color.GREEN;
            case "Blue":
                return Color.BLUE;
            case "Purple":
                return Color.MAGENTA;
            case "White":
            default:
                return Color.WHITE;

        }
    }
    private void sendParamsAndFinish() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config_nohands");
        DataMap dataMap = putDataMapRequest.getDataMap();
        dataMap.putInt("hour_color", mHourColor);
        dataMap.putInt("minute_color", mMinuteColor);
        dataMap.putInt("seconds_color", mSecondsColor);
        dataMap.putInt("footpath_color", mFootpathColor);
        dataMap.putBoolean("show_seconds", mShowSeconds);
        dataMap.putBoolean("show_stepcount", mShowStepCount);
        dataMap.putBoolean("show_footpath", mShowFootpath);
        dataMap.putBoolean("show_date", mShowDate);

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
    /*    if (watchFacePeerId != null) {
            Uri.Builder uriBuilder = new Uri.Builder();
            Uri uri = uriBuilder.scheme("wear")
                    .path(PATH_WITH_FEATURE)
                    .authority(watchFacePeerId)
                    .build();
            Wearable.DataApi.getDataItem(mGoogleApiClient, uri).setResultCallback(this);

        } else {
            noConnectedDeviceDialog();
        } */
        Wearable.DataApi.addListener(mGoogleApiClient, onDataChangedListener);
        Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);



    }

    private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {
        //    System.out.println("onDataChangedListener");

        @Override
        public void onDataChanged(DataEventBuffer dataEventBuffer) {
            for (DataEvent event : dataEventBuffer) {
                if (event.getType() == DataEvent.TYPE_CHANGED) {
                    DataItem item = event.getDataItem();
                    updateParamsForDataItem(item);
                }

                // set defaults on view items

            }

            dataEventBuffer.release();

        }
    };

    private void updateParamsForDataItem(DataItem item) {
        if ((item.getUri().getPath()).equals("/watch_face_config_nohands")) {

            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            if (dataMap.containsKey("hour_color")) {
                mHourColor = dataMap.getInt("hour_color");
            }
            if (dataMap.containsKey("minute_color")) {
                mMinuteColor = dataMap.getInt("minute_color");
            }
            if (dataMap.containsKey("seconds_color")) {
                mSecondsColor = dataMap.getInt("seconds_color");
            }
            if (dataMap.containsKey("footpath_color")) {
                mFootpathColor = dataMap.getInt("footpath_color");
            }
            if (dataMap.containsKey("show_seconds")) {
                mShowSeconds = dataMap.getBoolean("show_seconds");
            }
            if (dataMap.containsKey("show_stepcount")) {
                mShowStepCount = dataMap.getBoolean("show_stepcount");
            }
            if (dataMap.containsKey("show_footpath")) {
                mShowFootpath = dataMap.getBoolean("show_footpath");
            }
            if (dataMap.containsKey("show_date")) {
                mShowDate = dataMap.getBoolean("show_date");
            }

        }
    }

    final ResultCallback<DataItemBuffer> onConnectedResultCallback =
                new ResultCallback<DataItemBuffer>() {

                    //    System.out.println("onConnectedResultCallback");

                    @Override
                    public void onResult(@NonNull DataItemBuffer dataItems) {
                        for (DataItem item : dataItems) {
                            updateParamsForDataItem(item);
                        }

                        dataItems.release();

                    }
                };

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        
    }





}
