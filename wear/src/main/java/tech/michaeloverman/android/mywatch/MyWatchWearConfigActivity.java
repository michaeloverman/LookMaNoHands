package tech.michaeloverman.android.mywatch;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.BoxInsetLayout;
import android.view.View;
import android.view.WindowInsets;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Michael on 6/6/2016.
 */
public class MyWatchWearConfigActivity extends Activity implements
        /*WearableListView.ClickListener, WearableListView.OnScrollListener*/
        View.OnClickListener {

    private GoogleApiClient mGoogleApiClient;
    private boolean mShowSeconds;
    private boolean mSecondsChanged = false;
    private boolean mShowDate;
    private boolean mDateChanged = false;
    private boolean mShowFootpath;
    private boolean mFootpathChanged = false;
    private boolean mShowStepCount;
    private boolean mStepCountChanged = false;
    private CheckBox mSecondsCheck, mDateCheck, mFootpathCheck, mStepcountCheck;

    private TextView mHeader;
    private int mStepCountGoal = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);

        mSecondsCheck = (CheckBox) findViewById(R.id.show_seconds_check);
        mSecondsCheck.setOnClickListener(this);
        mDateCheck = (CheckBox) findViewById(R.id.show_date_check);
        mDateCheck.setOnClickListener(this);
        mFootpathCheck = (CheckBox) findViewById(R.id.show_footpath_check);
        mFootpathCheck.setOnClickListener(this);
        mStepcountCheck = (CheckBox) findViewById(R.id.show_stepcount_check);
        mStepcountCheck.setOnClickListener(this);

        // BoxInsetLayout adds padding by default on round devices. Add some on square devices.
        content.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                if (!insets.isRound()) {
                    v.setPaddingRelative(
                            (int) getResources().getDimensionPixelSize(R.dimen.content_padding_start),
                            v.getPaddingTop(),
                            v.getPaddingEnd(),
                            v.getPaddingBottom());
                }
                return v.onApplyWindowInsets(insets);
            }
        });


        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
                    @Override
                    public void onConnected(@Nullable Bundle bundle) {

                    }

                    @Override
                    public void onConnectionSuspended(int i) {

                    }
                })
                .addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        sendParamsAndFinish();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void sendParamsAndFinish() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config_nohands");
        DataMap dataMap = putDataMapRequest.getDataMap();
        if (mSecondsChanged) dataMap.putBoolean("show_seconds", mShowSeconds);
        if (mStepCountChanged) dataMap.putBoolean("show_stepcount", mShowStepCount);
        if (mFootpathChanged) dataMap.putBoolean("show_footpath", mShowFootpath);
        if (mDateChanged) dataMap.putBoolean("show_date", mShowDate);
//        dataMap.putInt("stepcount_goal", mStepCountGoal);

        PutDataRequest putDataRequest = putDataMapRequest.asPutDataRequest();
        Wearable.DataApi.putDataItem(mGoogleApiClient, putDataRequest);

        finish();
    }

/*
    @Override
    public void onClick(WearableListView.ViewHolder viewHolder) {

    }

    @Override
    public void onTopEmptyRegionClick() {
    }

    @Override
    public void onScroll(int i) {
    }

    @Override
    public void onAbsoluteScrollChange(int i) {

    }

    @Override
    public void onScrollStateChanged(int i) {
    }

    @Override
    public void onCentralPositionChanged(int i) {
    }
*/

    @Override
    public void onClick(View v) {
        CheckBox choice = (CheckBox) v;
        Boolean state = choice.isChecked();
        if (mSecondsCheck.equals(choice)) {
            mShowSeconds = state;
            mSecondsChanged = true;
        } else if (mDateCheck.equals(choice)) {
            mShowDate = state;
            mDateChanged = true;
        } else if (mFootpathCheck.equals(choice)) {
            mShowFootpath = state;
            mFootpathChanged = true;
        } else if (mStepcountCheck.equals(choice)) {
            mShowStepCount = state;
            mStepCountChanged = true;
        }
    }

}
