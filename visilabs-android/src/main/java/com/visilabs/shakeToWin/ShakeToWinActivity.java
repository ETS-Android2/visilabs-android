package com.visilabs.shakeToWin;

import android.app.Activity;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.visilabs.android.R;
import com.visilabs.android.databinding.ActivityShakeToWinStep1Binding;
import com.visilabs.android.databinding.ActivityShakeToWinStep2Binding;
import com.visilabs.android.databinding.ActivityShakeToWinStep3Binding;

import java.util.Timer;
import java.util.TimerTask;

public class ShakeToWinActivity extends Activity implements SensorEventListener {

    private ActivityShakeToWinStep1Binding bindingStep1;
    private ActivityShakeToWinStep2Binding bindingStep2;
    private ActivityShakeToWinStep3Binding bindingStep3;
    private SensorManager mSensorManager;
    private float mAccelerometer;
    private float mAccelerometerCurrent;
    private float mAccelerometerLast;
    private Timer mTimerWithoutShaking;
    private Timer mTimerAfterShaking;
    private boolean isShaken = false;
    private boolean isStep3 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        bindingStep1 = ActivityShakeToWinStep1Binding.inflate(getLayoutInflater());
        View view = bindingStep1.getRoot();
        setContentView(view);

        cacheResources();

        //mShakeToWinMessage = getShakeToWinMessage();

        setupStep1View();
    }

    private void setupStep1View() {
        //TODO : replace this dummy data with the real one later
        //TODO : check and set the visibilities. Only the button is mandatory
        setupCloseButtonStep1();
        bindingStep1.container.setBackgroundColor(Color.parseColor("#ff99de"));
        Picasso.get().load("https://imgvisilabsnet.azureedge.net/in-app-message/uploaded_images/163_1100_490_20210319175823217.jpg")
                .into(bindingStep1.imageView);
        bindingStep1.titleView.setText("Title");
        bindingStep1.titleView.setTextColor(Color.parseColor("#92008c"));
        bindingStep1.titleView.setTextSize(32);
        bindingStep1.bodyTextView.setText("Text");
        bindingStep1.bodyTextView.setTextColor(Color.parseColor("#4060ff"));
        bindingStep1.bodyTextView.setTextSize(24);
        bindingStep1.buttonView.setText("Button");
        bindingStep1.buttonView.setBackgroundColor(Color.parseColor("#79e7ff"));
        bindingStep1.buttonView.setTextColor(Color.parseColor("#000000"));
        bindingStep1.buttonView.setTextSize(24);

        bindingStep1.buttonView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bindingStep2 = ActivityShakeToWinStep2Binding.inflate(getLayoutInflater());
                setContentView(bindingStep2.getRoot());
                setupStep2View();
            }
        });
    }

    private void setupStep2View() {
        bindingStep2.videoView.setVideoURI(Uri.parse(
                "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4"));
        bindingStep2.videoView.start();
        initAccelerometer();
    }

    private void setupCloseButtonStep1() {
        bindingStep1.closeButton.setBackgroundResource(getCloseIconStep1());
        bindingStep1.closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }

    private int getCloseIconStep1() {

        return R.drawable.ic_close_black_24dp;
        //TODO when real data comes:
       /* switch (mInAppMessage.getActionData().getCloseButtonColor()) {

            case "white":
                return R.drawable.ic_close_white_24dp;

            case "black":
                return R.drawable.ic_close_black_24dp;
        }
        return R.drawable.ic_close_black_24dp;*/
    }

    private void cacheResources() {
        //TODO : cache video in step 2 and picture in step 3 here
    }

    private void initAccelerometer() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mAccelerometer = 10f;
        mAccelerometerCurrent = SensorManager.GRAVITY_EARTH;
        mAccelerometerLast = SensorManager.GRAVITY_EARTH;
        mTimerWithoutShaking = new Timer("ShakeToWinTimerWithoutShaking", false);
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                if(!isShaken) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setupStep3View();
                        }
                    });
                }
            }
        };
        mTimerWithoutShaking.schedule(task, 5000);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if(!isStep3) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            mAccelerometerLast = mAccelerometerCurrent;
            mAccelerometerCurrent = (float) Math.sqrt((double) (x * x + y * y + z * z));
            float delta = mAccelerometerCurrent - mAccelerometerLast;
            mAccelerometer = mAccelerometer * 0.9f + delta;
            if (mAccelerometer > 12) {
                isShaken = true;
                mTimerAfterShaking = new Timer("ShakeToWinTimerAfterShaking", false);
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                setupStep3View();
                            }
                        });
                    }
                };
                mTimerAfterShaking.schedule(task, 0); //TODO: real data here
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setupStep3View() {
        if(mTimerWithoutShaking!=null) {
            mTimerWithoutShaking.cancel();
        }
        if(mTimerAfterShaking!=null) {
            mTimerAfterShaking.cancel();
        }
        isStep3 = true;
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER));
        bindingStep3 = ActivityShakeToWinStep3Binding.inflate(getLayoutInflater());
        setContentView(bindingStep3.getRoot());


    }
}
