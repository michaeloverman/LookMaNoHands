/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package tech.michaeloverman.android.mywatch;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.WindowInsets;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessStatusCodes;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DailyTotalResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
public class MyWatchFaceService extends CanvasWatchFaceService {
    private static final Typeface HOUR_TYPEFACE =
            Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
    private static final Typeface MINUTE_TYPEFACE = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);

    /**
     * Update rate in milliseconds for interactive mode. We update once a second since seconds are
     * displayed in interactive mode.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;
    private static final float FONTSIZE = 25f;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }


    private class Engine extends CanvasWatchFaceService.Engine implements
            GoogleApiClient.ConnectionCallbacks,
            GoogleApiClient.OnConnectionFailedListener,
            ResultCallback<DailyTotalResult> {
        GregorianCalendar mTime;

        private SensorManager mSensorManager;
        private Sensor mSensor;
        private SensorEventListener mSensorEventListener;

        final Handler mUpdateTimeHandler = new EngineHandler(this);
        boolean mRegisteredTimeZoneReceiver = false;

        boolean burnInProtectionModeFlag;

        Paint mBackgroundPaint;
        Paint mHoursPaint;
        Paint mMinutesPaint;
        Paint mSecondsPaint;
        int mHourColor;
        int mMinuteColor;
        int mSecondColor;
        int mFootpathColor;
        int mBackgroundColor = Color.BLACK;

        Bitmap mRightFoot, mLeftFoot, mRightFootHorz, mLeftFootHorz;
        Drawable mRightDrawable, mLeftDrawable, mRightHorzDrawable, mLeftHorzDrawable;
        Paint mFootpathPaint;

        boolean mAmbient;

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
            //    mTime.clear(intent.getStringExtra("time-zone"));
            //    mTime.setToNow();
                mTime.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };

        float mXOffset;
        float mYOffset;
//        float mCenterX;
//        float mCenterY;
        float mCenterX;
        float mCenterY;
        float mHourRadius;
        float mMinuteRadius;
        float mSecondRadius;
        boolean mShowSeconds = false;
        boolean mShowFootpath = true;
        float mTextSize;

        /**
         * Whether the display supports fewer bits for each color in ambient mode. When true, we
         * disable anti-aliasing in ambient mode.
         */
        boolean mLowBitAmbient;
        private Paint mStepsPaint;

        private boolean mRegisteredReceiver;
        private GoogleApiClient mGoogleApiClient;
        private boolean mStepsRequested;
        private int mStepCount;
        private int mStepCountGoal = 7000;
        private int mStepsPerFoot;
        float[] mFootpathX = new float[] {   7f, 25f,  5f, 25f, 15f, 22f, 20f, 20f, 16f, 25f,  5f, 25f,  5f, 25f,  5f, 25f,  5f, 25f };
        float[] mFootpathY = new float[] {-250f,  5f, 25f,  5f, 25f,-15f, 15f,-15f, 25f,  5f, 25f,  5f, 25f,  5f, 25f,  5f, 25f,  5f };
        private boolean mShowDate;
        private boolean mShowStepCount;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(MyWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_VARIABLE)
                    .setPeekOpacityMode(WatchFaceStyle.PEEK_OPACITY_MODE_TRANSLUCENT)
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN)
                    .setHotwordIndicatorGravity(Gravity.CENTER_VERTICAL)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setViewProtectionMode(WatchFaceStyle.PROTECT_WHOLE_SCREEN)
                    .setShowSystemUiTime(false)
                    .build()
            );
            mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    .useDefaultAccount()
                    .build();

            Resources resources = MyWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(mBackgroundColor);

            mHoursPaint = new Paint();
            mHoursPaint = createTextPaint(mHourColor, HOUR_TYPEFACE);
            mHoursPaint.setLetterSpacing(-0.15f);

            mMinutesPaint = new Paint();
            mMinutesPaint = createTextPaint(mMinuteColor, MINUTE_TYPEFACE);

            mSecondsPaint = new Paint();
            mSecondsPaint.setColor(mSecondColor);

            mTime = new GregorianCalendar();

            mStepsPaint = new Paint();
            mStepsPaint = createTextPaint(mFootpathColor, MINUTE_TYPEFACE);

            mStepsRequested = false;

            mRightDrawable = resources.getDrawable(R.drawable.tiny_right_footprint_angle, null);
            mLeftDrawable = resources.getDrawable(R.drawable.tiny_left_footprint_angle, null);
            mRightHorzDrawable = resources.getDrawable(R.drawable.tiny_right_footprint_horz, null);
            mLeftHorzDrawable = resources.getDrawable(R.drawable.tiny_left_footprint_horz, null);
            mRightFoot = ( (BitmapDrawable) mRightDrawable).getBitmap();
            mLeftFoot = ( (BitmapDrawable) mLeftDrawable).getBitmap();
            mRightFootHorz = ( (BitmapDrawable) mRightHorzDrawable).getBitmap();
            mLeftFootHorz = ( (BitmapDrawable) mLeftHorzDrawable).getBitmap();
            mFootpathPaint = new Paint();
            ColorFilter filter = new PorterDuffColorFilter(mFootpathColor, PorterDuff.Mode.SRC_IN);
            mFootpathPaint.setColorFilter(filter);
            mStepsPerFoot = mStepCountGoal / mFootpathX.length;

        /*    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null) {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
                mSensorEventListener = new SensorEventListener() {
                    @Override
                    public void onSensorChanged(SensorEvent event) {
                        mStepCount = (int) event.values[0];
                    }

                    @Override
                    public void onAccuracyChanged(Sensor sensor, int accuracy) {

                    }
                };
                mSensorManager.registerListener(mSensorEventListener, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                mSensor = null;
                mShowFootpath = false;
            }
        */
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
        //    mSensorManager.unregisterListener(mSensorEventListener);
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                Wearable.DataApi.removeListener(mGoogleApiClient, onDataChangedListener);
                mGoogleApiClient.disconnect();
            }
            super.onDestroy();
        }

        private Paint createTextPaint(int textColor, Typeface type) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTypeface(type);
//            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setAntiAlias(true);
            return paint;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                // Update time zone in case it changed while we weren't visible.
            //    mTime.clear(TimeZone.getDefault().getID());
            //    mTime.setToNow();

                mTime.setTimeZone(TimeZone.getDefault());

            } else {
                unregisterReceiver();

                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    mGoogleApiClient.disconnect();
                }
            }

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            MyWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            MyWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onApplyWindowInsets(WindowInsets insets) {
            super.onApplyWindowInsets(insets);

            // Load resources that have alternate values for round watches.
            Resources resources = MyWatchFaceService.this.getResources();
            boolean isRound = insets.isRound();
            mXOffset = resources.getDimension(isRound
                    ? R.dimen.digital_x_offset_round : R.dimen.digital_x_offset);
            mTextSize = resources.getDimension(isRound
                    ? R.dimen.digital_text_size_round : R.dimen.digital_text_size);

            mHoursPaint.setTextSize(mTextSize);
            mMinutesPaint.setTextSize(mTextSize * 0.5f);
            mSecondsPaint.setTextSize(mTextSize * 0.3f);
            mStepsPaint.setTextSize(15f);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
            burnInProtectionModeFlag = properties.getBoolean(PROPERTY_BURN_IN_PROTECTION, false);


        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            //getTotalSteps();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
/*            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode;
                if (mLowBitAmbient) {
                    mHoursPaint.setAntiAlias(!inAmbientMode);
                    mMinutesPaint.setAntiAlias(!inAmbientMode);
                }
//                mHoursPaint.setColor(getResources().getColor(R.color.ambient_hour));
//                mMinutesPaint.setColor(getResources().getColor(R.color.ambient_minute));

            } //else {
               // mAmbient =  inAmbientMode;
               // mHoursPaint.setColor(getResources().getColor(R.color.hour_text));
              //  mMinutesPaint.setColor(getResources().getColor(R.color.minute_text));

          //  }*/

            mAmbient = inAmbientMode;
            updateColorsAndSteps();
            invalidate();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @TargetApi(Build.VERSION_CODES.M)
        private void updateColorsAndSteps() {
            if (isInAmbientMode()) {
                mHoursPaint.setColor(getResources().getColor(R.color.ambient_hour, null));
                mHoursPaint.setStyle(Paint.Style.STROKE);
                mMinutesPaint.setColor(getResources().getColor(R.color.ambient_minute, null));
                mStepsPaint.setColor(getResources().getColor(R.color.ambient_hour, null));

            } else {
                mHoursPaint.setColor(mHourColor);
                mHoursPaint.setStyle(Paint.Style.FILL);
                mMinutesPaint.setColor(mMinuteColor);
                getTotalSteps();
            }
        }
        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            // Draw the background.
            if (isInAmbientMode()) {
                canvas.drawColor(Color.BLACK);
//                canvas.drawLine(0.0f, 0.0f, 350f, 350f, mExperimentPaint);
            } else {
                canvas.drawRect(0, 0, bounds.width(), bounds.height(), mBackgroundPaint);
//                canvas.drawPoint(mCenterX, mCenterY, mExperimentPaint);
//                canvas.drawPoint(bounds.width() / 2, bounds.height() / 2, mExperimentPaint);
            }

            // Draw H:MM in ambient mode or H:MM:SS in interactive mode.
            mTime.setTimeInMillis(System.currentTimeMillis());


        /*    int localHour = mTime.get(Calendar.HOUR);
            if (localHour > 12) localHour -= 12;
            if (localHour == 0) localHour = 12;
        */
            int hourInt = mTime.get(Calendar.HOUR);
            if (hourInt == 0) hourInt = 12;
            String hour = String.format("%d", hourInt);
            int minuteInt = mTime.get(Calendar.MINUTE);
            String minute = String.format("%02d", minuteInt);

            float hourRot = (float) (hourInt * Math.PI * 2 / 12);
            float minuteRot = (float) (minuteInt * Math.PI * 2 / 60);
            float hourX = (float) ((mCenterX + ( Math.sin(hourRot) * mHourRadius))
                    - (0.5 * mHoursPaint.measureText(hour)));
            float hourY = (float) ((mCenterY + ( -Math.cos(hourRot) * mHourRadius))
                    + (0.4 * mHoursPaint.getTextSize()));
            float minuteX = (float) ((mCenterX + ( Math.sin(minuteRot) * mMinuteRadius))
                    - (0.5 * mMinutesPaint.measureText(minute)));
            float minuteY = (float) ((mCenterY + ( -Math.cos(minuteRot) * mMinuteRadius))
                    + (0.4 * mMinutesPaint.getTextSize()));

            canvas.drawText(hour, hourX, hourY, mHoursPaint);

            if (!isInAmbientMode()) {
                if(mShowSeconds) {
                    int secondsInt = mTime.get(Calendar.SECOND);
                    String second = String.format("%02d", secondsInt);
                    float secondRot = (float) (secondsInt * Math.PI * 2 / 60);
                    float secondX = (float) ((mCenterX + (Math.sin(secondRot) * mSecondRadius))
                            - (0.5 * mSecondsPaint.measureText(second)));
                    float secondY = (float) ((mCenterY + (-Math.cos(secondRot) * mSecondRadius))
                            + (0.4 * mSecondsPaint.getTextSize()));
                    canvas.drawText(second, secondX, secondY, mSecondsPaint);
                }
                if(mShowFootpath) {
                    //getTotalSteps();

                    Bitmap bm;
                    boolean whichFoot = true;
                    int i;
                    float x = 0f;
                    float y = 0f;

                    int numFeet = mStepCount / mStepsPerFoot;
                    if (numFeet > mFootpathX.length) numFeet = mFootpathX.length;
                //    canvas.drawText(mStepsPerFoot + " steps/foot", mCenterX, mCenterY + 55, mStepsPaint);
                //    canvas.drawText(numFeet + " total feet", mCenterX, mCenterY + 70, mStepsPaint);

                    for (i = 0; i < numFeet; i++) {
                        x += mFootpathX[i];
                        y -= mFootpathY[i];
                        if (i < 4 || i > 7) {
                            if (whichFoot) {
                                bm = mLeftFoot;
                            } else {
                                bm = mRightFoot;
                            }
                        } else {
                            if (whichFoot) {
                                bm = mLeftFootHorz;
                            } else {
                                bm = mRightFootHorz;
                            }
                        }
                        canvas.drawBitmap(bm, x, y, mFootpathPaint);

                        whichFoot = !whichFoot;
                    }


                }
                if (mShowStepCount) {
                    String stepsString = mStepCount + "";
                    canvas.drawText(stepsString, mCenterX - mStepsPaint.measureText(stepsString) * 0.5f,
                            mCenterY - mStepsPaint.getTextSize() * 0.5f, mStepsPaint);

                }
                if (mShowDate) {

                }
            }

            canvas.drawText(minute, minuteX, minuteY, mMinutesPaint);


        }

        /**
         * Starts the {@link #mUpdateTimeHandler} timer if it should be running and isn't currently
         * or stops it if it shouldn't be running but currently is.
         */
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        /**
         * Returns whether the {@link #mUpdateTimeHandler} timer should be running. The timer should
         * only run when we're visible and in interactive mode.
         */
        private boolean shouldTimerBeRunning() {
            return isVisible() && !isInAmbientMode();
        }

        /**
         * Handle updating the time periodically in interactive mode.
         */
        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }

        private void getTotalSteps() {
            if ((mGoogleApiClient != null)
                    && (mGoogleApiClient.isConnected())
                    && (!mStepsRequested)) {
                mStepsRequested = true;
                PendingResult<DailyTotalResult> stepsResult =
                        Fitness.HistoryApi.readDailyTotal(
                                mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA);
                stepsResult.setResultCallback(this);

            }
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            mCenterX = width / 2f;
            mCenterY = height / 2f;
            mHourRadius = (float) (mCenterX * 0.65);
            mMinuteRadius = (float) (mCenterX * 0.60);
            mSecondRadius = (float) (mCenterX * 0.50);
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, onDataChangedListener);
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
            mStepsRequested = false;
            subscribeToSteps();
            getTotalSteps();
        }

        private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener() {

            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent event : dataEventBuffer) {
                    if (event.getType() == DataEvent.TYPE_CHANGED) {
                        DataItem item = event.getDataItem();
                        updateParamsForDataItem(item);
                    }
                }

                dataEventBuffer.release();
                if (isVisible() && !isInAmbientMode()) {
                    invalidate();
                }
            }
        };

        private final ResultCallback<DataItemBuffer> onConnectedResultCallback =
                new ResultCallback<DataItemBuffer>() {
                    @Override
                    public void onResult(@NonNull DataItemBuffer dataItems) {
                        for (DataItem item : dataItems) {
                            updateParamsForDataItem(item);
                        }

                        dataItems.release();
                        if (isVisible() && !isInAmbientMode()) {
                            invalidate();
                        }
                    }
                };

        private void updateParamsForDataItem(DataItem item) {
            if ((item.getUri().getPath()).equals("/watch_face_config_nohands")) {

                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (dataMap.containsKey("hour_color")) {
                    int c = dataMap.getInt("hour_color");
                    mHoursPaint.setColor(c);
                }
                if (dataMap.containsKey("minute_color")) {
                    int c = dataMap.getInt("minute_color");
                    mMinutesPaint.setColor(c);
                }
                if (dataMap.containsKey("seconds_color")) {
                    int c = dataMap.getInt("seconds_color");
                    mSecondsPaint.setColor(c);
                }
                if (dataMap.containsKey("footpath_color")) {
                    int c = dataMap.getInt("footpath_color");
                    mFootpathPaint.setColor(c);
                }
                if (dataMap.containsKey("show_seconds")) {
                    boolean b = dataMap.getBoolean("show_seconds");
                    mShowSeconds = b;
                }
                if (dataMap.containsKey("show_stepcount")) {
                    boolean b = dataMap.getBoolean("show_stepcount");
                    mShowStepCount = b;
                }
                if (dataMap.containsKey("show_footpath")) {
                    boolean b = dataMap.getBoolean("show_footpath");
                    mShowFootpath = b;
                }
                if (dataMap.containsKey("show_date")) {
                    boolean b = dataMap.getBoolean("show_date");
                    mShowDate = b;
                }
                invalidate();
            }
        }

        private void subscribeToSteps() {
            Fitness.RecordingApi.subscribe(mGoogleApiClient, DataType.TYPE_STEP_COUNT_DELTA)
                    .setResultCallback(new ResultCallback<Status>() {
                        @Override
                        public void onResult(@NonNull Status status) {
                            if (status.isSuccess()) {
                                if (status.getStatusCode() == FitnessStatusCodes.SUCCESS_ALREADY_SUBSCRIBED) {

                                } else {

                                }
                            } else {

                            }
                        }

                    });
        }
        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }

        @Override
        public void onResult(@NonNull DailyTotalResult dailyTotalResult) {
            mStepsRequested = false;

            if (dailyTotalResult.getStatus().isSuccess()) {
                List<DataPoint> points = dailyTotalResult.getTotal().getDataPoints();

                if (!points.isEmpty()) {
                    mStepCount = points.get(0).getValue(Field.FIELD_STEPS).asInt();
                }
            } else {

            }
        }
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<MyWatchFaceService.Engine> mWeakReference;

        public EngineHandler(MyWatchFaceService.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            MyWatchFaceService.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
