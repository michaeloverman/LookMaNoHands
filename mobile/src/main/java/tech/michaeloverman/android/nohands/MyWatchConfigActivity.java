package tech.michaeloverman.android.nohands;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.multidex.MultiDex;
import android.support.v7.app.AppCompatActivity;
import android.support.wearable.companion.WatchFaceCompanion;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorSelectedListener;
import com.flask.colorpicker.builder.ColorPickerClickListener;
import com.flask.colorpicker.builder.ColorPickerDialogBuilder;
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
public class MyWatchConfigActivity extends AppCompatActivity
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
    private int mStepCountGoal;

    private String mColorScheme;

    private String watchFacePeerId;
    private ComponentName mComponentName;
    private GoogleApiClient mGoogleApiClient;

    private CheckBox mShowSecondsBox;
    private CheckBox mShowStepCountBox;
    private CheckBox mShowFootpathBox;
    private CheckBox mShowDateBox;
    private Spinner mGoalStepCountSpinner;
    private Button mHourButton;
    private Button mMinuteButton;
    private Button mSecondsButton;
    private Button mFootpathButton;

    private Button mOkButton;
    private Button mCancelButton;
    private Spinner mColorSchemeSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_mywatch_config);

        watchFacePeerId = getIntent().getStringExtra(WatchFaceCompanion.EXTRA_PEER_ID);
        mComponentName = getIntent().getParcelableExtra(WatchFaceCompanion.EXTRA_WATCH_FACE_COMPONENT);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        mShowSecondsBox = (CheckBox) findViewById(R.id.show_seconds);
        mShowStepCountBox = (CheckBox) findViewById(R.id.show_stepcount);
        mShowFootpathBox = (CheckBox) findViewById(R.id.show_footpath);
        mShowDateBox = (CheckBox) findViewById(R.id.show_date);

        mGoalStepCountSpinner = (Spinner) findViewById(R.id.stepcount_goal_spinner);
        mGoalStepCountSpinner.setSelection(9);
        mGoalStepCountSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                System.out.println("StepCountGoalSpinner onItemSelected()");
                mStepCountGoal = (position * 1000) + 1000;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mColorSchemeSpinner = (Spinner) findViewById(R.id.color_schemes);
        mColorSchemeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setColorsToPresets(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        View.OnClickListener colorClick = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                colorButtonClicked(v);
            }
        };

        mHourButton = (Button) findViewById(R.id.hours_color_button);
        mHourButton.setOnClickListener(colorClick);
        mMinuteButton = (Button) findViewById(R.id.minutes_color_button);
        mMinuteButton.setOnClickListener(colorClick);
        mSecondsButton = (Button) findViewById(R.id.seconds_color_button);
        mSecondsButton.setOnClickListener(colorClick);
        mFootpathButton = (Button) findViewById(R.id.footpath_color_button);
        mFootpathButton.setOnClickListener(colorClick);

        mOkButton = (Button) findViewById(R.id.okay_button);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                okButtonClicked(v);
            }
        });

        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelButtonClicked(v);
            }
        });

    }

    private void setColorsToPresets(int position) {
        String choice = mColorSchemeSpinner.getItemAtPosition(position).toString();

        switch(choice) {
            case "WVU":
                System.out.println("WVU colors selected!!");
                setButtonColors(R.color.wvu_hour, R.color.wvu_minute, R.color.wvu_second, R.color.wvu_footpath);
                break;
            case "Northwestern":
                setButtonColors(R.color.northwestern_hour, R.color.northwestern_minute, R.color.northwestern_second, R.color.northwestern_footpath);
                break;
            case "JMU":
                setButtonColors(R.color.jmu_hour_text, R.color.jmu_minute, R.color.jmu_second, R.color.jmu_footpath);
                break;
            case "Clemson":
                setButtonColors(R.color.clemson_hour, R.color.clemson_minute, R.color.clemson_second, R.color.clemson_footpath);
                break;
            case "Virginia Tech":
                setButtonColors(R.color.vatech_hour, R.color.vatech_minute, R.color.vatech_second, R.color.vatech_footpath);
                break;
            case "UVA":
                setButtonColors(R.color.uva_hour, R.color.uva_minute, R.color.uva_second, R.color.uva_footpath);
                break;
            case "William and Mary":
                setButtonColors(R.color.wandm_hour, R.color.wandm_minute, R.color.wandm_second, R.color.wandm_footpath);
                break;
            case "The Ohio State":
                setButtonColors(R.color.ohio_hour, R.color.ohio_minute, R.color.ohio_second, R.color.ohio_footpath);
                break;
            case "Alabama":
                setButtonColors(R.color.alabama_hour, R.color.alabama_minute, R.color.alabama_second, R.color.alabama_footpath);
                break;
            case "Harvard":
                setButtonColors(R.color.harvard_hour, R.color.harvard_minute, R.color.harvard_second, R.color.harvard_footpath);
                break;
            case "Ithaca College":
                setButtonColors(R.color.ic_hour, R.color.ic_minute, R.color.ic_second, R.color.ic_footpath);
                break;
            case "Cornell University":
                setButtonColors(R.color.cornell_hour, R.color.cornell_minute, R.color.cornell_second, R.color.cornell_footpath);
                break;
            case "Penn State":
                setButtonColors(R.color.penn_hour, R.color.penn_minute, R.color.penn_second, R.color.penn_footpath);
                break;
            case "UCLA":
                setButtonColors(R.color.ucla_hour, R.color.ucla_minute, R.color.ucla_second, R.color.ucla_footpath);
                break;
            case "U Kentucky":
                setButtonColors(R.color.uky_hour, R.color.uky_minute, R.color.uky_second, R.color.uky_footpath);
                break;
            case "U Kansas":
                setButtonColors(R.color.ku_hour, R.color.ku_minute, R.color.ku_second, R.color.ku_footpath);
                break;
            case "Nature":
                setButtonColors(R.color.nature_hour, R.color.nature_minute, R.color.nature_second, R.color.nature_footpath);
                break;
            case "Fresh":
                setButtonColors(R.color.fresh_hour, R.color.fresh_minute, R.color.fresh_second, R.color.fresh_footpath);
                break;
            case "Icy":
                setButtonColors(R.color.icy_hour, R.color.icy_minute, R.color.icy_second, R.color.icy_footpath);
                break;
            case "Nautical":
                setButtonColors(R.color.nautical_hour, R.color.nautical_minute, R.color.nautical_second, R.color.nautical_footpath);
                break;
            case "Mikah's Colors":
                setButtonColors(R.color.mikah_hour, R.color.mikah_minute, R.color.mikah_second, R.color.mikah_footpath);
                break;
            case "Browns":
                setButtonColors(R.color.browns_hour, R.color.browns_minute, R.color.browns_second, R.color.browns_footpath);
                break;
            case "Pastels":
                setButtonColors(R.color.pastels_hour, R.color.pastels_minute, R.color.pastels_second, R.color.pastels_footpath);
                break;
            case "Grays":
                setButtonColors(R.color.grays_hour, R.color.grays_minute, R.color.grays_second, R.color.grays_footpath);
                break;
            case "Greece":
                setButtonColors(R.color.greece_hour, R.color.greece_minute, R.color.greece_second, R.color.greece_footpath);
                break;
            case "Rachael's Colors":
                setButtonColors(R.color.rachael_hour, R.color.rachael_minute, R.color.rachael_second, R.color.rachael_footpath);
                break;
            default:
                break;
        }
    }

    private void setButtonColors(int h, int m, int s, int f) {
        System.out.println("h: " + h + ", M: " + m + ", Sec: " + s);
        mHourButton.setBackgroundColor(getResources().getColor(h));
        mMinuteButton.setBackgroundColor(getResources().getColor(m));
        mSecondsButton.setBackgroundColor(getResources().getColor(s));
        mFootpathButton.setBackgroundColor(getResources().getColor(f));
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
        dataMap.putInt("stepcount_goal", mStepCountGoal);

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

        finish();
    }

    @Override
    protected void onStart() {
        System.out.println("in onStart()");
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        System.out.println("in onStop()");
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }

        super.onStop();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

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
            }

            dataEventBuffer.release();

        }
    };

    private void updateParamsForDataItem(DataItem item) {
        if ((item.getUri().getPath()).equals("/watch_face_config_nohands")) {

            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
            if (dataMap.containsKey("hour_color")) {
                mHourColor = dataMap.getInt("hour_color");
                mHourButton.setBackgroundColor(mHourColor);
            }
            if (dataMap.containsKey("minute_color")) {
                mMinuteColor = dataMap.getInt("minute_color");
                mMinuteButton.setBackgroundColor(mMinuteColor);
            }
            if (dataMap.containsKey("seconds_color")) {
                mSecondsColor = dataMap.getInt("seconds_color");
                mSecondsButton.setBackgroundColor(mSecondsColor);
            }
            if (dataMap.containsKey("footpath_color")) {
                mFootpathColor = dataMap.getInt("footpath_color");
                mFootpathButton.setBackgroundColor(mFootpathColor);
            }
            if (dataMap.containsKey("show_seconds")) {
                mShowSeconds = dataMap.getBoolean("show_seconds");
                mShowSecondsBox.setChecked(mShowSeconds);
            }
            if (dataMap.containsKey("show_stepcount")) {
                mShowStepCount = dataMap.getBoolean("show_stepcount");
                mShowStepCountBox.setChecked(mShowStepCount);
            }
            if (dataMap.containsKey("show_footpath")) {
                mShowFootpath = dataMap.getBoolean("show_footpath");
                mShowFootpathBox.setChecked(mShowFootpath);
            }
            if (dataMap.containsKey("show_date")) {
                mShowDate = dataMap.getBoolean("show_date");
                mShowDateBox.setChecked(mShowDate);
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


    public void colorButtonClicked(View v) {

        final Button button = (Button) v;
        ColorPickerDialogBuilder
                .with(this)
                .setTitle("Choose color")
                .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
                .density(8)
                .setOnColorSelectedListener(new OnColorSelectedListener() {
                    @Override
                    public void onColorSelected(int selectedColor) {
           //             Toast("onColorSelected: 0x" + Integer.toHexString(selectedColor));
                    }
                })
                .setPositiveButton("ok", new ColorPickerClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int selectedColor, Integer[] allColors) {
                        button.setBackgroundColor(selectedColor);
                        int id = button.getId();
                        switch (id) {
                            case R.id.hours_color_button:
                                mHourColor = selectedColor;
                                break;
                            case R.id.minutes_color_button:
                                mMinuteColor = selectedColor;
                                break;
                            case R.id.seconds_color_button:
                                mSecondsColor = selectedColor;
                                break;
                            case R.id.footpath_color_button:
                                mFootpathColor = selectedColor;
                                break;
                        }

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .build()
                .show();

    }

    public void okButtonClicked(View v) {
        mHourColor = ((ColorDrawable) mHourButton.getBackground()).getColor();
        mMinuteColor = ((ColorDrawable) mMinuteButton.getBackground()).getColor();
        mSecondsColor = ((ColorDrawable) mSecondsButton.getBackground()).getColor();
        mFootpathColor = ((ColorDrawable) mFootpathButton.getBackground()).getColor();
        mShowSeconds = ((CheckBox) findViewById(R.id.show_seconds)).isChecked();
        mShowStepCount = ((CheckBox) findViewById(R.id.show_stepcount)).isChecked();
        mShowFootpath = ((CheckBox) findViewById(R.id.show_footpath)).isChecked();
        mShowDate = ((CheckBox) findViewById(R.id.show_date)).isChecked();
        //mColorScheme = ((Spinner) findViewById(R.id.color_schemes)).getSelectedItem().toString();
        sendParamsAndFinish();
    }
    public void cancelButtonClicked(View v) {
        sendParamsAndFinish();
    }

    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
 }
