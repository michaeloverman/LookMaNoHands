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

        Bitmap mRightFoot, mLeftFoot;
        Drawable mRightDrawable, mLeftDrawable;
        Paint mFeetPaint;
        Drawable mVectorBoot;

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
        boolean mShowSteps = true;
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

            Resources resources = MyWatchFaceService.this.getResources();
            mYOffset = resources.getDimension(R.dimen.digital_y_offset);

            mBackgroundPaint = new Paint();
            mBackgroundPaint.setColor(resources.getColor(R.color.background, null));

            mHoursPaint = new Paint();
            mHoursPaint = createTextPaint(resources.getColor(R.color.clemson_hour_text, null), HOUR_TYPEFACE);
            mHoursPaint.setLetterSpacing(-0.15f);

            mMinutesPaint = new Paint();
            mMinutesPaint = createTextPaint(resources.getColor(R.color.clemson_minute_text, null), MINUTE_TYPEFACE);

            mSecondsPaint = new Paint();
            mSecondsPaint.setColor(Color.WHITE);

            mTime = new GregorianCalendar();

            mStepsPaint = new Paint();
            mStepsPaint = createTextPaint(Color.WHITE, MINUTE_TYPEFACE);

            mStepsRequested = false;
            mGoogleApiClient = new GoogleApiClient.Builder(MyWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Fitness.HISTORY_API)
                    .addApi(Fitness.RECORDING_API)
                    .useDefaultAccount()
                    .build();

            mRightDrawable = resources.getDrawable(R.drawable.tiny_right_footprint_angle, null);
            mLeftDrawable = resources.getDrawable(R.drawable.tiny_left_footprint_angle, null);
            mRightFoot = ( (BitmapDrawable) mRightDrawable).getBitmap();
            mLeftFoot = ( (BitmapDrawable) mLeftDrawable).getBitmap();
            mVectorBoot = resources.getDrawable(R.drawable.vector_boot, null);
            mFeetPaint = new Paint();
            ColorFilter filter = new PorterDuffColorFilter(resources.getColor(R.color.clemson_hour_text, null), PorterDuff.Mode.SRC_IN);
            mFeetPaint.setColorFilter(filter);

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
                mShowSteps = false;
            }
        */
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
        //    mSensorManager.unregisterListener(mSensorEventListener);
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
            getTotalSteps();
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
            updateColors();
            invalidate();

            // Whether the timer should be running depends on whether we're visible (as well as
            // whether we're in ambient mode), so we may need to start or stop the timer.
            updateTimer();
        }

        @TargetApi(Build.VERSION_CODES.M)
        private void updateColors() {
            if (mAmbient) {
                mHoursPaint.setColor(getResources().getColor(R.color.ambient_hour, null));
                mHoursPaint.setStyle(Paint.Style.STROKE);
                mMinutesPaint.setColor(getResources().getColor(R.color.ambient_minute, null));
                mStepsPaint.setColor(getResources().getColor(R.color.ambient_hour, null));
            } else {
                mHoursPaint.setColor(getResources().getColor(R.color.clemson_hour_text, null));
                mHoursPaint.setStyle(Paint.Style.FILL);
                mMinutesPaint.setColor(getResources().getColor(R.color.clemson_minute_text, null));
                //mStepsPaint.setColor(Color.WHITE);
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
            int minuteInt = mTime.get(Calendar.MINUTE);
        //    String hour = String.format("%d", hourInt);
            String minute = String.format("%02d", minuteInt);
            String hour = hourInt + "";

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
            canvas.drawText(minute, minuteX, minuteY, mMinutesPaint);

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
                if(mShowSteps) {
                    getTotalSteps();
                    String stepsString = mStepCount + "";
                    canvas.drawText(stepsString, mCenterX - mStepsPaint.measureText(stepsString) * 0.5f,
                            mCenterY - mStepsPaint.getTextSize() * 0.5f, mStepsPaint);

                    canvas.drawBitmap(mRightFoot, 100f, 200f, mFeetPaint);
                    canvas.drawBitmap(mLeftFoot, 115f, 165f, mFeetPaint);
                    canvas.drawBitmap(mRightFoot, 150f, 150f, mFeetPaint);
                    canvas.drawBitmap(mLeftFoot, 165f, 115f, mFeetPaint);
                }
            }



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
            mStepsRequested = false;
            subscribeToSteps();
            getTotalSteps();
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
