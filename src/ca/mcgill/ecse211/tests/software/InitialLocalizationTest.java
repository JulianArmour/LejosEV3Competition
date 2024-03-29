package ca.mcgill.ecse211.tests.software;

import ca.mcgill.ecse211.Main;
import ca.mcgill.ecse211.localizers.Localization;
import ca.mcgill.ecse211.navigators.MovementController;
import ca.mcgill.ecse211.odometer.Odometer;
import ca.mcgill.ecse211.odometer.OdometerExceptions;
import ca.mcgill.ecse211.sensors.LightDifferentialFilter;
import ca.mcgill.ecse211.sensors.MedianDistanceSensor;
import lejos.hardware.Button;
import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.hardware.sensor.SensorMode;

public class InitialLocalizationTest {

    public static final double             WHEEL_RAD     = Main.WHEEL_RAD;
    public static final double             TRACK_CW      = Main.TRACK_CW;
    public static final double             TRACK_CCW     = Main.TRACK_CCW;
    public static final int                SC            = 3;
    public static final float              TILE_LENGTH   = Main.TILE_SIZE;

    private static Port                    USPort;
    private static EV3UltrasonicSensor     ultrasonicSensor;
    private static SensorMode              distanceProvider;
    private static float[]                 USSample;
    private static Odometer                odometer;
    private static EV3LargeRegulatedMotor  leftMotor;
    private static EV3LargeRegulatedMotor  rightMotor;
    private static MovementController      movementController;
    private static MedianDistanceSensor    medianDistanceSensor;
    private static LocalEV3                localEV3;
    private static LightDifferentialFilter leftLightDiff;
    private static Localization            localizer;
    private static Port                    leftLSPort;
    private static Port                    rightLSPort;
    private static EV3ColorSensor          leftLightSensor;
    private static SensorMode              leftLSProvider;
    private static float[]                 leftLSSample;
    private static EV3ColorSensor          rightLightSensor;
    private static SensorMode              rightLSProvider;
    private static float[]                 rightLSSample;
    private static LightDifferentialFilter rightLightDiff;

    public static int                      Red_LL_x      = 0;
    public static int                      Red_LL_y      = 0;
    public static int                      Red_UR_x      = 4;
    public static int                      Red_UR_y      = 9;

    public static int[]                    startzone_LL  = { Red_LL_x, Red_LL_y };
    public static int[]                    startzone_UR  = { Red_UR_x, Red_UR_y };

    public static int                      Island_LL_x   = 6;
    public static int                      Island_LL_y   = 0;
    public static int                      Island_UR_x   = 15;
    public static int                      Island_UR_y   = 9;

    public static int[]                    ILL           = { Island_LL_x, Island_LL_y };
    public static int[]                    IUR           = { Island_UR_x, Island_UR_y };

    public static int                      TNR_LL_x      = 4;
    public static int                      TNR_LL_y      = 8;
    public static int                      TNR_UR_x      = 6;
    public static int                      TNR_UR_y      = 9;

    public static int[]                    TLL           = { TNR_LL_x, TNR_LL_y };
    public static int[]                    TUR           = { TNR_UR_x, TNR_UR_y };

    public static int                      TNG_LL_x;
    public static int                      TNG_LL_y;
    public static int                      TNG_UR_x;
    public static int                      TNG_UR_y;

    public static int                      SZR_LL_x      = 8;
    public static int                      SZR_LL_y      = 6;
    public static int                      SRZ_UR_x      = 10;
    public static int                      SRZ_UR_y      = 8;

    public static int[]                    searchzone_LL = { SZR_LL_x, SZR_LL_y };
    public static int[]                    searchzone_UR = { SRZ_UR_x, SRZ_UR_y };

    public static int                      SZG_LL_x;
    public static int                      SZG_LL_y;
    public static int                      SZG_UR_x;
    public static int                      SZG_UR_y;

    public static void main(String[] args) {
        // set up side ultrasonic sensor
        USPort = LocalEV3.get().getPort("S2");
        ultrasonicSensor = new EV3UltrasonicSensor(USPort);
        distanceProvider = ultrasonicSensor.getMode("Distance");
        USSample = new float[distanceProvider.sampleSize()];

        // set up left light sensor
        leftLSPort = LocalEV3.get().getPort("S3");
        leftLightSensor = new EV3ColorSensor(leftLSPort);
        leftLSProvider = leftLightSensor.getMode("Red");
        leftLSSample = new float[leftLSProvider.sampleSize()];

        // set up right light sensor
        rightLSPort = LocalEV3.get().getPort("S4");
        rightLightSensor = new EV3ColorSensor(rightLSPort);
        rightLSProvider = rightLightSensor.getMode("Red");
        rightLSSample = new float[rightLSProvider.sampleSize()];

        // set up wheel motors
        leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
        rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
        // starts odometer
        try {
            odometer = Odometer.getOdometer(leftMotor, rightMotor, TRACK_CW, WHEEL_RAD);
        } catch (OdometerExceptions e) {
            System.out.println("Could not get odometer.");
        }
        Thread odoThread = new Thread(odometer);
        odoThread.start();

        // initialize instances
        movementController = new MovementController(leftMotor, rightMotor, WHEEL_RAD, TRACK_CW, TRACK_CCW, odometer);
        medianDistanceSensor = new MedianDistanceSensor(distanceProvider, USSample, odometer, 5);
        leftLightDiff = new LightDifferentialFilter(leftLSProvider, leftLSSample);
        rightLightDiff = new LightDifferentialFilter(rightLSProvider, rightLSSample);
        localizer = new Localization(
                movementController, odometer, medianDistanceSensor, leftLightDiff, rightLightDiff, SC
        );

        localEV3 = (LocalEV3) LocalEV3.get();

        // start test
        localEV3.getTextLCD().clear();

        localizer.initialUSLocalization();
        localizer.initialLightLocalization();

        Sound.beep();
        Button.waitForAnyPress();
        System.exit(0);
    }
}
