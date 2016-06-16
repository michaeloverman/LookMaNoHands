package tech.michaeloverman.android.mywatch;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.WearableListView;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
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
        WearableListView.ClickListener, WearableListView.OnScrollListener {

    private GoogleApiClient mGoogleApiClient;
    private boolean mShowSeconds;
    private boolean mShowDate;
    private boolean mShowFootpath;
    private boolean mShowStepCount;

    private TextView mHeader;
    private int mStepCountGoal = 10000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_config);
    //    mHeader = (TextView) findViewById(R.id.header);
        WearableListView listView = (WearableListView) findViewById(R.id.config_list);
        BoxInsetLayout content = (BoxInsetLayout) findViewById(R.id.content);
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

        listView.setHasFixedSize(true);
        listView.setClickListener(this);
        listView.addOnScrollListener(this);

        String[] options = getResources().getStringArray(R.array.config_options_array);
        listView.setAdapter(new ConfigOptionsAdapter(options));

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
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        super.onStop();
    }

    private void sendParamsAndFinish() {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create("/watch_face_config_nohands");
        DataMap dataMap = putDataMapRequest.getDataMap();
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

    private class ConfigOptionsAdapter extends WearableListView.Adapter {
        private final String[] mOptions;

        public ConfigOptionsAdapter(String[] options) {
            mOptions = options;
        }

        @Override
        public WearableListView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return null;
        }

        @Override
        public void onBindViewHolder(WearableListView.ViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }
}
