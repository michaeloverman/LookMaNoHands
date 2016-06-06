package tech.michaeloverman.android.mywatch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.companion.WatchFaceCompanion;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
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

    private static final String PATH_WITH_FEATURE = "/mywatch_config/MyWatch";
    private static final String KEY_COLOR_HOURS = "COLOR_HOURS";
    private static final String KEY_COLOR_MINUTES = "COLOR_MINUTES";
    private static final String KEY_COLOR_SECONDS = "COLOR_SECONDS";
    private static final String KEY_SHOW_SECONDS = "SHOW_SECONDS";

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

        Button OKbutton = (Button) findViewById(R.id.okay_button);
        OKbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // get details of each config

                // assign to variables


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
        Wearable.DataApi.putDataItem(myGoogleApiClient, putDataRequest);

        finish();
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
    /*    if (watchFacePeerId != null) {
            Uri.Builder uriBuilder = new Uri.Builder();
            Uri uri = uriBuilder.scheme("wear")
                    .path(PATH_WITH_FEATURE)
                    .authority(watchFacePeerId)
                    .build();
            Wearable.DataApi.getDataItem(myGoogleApiClient, uri).setResultCallback(this);

        } else {
            noConnectedDeviceDialog();
        } */
    }

    private void noConnectedDeviceDialog() {
        String noConnectText = getResources().getString(R.string.no_connected_device);
        String okButtonLabel = getResources().getString(R.string.ok_button_label);
        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
        alertBuilder.setMessage(noConnectText)
                .setCancelable(false)
                .setPositiveButton(okButtonLabel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
        AlertDialog alertDialog = alertBuilder.create();
        alertDialog.show();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        
    }

    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {
        if (dataItemResult.getStatus().isSuccess() && dataItemResult.getDataItem() != null) {
            DataItem configDataItem = dataItemResult.getDataItem();
            DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
            DataMap configDataMap = dataMapItem.getDataMap();
        } else {

        }
    }

    private void setUpAllPickers(DataMap configData) {
        setUpColorPickerSelection(R.id.hours, KEY_COLOR_HOURS, configData, R.string.color_purple);
        setUpColorPickerSelection(R.id.minutes, KEY_COLOR_MINUTES, configData, R.string.color_orange);
        setUpColorPickerSelection(R.id.seconds, KEY_COLOR_SECONDS, configData, R.string.color_white);

        setUpColorPickerListener(R.id.hours, KEY_COLOR_HOURS);
        setUpColorPickerListener(R.id.minutes, KEY_COLOR_MINUTES);
        setUpColorPickerListener(R.id.seconds, KEY_COLOR_SECONDS);
    }
    private void setUpColorPickerSelection(int spinnerId, final String configKey,
                                           DataMap config, int defaultColorNameResId) {
        String defaultColorName = getString(defaultColorNameResId);
        int defaultColor = Color.parseColor(defaultColorName);
        int color;

        if(config != null) {
            color = config.getInt(configKey, defaultColor);
        } else {
            color = defaultColor;
        }

        Spinner spinner = (Spinner) findViewById(spinnerId);
        String[] colorNames = getResources().getStringArray(R.array.color_array);
        for (int i = 0; i < colorNames.length; i++) {
            if (Color.parseColor(colorNames[i]) == color) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    private void setUpColorPickerListener(int spinnerId, final String configKey) {
        Spinner spinner = (Spinner) findViewById(spinnerId);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                final String colorName = (String) parent.getItemAtPosition(position);
                sendConfigUpdateMessage(configKey, Color.parseColor(colorName));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void sendConfigUpdateMessage(String configKey, int color) {
        if (watchFacePeerId != null) {
            DataMap newConfig = new DataMap();
            newConfig.putInt(configKey, color);
            byte[] rawConfigData = newConfig.toByteArray();
            Wearable.MessageApi.sendMessage(myGoogleApiClient, watchFacePeerId,
                    PATH_WITH_FEATURE, rawConfigData);
        }
    }

}
