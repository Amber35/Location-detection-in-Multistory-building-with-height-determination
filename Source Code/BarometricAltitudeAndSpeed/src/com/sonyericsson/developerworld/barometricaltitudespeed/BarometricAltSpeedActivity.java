/*
 * Copyright 2011 Sony Ericsson Mobile Communications AB
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * This is the main activity class where most of the logic resides.
 * 
 */
package com.sonyericsson.developerworld.barometricaltitudespeed;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BarometricAltSpeedActivity extends Activity {
    /** Called when the activity is first created. */
    private WakeLock wake_lock; // A wake lock is used to lock the screen as
    // bright when the activity is in forgorund.

    private static float referencePressure; // Cache store of the reference
    // pressuere. Performance reasons.
    private Button setReference;
    private Button resetReference;
    private TextView altitudeDisplay, speedDisplay;
    private boolean isInForeground;
    private PressuredataObject pdoPrevious = null;

    private SensorManager sensorManager;
    private Sensor pressureSensor;

    /**
     * Geters and seters to detect if the activity is in foreground or not.
     */
    private synchronized boolean isInForeground() {
        return isInForeground;
    }

    private synchronized void setInForeground(boolean isInForeground) {
        this.isInForeground = isInForeground;
    }

    /**
     * Set up all the necesary infrastructure for the application.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        setReference = (Button) findViewById(R.id.setrefbutton);
        setReference.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (pdoPrevious != null) {
                    referencePressure = pdoPrevious.getAirPressure();
                    PressureUtilities.insertReferencePointToDB(
                            getApplicationContext(), referencePressure);

                }
            }
        });
        resetReference = (Button) findViewById(R.id.resetrefbutton);
        resetReference.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                referencePressure = PressureUtilities.DEFAULT_REFERENCE_PRESSURE;
                PressureUtilities.insertReferencePointToDB(
                        getApplicationContext(), referencePressure);

            }
        });
        altitudeDisplay = (TextView) findViewById(R.id.altitudeView);
        speedDisplay = (TextView) findViewById(R.id.speedView);

    }

    /**
     * On Pause bumps in when the application is no longer in foreground. Remove
     * the wakelock.
     */
    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        setInForeground(false);
        wake_lock.release();
    }

    /**
     * When onResume are recived, enable wake_lock and get hold of a
     * SensorManager. Note that the activity is in foreground. Start the
     * repetitive scanning of Sensors.
     */
    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wake_lock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "AirspeedData");
        wake_lock.acquire();
        referencePressure = PressureUtilities
                .getReferencePoint(getApplicationContext());
        if (referencePressure == -1) {
            referencePressure = PressureUtilities.DEFAULT_REFERENCE_PRESSURE;
        }
        collectDataHandler.sendEmptyMessageDelayed(0,
                PressureUtilities.INTERVAL_MS);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        setInForeground(true);
    }

    /**
     * It is a handler task to collect data from the sensor in order to make the
     * UI thread clean.
     * 
     */
    private Handler collectDataHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            DataCollector dc = new DataCollector();
            dc.collectData();
        }

    };

    /**
     * This is a pure helper class to do the collecting of data from sensor,
     * initiates the logging and stores whatever needed data.
     * 
     */
    private class DataCollector implements SensorEventListener, Runnable {
        private static final String LOG_TAG = "DataCollectorthread";
        private Thread thr;
        private ArrayList<PressuredataObject> pressureDataList = null;
        private int numReads;

        private DataCollector() {
            thr = new Thread(this);
        }

        public void collectData() {
            thr.start();
        }

        @Override
        public void run() {
            // TODO Auto-generated method stub

            // Register as listener to sensor framework
            sensorManager.registerListener(this, pressureSensor,
                    SensorManager.SENSOR_DELAY_UI);

            pressureDataList = new ArrayList<PressuredataObject>();

            numReads = 0;
            // Wait until list is full.
            while (numReads < PressureUtilities.MAXLENGTHOFLIST) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
            // Select median pressure value in order to compensate for eventual
            // peaks.
            PressuredataObject pdo = PressureUtilities
                    .selectMedianValue(pressureDataList);
            // Calculate speed and altitude
            float speed = 0f;
            float alt = 0f;
            if (pdoPrevious == null) {
                pdo.setSpeed(0);
            } else {
                speed = PressureUtilities.calculateSpeed(pdo, pdoPrevious);
            }
            alt = PressureUtilities.calculateAltitude(pdo, referencePressure);
            final float toWriteSpeed = speed;
            final float toWriteAlt = alt;
            // Write speed and altitude to UI.
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // TODO Auto-generated method stub
                    speedDisplay.setText(toWriteSpeed + " m/s");
                    altitudeDisplay.setText(toWriteAlt + " m");

                }
            });
            // Unregister from sensor framework
            sensorManager.unregisterListener(this);
            pdoPrevious = pdo;
            if (isInForeground()) {
                collectDataHandler.sendEmptyMessageDelayed(0,
                        PressureUtilities.INTERVAL_MS);
            }

        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // TODO Auto-generated method stub
            if (sensor.equals(pressureSensor)) {
                Log.d(LOG_TAG, "Accuracy changed on Pressure Sensor");
            } else {
                Log.d(LOG_TAG,
                        "Accuracy changed on Other Sensor, odd beahaviour");
            }

        }

        /**
         * This callback stores the current sensor value to array of values for
         * further procesing
         */
        @Override
        public void onSensorChanged(SensorEvent sensEvent) {
            // TODO Auto-generated method stub
            float[] values = sensEvent.values;
            PressureUtilities.insertPressureObjectToList(
                    new PressuredataObject(values[0], 0f, System
                            .currentTimeMillis()), pressureDataList);
            numReads++;
        }
    }

}