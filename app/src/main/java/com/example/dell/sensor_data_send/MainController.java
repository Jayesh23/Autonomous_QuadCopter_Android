package com.example.dell.sensor_data_send;

import com.example.dell.sensor_data_send.PosRotSensors.HeliState;
import android.os.SystemClock;

public class MainController
{
    private static final double MAX_MOTOR_POWER = 250.0; // 255.0 normally, less for testing.
    private static final float MAX_SAFE_PITCH_ROLL = 60; // [deg].
    private static final float PID_DERIV_SMOOTHING = 0.5f;

    public MainController(MainActivity activity) {
        this.activity = activity;

        motorsPowers = new MotorsPowers();

        yawRegulator = new PidAngleRegulator(0.0f, 0.0f, 0.0f, PID_DERIV_SMOOTHING);
        pitchRegulator = new PidAngleRegulator(0.6f, 0.0f, 0.15f, PID_DERIV_SMOOTHING);
        rollRegulator = new PidAngleRegulator(0.6f, 0.0f, 0.15f, PID_DERIV_SMOOTHING);

        yawAngleTarget = 0.0f;
        pitchAngleTarget = 0.0f;
        rollAngleTarget = 0.0f;

        // Create the sensors manager.
        posRotSensors = new PosRotSensors(activity);
        heliState = posRotSensors.new HeliState();
    }

    public void start() throws Exception {
        // Initializations.
        regulatorEnabled = true;
        meanThrust = 150.0f;

        // Start the sensors.
        posRotSensors.resume();

        // Start the main controller thread.
        controllerThread = new ControllerThread();
        controllerThread.start();
    }

    public void stop() {
        // Stop the main controller thread.
        controllerThread.requestStop();

        // Stop the sensors.
        posRotSensors.pause();
    }

    public PosRotSensors.HeliState getSensorsData() {
        return heliState;
    }

    public MotorsPowers getMotorsPowers() {
        return motorsPowers;
    }

    public boolean getRegulatorsState() {
        return regulatorEnabled;
    }

    public static String values;

    public class ControllerThread extends Thread {
        @Override
        public void run() {
            again = true;

            while (again) {
                // This loop runs at a very high frequency (1kHz), but all the
                // control only occurs when new measurements arrive.
                if (!posRotSensors.newMeasurementsReady()) {
                    SystemClock.sleep(1);
                    continue;
                }

                // Get the sensors data.
                heliState = posRotSensors.getState();

                float currentYaw = heliState.yaw;
                float currentPitch = heliState.pitch;
                float currentRoll = heliState.roll;

                long currentTime = heliState.time;
                float dt = ((float) (currentTime - previousTime)) / 1000000000.0f; // [s].
                previousTime = currentTime;

                if (Math.abs(dt) > 1.0) // In case of the counter has wrapped around.
                    continue;

                // Check for dangerous situations.
                if (regulatorEnabled) {
                    // If the quadcopter is too inclined, emergency stop it.
                    if (Math.abs(currentPitch) > MAX_SAFE_PITCH_ROLL ||
                            Math.abs(currentRoll) > MAX_SAFE_PITCH_ROLL) {
                        emergencyStop();
                    }
                }

                // Compute the motors powers.
                float yawForce, pitchForce, rollForce, altitudeForce;

                if (regulatorEnabled && meanThrust > 1.0) {
                    // Compute the "forces" needed to move the quadcopter to the
                    // set point.
                    yawForce = yawRegulator.getInput(yawAngleTarget, currentYaw, dt);
                    pitchForce = pitchRegulator.getInput(pitchAngleTarget, currentPitch, dt);
                    rollForce = rollRegulator.getInput(rollAngleTarget, currentRoll, dt);
                    altitudeForce = meanThrust;

                    // Compute the power of each motor.
                    double tempPowerNW, tempPowerNE, tempPowerSE, tempPowerSW;

                    tempPowerNW = altitudeForce; // Vertical "force".
                    tempPowerNE = altitudeForce; //
                    tempPowerSE = altitudeForce; //
                    tempPowerSW = altitudeForce; //

                    tempPowerNW += pitchForce; // Pitch "force".
                    tempPowerNE += pitchForce; //
                    tempPowerSE -= pitchForce; //
                    tempPowerSW -= pitchForce; //

                    tempPowerNW += rollForce; // Roll "force".
                    tempPowerNE -= rollForce; //
                    tempPowerSE -= rollForce; //
                    tempPowerSW += rollForce; //

                    tempPowerNW += yawForce; // Yaw "force".
                    tempPowerNE -= yawForce; //
                    tempPowerSE += yawForce; //
                    tempPowerSW -= yawForce; //

                    // Saturate the values, because the motors input are 0-255.
                    motorsPowers.nw = motorSaturation(tempPowerNW);
                    motorsPowers.ne = motorSaturation(tempPowerNE);
                    motorsPowers.se = motorSaturation(tempPowerSE);
                    motorsPowers.sw = motorSaturation(tempPowerSW);
                } else {
                    motorsPowers.nw = 0;
                    motorsPowers.ne = 0;
                    motorsPowers.se = 0;
                    motorsPowers.sw = 0;
                    yawForce = 0.0f;
                    pitchForce = 0.0f;
                    rollForce = 0.0f;
                    altitudeForce = 0.0f;
                }

                //transfer to arduino the motorPowers
                //transmitter.setPowers(motorsPowers);
                //Log.d("sensor", Float.toString(motorsPowers.nw) + ", " + Float.toString(motorsPowers.ne) +
                //      ", " + Float.toString(motorsPowers.se) + ", " + Float.toString(motorsPowers.sw) + "\n");
                values = (">" + Float.toString(motorsPowers.nw) + ", " + Float.toString(motorsPowers.ne) +
                                ", " + Float.toString(motorsPowers.se) + ", " + Float.toString(motorsPowers.sw) + "\n");
            }
        }

        public void requestStop() {
            again = false;
        }

        private boolean again;
    }

    private int motorSaturation(double val) {
        if (val > MAX_MOTOR_POWER)
            return (int) MAX_MOTOR_POWER;
        else if (val < 0.0)
            return 0;
        else
            return (int) val;
    }

    private void emergencyStop() {
        // TODO
        // The motors should stop gradually, to avoid hitting the ground too hard ?
        regulatorEnabled = false;
    }

    public class MotorsPowers {
        public int nw, ne, se, sw; // 0-1023 (10 bits values).

        public int getMean() {
            return (nw + ne + se + sw) / 4;
        }
    }

    private MainActivity activity;
    private float meanThrust, yawAngleTarget, pitchAngleTarget, rollAngleTarget;
    public PosRotSensors posRotSensors;
    private MotorsPowers motorsPowers;
    private boolean regulatorEnabled;
    public PidAngleRegulator yawRegulator, pitchRegulator, rollRegulator;
    private PosRotSensors.HeliState heliState;
    private ControllerThread controllerThread;
    private long previousTime;
}



