package tech.michaeloverman.android.mywatch;

import android.graphics.Color;
import android.net.Uri;
import android.support.annotation.NonNull;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Michael on 5/31/2016.
 */
public final class MyWatchUtility {
    public static final String PATH_WITH_FEATURE = "/mywatch_config/MyWatch";
    public static final String KEY_COLOR_HOURS = "COLOR_HOURS";
    public static final String KEY_COLOR_MINUTES = "COLOR_MINUTES";
    public static final String KEY_COLOR_SECONDS = "COLOR_SECONDS";
    public static final String KEY_SHOW_SECONDS = "SHOW_SECONDS";

    public static final String COLOR_HOURS_INTERACTIVE = "Purple";
    public static final String COLOR_MINUTES_INTERACTIVE = "Orange";
    public static final String COLOR_SECONDS_INTERACTIVE = "White";
    public static final String SHOW_SECONDS_INTERACTIVE = "false";

    private static int parseOptionColor(String optionColor) {
        return Color.parseColor(optionColor.toLowerCase());
    }

    public static final int COLOR_VALUE_HOURS_INTERACTIVE =
            parseOptionColor(COLOR_HOURS_INTERACTIVE);
    public static final int COLOR_VALUE_MINUTES_INTERACTIVE =
            parseOptionColor(COLOR_MINUTES_INTERACTIVE);
    public static final int COLOR_VALUE_SECONDS_INTERACTIVE =
            parseOptionColor(COLOR_SECONDS_INTERACTIVE);

    public interface FetchConfigDataMapCallback {
        void onConfigDataMapFetched(DataMap config);
    }

    public static void putConfigDataItem(GoogleApiClient googleApiClient, DataMap newConfigData) {
        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(PATH_WITH_FEATURE);
        DataMap configurationToPut = putDataMapRequest.getDataMap();
        configurationToPut.putAll(newConfigData);
        Wearable.DataApi.putDataItem(googleApiClient, putDataMapRequest.asPutDataRequest())
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(@NonNull DataApi.DataItemResult dataItemResult) {

                    }
                });
    }
    public static void fetchConfigDataMap (final GoogleApiClient client,
                                           final FetchConfigDataMapCallback callback) {
        Wearable.NodeApi.getLocalNode(client)
                .setResultCallback(new ResultCallback<NodeApi.GetLocalNodeResult>() {
                    @Override
                    public void onResult(@NonNull NodeApi.GetLocalNodeResult getLocalNodeResult) {
                        String myLocalNode = getLocalNodeResult.getNode().getId();
                        Uri uri = new Uri.Builder()
                                .scheme("wear")
                                .path(MyWatchUtility.PATH_WITH_FEATURE)
                                .authority(myLocalNode)
                                .build();
                        Wearable.DataApi.getDataItem(client, uri)
                                .setResultCallback(new DataItemResultCallback(callback));
                    }
                });
    }
    public static void overwriteKeysInConfigDataMap (final GoogleApiClient googleApiClient,
                                                     final DataMap configKeysToOverwrite) {
        MyWatchUtility.fetchConfigDataMap(googleApiClient, new FetchConfigDataMapCallback() {
            @Override
            public void onConfigDataMapFetched(DataMap currentConfig) {
                DataMap overwriteConfig = new DataMap();
                overwriteConfig.putAll(currentConfig);
                overwriteConfig.putAll(configKeysToOverwrite);
                MyWatchUtility.putConfigDataItem(googleApiClient, overwriteConfig);
            }
        });
    }
    private static class DataItemResultCallback implements ResultCallback<DataApi.DataItemResult> {
        private final FetchConfigDataMapCallback mCallback;
        public DataItemResultCallback(FetchConfigDataMapCallback callback) {
            mCallback = callback;
        }

        @Override
        public void onResult(@NonNull DataApi.DataItemResult result) {
            if (result.getStatus().isSuccess()) {
                if (result.getDataItem() != null) {
                    DataItem configDataItem = result.getDataItem();
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(configDataItem);
                    DataMap config = dataMapItem.getDataMap();
                    mCallback.onConfigDataMapFetched(config);
                }else {
                    mCallback.onConfigDataMapFetched(new DataMap());
                }
            }
        }

    }
    private MyWatchUtility() {

    }
}
