package com.example.dell.sensor_data_send;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class PosRotSensors implements SensorEventListener
{
    public static final float PI = 3.14159265359f;
    public static final float RAD_TO_DEG = 180.0f / PI;
    public static String text;

    public PosRotSensors(Context context) {
        heliState = new HeliState();
        rotationMatrix = new float[9];
        yawPitchRollVec = new float[3];
        rotationVec = new float[3];

        yawZero = 0.0f;
        pitchZero = 0.0f;
        rollZero = 0.0f;

        // Get the sensors manager.
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        // Get the sensors.
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
    }

    public void resume()
    {
        sensorManager.registerListener(this, rotationSensor,
                SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void pause()
    {
        // Disable the inertial sensors.
        sensorManager.unregisterListener(this);
    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        // These sensors should not change precision.
    }

    @Override
    public final void onSensorChanged(SensorEvent event)
    {
        if(event.sensor == rotationSensor)
        {
            // Get the time and the rotation vector.
            heliState.time = event.timestamp;
            System.arraycopy(event.values, 0, rotationVec, 0, 3);

            // Convert the to "yaw, pitch, roll".
            SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVec);
            SensorManager.getOrientation(rotationMatrix, yawPitchRollVec);

            // Make the measurements relative to the user-defined zero orientation.
            heliState.yaw = getMainAngle(-(yawPitchRollVec[0]-yawZero) * RAD_TO_DEG);
            heliState.pitch = getMainAngle(-(yawPitchRollVec[1]-pitchZero) * RAD_TO_DEG);
            heliState.roll = getMainAngle((yawPitchRollVec[2]-rollZero) * RAD_TO_DEG);

            text = "roll: " + Float.toString(heliState.roll) + ",\npitch: " + Float.toString(heliState.pitch) +
                    ",\nyaw: " + Float.toString(heliState.yaw) ;

            // New sensors data are ready.
            newMeasurementsReady = true;
        }
    }

    public class HeliState
    {
        public float yaw, pitch, roll; // [degrees].
        public long time; // [nanoseconds].
    }

    public HeliState getState()
    {
        newMeasurementsReady = false;
        return heliState;
    }

    public void setCurrentStateAsZero()
    {
        yawZero = yawPitchRollVec[0];
        pitchZero = yawPitchRollVec[1];
        rollZero = yawPitchRollVec[2];
    }

    public boolean newMeasurementsReady()
    {
        return newMeasurementsReady;
    }

    public static float getMainAngle(float angle)
    {
        while(angle < -180.0f)
            angle += 360.0f;
        while(angle > 180.0f)
            angle -= 360.0f;

        return angle;
    }

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private HeliState heliState;
    private float[] rotationVec, rotationMatrix, yawPitchRollVec;
    private float yawZero, pitchZero, rollZero;
    private boolean newMeasurementsReady;
}