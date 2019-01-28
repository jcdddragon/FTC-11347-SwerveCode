//11347 has all the cool kids
// ***********************************************************************
// SwerveAuto
// ***********************************************************************
// The autonomous mode for swerve operations


//
// ****** IMPORTANT NOTE FOR STATE CHANGES ******
// The state machine used here has the ability to automatically add a wait
// before the next state starts operation. This is very useful for robot
// operations because the robot sometimes needs a delay before it starts
// any status checks in the following state. It allows the robot to start
// and action and wait before taking the next action.
//


// TODO - Add new log file just for autonomous state results? Or flag at END of each state...

package org.firstinspires.ftc.teamcode;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.teamcode.vision.MasterVision;
import org.firstinspires.ftc.teamcode.vision.SampleRandomizedPositions;

// ***********************************************************************
// Definitions from Qualcomm code for OpMode recognition
// ***********************************************************************
// **** DO NOT ENABLE - Started from the Silver and Gold auto code derived classes now ****
// ****   @Autonomous(name="Swerve: 1-Auto 0.6", group="Swerve")
// ***********************************************************************
// 11/9/2018 - Swerve drive is now capable of auto correcting orientation to a degree while driving. The wheels actively adjust while moving
public class SwerveAuto extends SwerveCore {

    //private static Boolean useLightFlicker = Boolean.TRUE;

    // State machine for where we are on the autonomous journey

    // NEW states and initial working code done at GRITS meeting 10/13/2018

    enum autoStates {
        SWERVE_INIT,
        SWERVE_START,
        SWERVE_ALIGN,
        SWERVE_DROP,
        SWERVE_DELAY,
        SWERVE_SLIDE,
        SWERVE_PULL_BACK,
        SWERVE_CENTER,
        SWERVE_TURN_TO_PARTICLES,
        SWERVE_HIT_PARTICLE,
        SWERVE_PULL_FORWARD,
        SWERVE_TO_WALL,
        SWERVE_TO_DEPOT,
        SWERVE_PLACE_MARKER,
        SWERVE_TO_CRATER,
        SWERVE_CRATER_PARTICLES,
        SWERVE_PARTICLE_TO_LANDER,
        SWERVE_SCORE_BALLS,
        SWERVE_ARM_TO_CRATER,
        SWERVE_LAST_MOVE,
        SWERVE_DONE,

        SWERVE_TEST_TURN_ROBOT,
        SWERVE_TEST_MOVE_ROBOT
    }

    private autoStates revTankState;
    private double stateStartTime;
    private double stateWaitTime;
    // report of our time wait
    private String checkReport;
    private String loopSenseStatus;
    private Boolean autoDriveWait;
    private Boolean autoDriveStop;
    public Boolean start90;
    // debug options to run a few states
    // -- enabled/controlled in SwerveAutoTEST init/start
    boolean debugActive = Boolean.FALSE;
    long debugStates = 1;
    autoStates debugStartState = autoStates.SWERVE_DONE;


    // Sensor data for robot positioning
    public boolean skipDrop;
    private float robotTurn[];
    private float robotMove[];
    private float lastSenseTime;

    // Robot orientation data
    private float robotOrientation[];
    private float robotSpeed[];
    private float robotPosition[];
    private int delaycount;
    // for Vuforia detection
    MasterVision vision;
    SampleRandomizedPositions goldPosition;
    Boolean right = Boolean.FALSE;
    Boolean center = Boolean.FALSE;
    Boolean left = Boolean.FALSE;

    // variables for auto actions
    private int moveTimePushoff;


    // ***********************************************************************
    // SwerveAuto
    // ***********************************************************************
    // Constructs the class.
    // The system calls this member when the class is instantiated.
    public SwerveAuto() {
        // Initialize base classes.
        // All via self-construction.

        // Initialize class members.
        // All via self-construction.
    }


    // ***********************************************************************
    // getStateName
    // ***********************************************************************
    // Return the name of the current state
    public String getStateName(autoStates myState) {
        // Set the name for the state we are in
        switch (myState) {

            case SWERVE_INIT:
                return "INITIALIZING";
            case SWERVE_START:
                return "START";
            case SWERVE_ALIGN:
                return "ALIGN";
            case SWERVE_DROP:
                return "DROP";
            case SWERVE_DELAY:
                return "DELAY";
            case SWERVE_SLIDE:
                return "SLIDE";
            case SWERVE_PULL_BACK:
                return "PULL BACK";
            case SWERVE_CENTER:
                return "CENTER";
            case SWERVE_TURN_TO_PARTICLES:
                return "TURN TO PARTICLES";
            case SWERVE_TO_WALL:
                return "TO WALL";
            case SWERVE_TO_DEPOT:
                return "TO DEPOT";
            case SWERVE_PLACE_MARKER:
                return "PLACE MARKER";
            case SWERVE_TO_CRATER:
                return "TO CRATER";
            case SWERVE_CRATER_PARTICLES:
                return "CRATER PARTICLES";
            case SWERVE_PARTICLE_TO_LANDER:
                return "PARTICLE TO LANDER";
            case SWERVE_SCORE_BALLS:
                return "SCORE BALLS";
            case SWERVE_ARM_TO_CRATER:
                return "ARM TO CRATER";
            case SWERVE_LAST_MOVE:
                return "LAST MOVE";
            case SWERVE_DONE:
                return "DONE";

//          Testing cases
            case SWERVE_TEST_TURN_ROBOT:
                return "TEST - turn robot";
            case SWERVE_TEST_MOVE_ROBOT:
                return "TEST - move robot";
            default:
                return "UNKNOWN! ID = " + revTankState;
        }
    }


    // ***********************************************************************
    // getCurStateName
    // ***********************************************************************
    // Return the name of the current state
    public String getCurStateName() {
        return getStateName(revTankState);
    }


    // ***********************************************************************
    // Init
    // ***********************************************************************
    // Performs any actions that are necessary when the OpMode is enabled.
    // The system calls this member once when the OpMode is enabled.
    @Override
    public void init() {


        double initWheelAngle;
        double initWheelPower;

        swerveDebug(500, "SwerveAuto::init", "START");
        initWheelAngle = .3;
        initWheelPower = 0.02;
        // Run initialization of other parts of the class
        // Note that the class will connect to all of our motors and servos

        super.init();

        swerveDebug(500, "SwerveAuto::init", "Back from super.init");

        //Tensor Flow Initialization
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();
//        sets which camera to use
        parameters.cameraDirection = VuforiaLocalizer.CameraDirection.BACK;
        parameters.vuforiaLicenseKey = "AXALhZf/////AAABmeL06CuSFUvSihBEZtVB9MllwYAol1njgG9CAEcNIyohat03TdAACXdYBpbS6M0BCHZAnGChIMBGm0BP2MHKV7IHPsfti2ZwLEf0bZgd/oNwpq+h/YnIhrm4qARe/3sKUsJZo4tlHK+FkFU10vWg0uBHqgfSf1zW/lJbyVhh+h4u8/3y6B6tXG+3yb9zQZECGgJyqifA5sQNyqCP/Wy0O1AY9hgCnbCHeOMChhpaKiGpXM4PNPsDbKo59yEb6QSF8KNciYUQmR7vviirGKFj4TetMNHrgKVPYCQGzmWdKvmCB5sikQ6lelNGHU9Je6sKMScefU0s8Vn5WyToDfddPoNejyrmLkq9jH3ccZ/7Q+gA";

//        infer tells tensor flow which side  it doesnt have in relation to the robot
        if(targetSilver) {
            vision = new MasterVision(parameters, hardwareMap, false, MasterVision.TFLiteAlgorithm.INFER_RIGHT);
        }
        else{
            vision = new MasterVision(parameters, hardwareMap, false, MasterVision.TFLiteAlgorithm.INFER_LEFT);
        }
        vision.init();// enables the camera overlay
        vision.enable();// enables the tracking algorithms


        swerveDebug(500, "SwerveAuto::init", "TensorFlow Ready");


        // orient to the field now and save our angle for use in teleOp
        ourSwerve.setFieldOrientation();

        // set initial pushoff delay
        moveTimePushoff = 400;
        autoDriveWait = Boolean.FALSE;
        // force the marker drop servo to hold tight
        gameMarkDrop.setPosition(0);
        flapL.setPosition(0);
        flapR.setPosition(0);

        // cause all the wheels to turn to the initialization position - 45 degrees
        swerveLeftFront.updateWheel(initWheelPower, -initWheelAngle);
        swerveRightFront.updateWheel(initWheelPower, initWheelAngle);
        swerveLeftRear.updateWheel(initWheelPower, initWheelAngle);
        swerveRightRear.updateWheel(initWheelPower, -initWheelAngle);

        // wait for the wheels to turn
        swerveSleep(500);

        // stop power to the wheels - servos stay locked
        swerveLeftFront.updateWheel(0, -initWheelAngle);
        swerveRightFront.updateWheel(0, initWheelAngle);
        swerveLeftRear.updateWheel(0, initWheelAngle);
        swerveRightRear.updateWheel(0, -initWheelAngle);

        swerveDebug(500, "SwerveAuto::init", "Swerve wheels in init positions");

        // Robot and autonomous settings are read in from files in the core class init()
        // Report the autonomous settings
        // ***** now done in silver & gold *****
        // ***** showAutonomousGoals();

        swerveDebug(500, "SwerveAuto::init", "DONE");
    }


    // ***********************************************************************
    // start
    // ***********************************************************************
    // Do first actions when the start command is given.
    // Called once when the OpMode is started.
    @Override
    public void start() {
        swerveDebug(500, "SwerveAuto::start", "START");

        // Call the super/base class start method.
        super.start();


        // nothing sensed yet
        loopSenseStatus = "No sensing yet";

        // Start in the initial robot state
        revTankState = autoStates.SWERVE_INIT;
        setState(autoStates.SWERVE_START, 0);

        swerveDebug(500, "SwerveAuto::start", "DONE");
    }


    // ***********************************************************************
    // loop
    // ***********************************************************************
    // State machine for autonomous robot control
    // Called continuously while OpMode is running
    //
    // Note that since this is autonomous and we do not care about reading
    // joysticks.
    @Override
    public void loop() {

//        turn off tensorFlow

        vision.disable();
        goldPosition = vision.getTfLite().getLastKnownSampleOrder();

        // Normal logging of loop start

        swerveDebug(500, "SwerveAuto::loop", "START, state is " +
                getCurStateName() + "'");

//        display the target

        swerveDebug(50, "SwerveAuto::loop", "Sensing status: " +
                goldPosition + "'");

        // check for auto drive

        if ( autoDriveWait ) {
            if ( ourSwerve.autoDriveCheck( autoDriveStop, targetSilver )) {
                autoDriveWait = Boolean.FALSE;

                // auto finished in time, so no more waiting
                stateWaitTime = 0.0;
            }
        }

        // if we are waiting, move on

        if (!checkStateReady()) {
            swerveDebug(500, "SwerveAuto::loop", "Waiting for steady state (" +
                    checkReport + ")");

            loopEndReporting();
            return;
        }

        // Move based on the current state

        switch (revTankState) {

//            SHOULD NEVER HAPPEN - INIT while we are running....
            case SWERVE_INIT:
                // INIT is only used to have some state before we set START in start()
                swerveDebug(1, "SwerveAuto::loop *ERROR*", "== loop with INIT state");
                setState(autoStates.SWERVE_START, 0);
                delaycount = 0;
                break;

//            First state
            case SWERVE_START:
                // jump to debug if active
                if (debugActive) {
                    setState(debugStartState, 0);
                } else {


                    // start the drop


                    setState(autoStates.SWERVE_ALIGN, 0);
                }
                break;

//            align the wheels before the drop
            case SWERVE_ALIGN:
                ourSwerve.autoDrive( 0.2, 85, 0.0, .01, targetSilver );
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;
                setState(autoStates.SWERVE_DROP, 250);
                break;
//            Drop down from the lander
            case SWERVE_DROP:
                if(noLift=true){
                    extension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    extension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    extension.setTargetPosition(-1000);

                    wrist.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    wrist.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    wrist.setTargetPosition(-1800);
                    // extend wrist
                    wrist.setPower(-1);
                    swerveSleep(2000);
                    extension.setPower(-.6);

                    setState(autoStates.SWERVE_DELAY, 3300);
                }

                else {
                    extension.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    extension.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    extension.setTargetPosition(-1000);

                    wrist.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
                    wrist.setMode(DcMotor.RunMode.RUN_TO_POSITION);
                    wrist.setTargetPosition(-1800);
                    // extend wrist
                    climber.setPower(.85);
                    wrist.setPower(-1);
                    swerveSleep(2000);
                    extension.setPower(-.6);

                    setState(autoStates.SWERVE_DELAY, 3300);
                }
                break;

//            delays in order to give the lit time to finish
            case SWERVE_DELAY:
                climber.setPower(0);

//                                customizable delay
                swerveSleep(0);
                setState(autoStates.SWERVE_SLIDE, 10);
                break;

//            slide off of the lander
            case SWERVE_SLIDE:
                ourSwerve.autoDrive(0.6, 85, 0, 6, targetSilver);
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;

//                 turn for the planned time
                setState(autoStates.SWERVE_CENTER, 5000);
                break;

//            Move to the particles
            case SWERVE_CENTER:
                ourSwerve.autoDrive(1.0, 295.0, 0.0, 25.0, targetSilver);
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;
                setState(autoStates.SWERVE_PULL_BACK, 2500);
                break;

//            pull back against lander
            case SWERVE_PULL_BACK:
                ourSwerve.autoDrive(0.4, 180.0, 0.0, 5.0, targetSilver);
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;
                swerveSleep(750);
                wrist.setTargetPosition(-2150);
                wrist.setPower(-1);
                setState(autoStates.SWERVE_TURN_TO_PARTICLES, 2000);
                break;

            case SWERVE_TURN_TO_PARTICLES:
                if (goldPosition == SampleRandomizedPositions.RIGHT){
                    orientRobot(20);
                    setState(autoStates.SWERVE_HIT_PARTICLE, 1000);
                }
                else if (goldPosition == SampleRandomizedPositions.LEFT){
                    extension.setTargetPosition(-1100);
                    extension.setPower(-1);
                    orientRobot(-20);
                    setState(autoStates.SWERVE_HIT_PARTICLE, 1000);
                }
                else{
                    setState(autoStates.SWERVE_HIT_PARTICLE, 0);
                }
                break;

//            turn towards the particle
            case SWERVE_HIT_PARTICLE:
                wrist.setTargetPosition(-2365);
                wrist.setPower(-1);
                if (goldPosition == SampleRandomizedPositions.LEFT) {
                    right = Boolean.TRUE;
                    orientRobot(-30);
                    if(!targetSilver) {
                        setState(autoStates.SWERVE_PULL_FORWARD, 1600);
                    }
                    else{
                        setState(autoStates.SWERVE_TO_WALL, 1600);
                    }                }
                if (goldPosition == SampleRandomizedPositions.CENTER) {
                    center = Boolean.TRUE;
                    orientRobot(-17);
                    ourSwerve.autoDrive(1.0, 17, 0.0, 3, targetSilver);
                    autoDriveStop = Boolean.TRUE;
                    autoDriveWait = Boolean.TRUE;
                    setState(autoStates.SWERVE_TO_WALL,700);
                }
                if (goldPosition == SampleRandomizedPositions.RIGHT) {
                    left = Boolean.TRUE;
                    orientRobot(60);
                    if(targetSilver) {
                        setState(autoStates.SWERVE_PULL_FORWARD, 700);
                    }
                    else{
                        setState(autoStates.SWERVE_TO_WALL, 700);
                    }
                }
                else {
//                    guess center
                    center = Boolean.TRUE;
                    orientRobot(-17);
                    setState(autoStates.SWERVE_TO_WALL,1000);
                }
                break;

            case SWERVE_PULL_FORWARD:
                ourSwerve.autoDrive(1.0, 17, 0.0, 5, targetSilver);
                autoDriveStop = Boolean.TRUE;
                autoDriveWait = Boolean.TRUE;
                setState(autoStates.SWERVE_TO_WALL, 1000);
                break;

//            Move to the wall
            case SWERVE_TO_WALL:
                wrist.setTargetPosition(-1200);
                extension.setTargetPosition(0);

                wrist.setPower(1);
                swerveSleep(1000);
                extension.setPower(1);
                if (targetSilver) {
                    ourSwerve.autoDrive(1.0, 286, 50.0, 83.0, targetSilver);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    // wait for wall move
                    setState(autoStates.SWERVE_TO_DEPOT, 5000);
                }
                else {
                    ourSwerve.autoDrive( 1.0, 286, 15.0, 76.0, targetSilver );
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    // wait for wall move
                    setState(autoStates.SWERVE_TO_DEPOT, 9000);
                }
                break;

            // Move to the depot
            case SWERVE_TO_DEPOT:
                // drive to the depot
                if (targetSilver) {
                    ourSwerve.autoDrive( 1.0, 225.0, 45.0, 135.0, targetSilver );
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_PLACE_MARKER, 5000);
                } else {
                    ourSwerve.autoDrive( 1.0, 37.0, 45.0, 127.0, targetSilver );
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_PLACE_MARKER, 6000);
                }
                break;

            // place marker
            case SWERVE_PLACE_MARKER:
                // stop robot
                ourSwerve.stopRobot();

                // drop the marker
                gameMarkDrop.setPosition(1);

                // wait for marker drop
                setState(autoStates.SWERVE_TO_CRATER, 1000);
                break;

            // Move to the crater
            case SWERVE_TO_CRATER:
                if(targetSilver) {
                    ourSwerve.autoDrive(1.0, 42, 45.0, 165.0, targetSilver);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    wrist.setTargetPosition(-2150);
                    wrist.setPower(-1);
                    setState(autoStates.SWERVE_CRATER_PARTICLES, 5000);
                }
                else {
                    ourSwerve.autoDrive(1.0, 233.0, 0.0, 200.0, targetSilver);
                    autoDriveWait = Boolean.TRUE;
                    autoDriveStop = Boolean.TRUE;
                    setState(autoStates.SWERVE_CRATER_PARTICLES, 5000);
                }
                break;

//            grab two balls from the crater
            case SWERVE_CRATER_PARTICLES:
//                not yet implemented
                wrist.setTargetPosition(-1900);
                extension.setTargetPosition(-1200);
                swerveSleep(1000);
                wrist.setPower(-1.0);
                extension.setPower(0.6);
                intake.setPower(-1);
                orientRobot(60);
                setState(autoStates.SWERVE_LAST_MOVE, 5000);
                break;

/***** Code to Potentially score balls in auto *****/

//            drive back to lander
            case SWERVE_PARTICLE_TO_LANDER:

                break;

//            score the balls
            case SWERVE_SCORE_BALLS:
//                not yet implemented
                break;

//            break the crater plane with arm
            case SWERVE_ARM_TO_CRATER:
//                not yet implemented
                break;

            // Waiting for final move to complete before done
            // We use this because waiting for this state still gives reporting of wait status

            case SWERVE_LAST_MOVE:
                // stop moving
                ourSwerve.stopRobot();

                setState(autoStates.SWERVE_DONE, 10);
                break;

            // All moves are done

            case SWERVE_DONE:
                // stop any movement
                ourSwerve.stopRobot();
                break;




            // **** TEST cases **** //

            // test turning robot to face 90 degrees

            case SWERVE_TEST_TURN_ROBOT:
                orientRobot(90.0);
                setState(autoStates.SWERVE_DONE, 1000);
                break;

            // test moving the robot 60cm (one mat tile, roughly) at 20 degrees

            case SWERVE_TEST_MOVE_ROBOT:
                ourSwerve.autoDrive( 0.6, 90.0, 0.0, 420.0, targetSilver );
                autoDriveWait = Boolean.TRUE;
                autoDriveStop = Boolean.TRUE;

                setState(autoStates.SWERVE_DONE, 4000);
                break;


            default:
                swerveDebug(1, "SwerveAuto::loop *ERROR*", "== unknown case state " +
                        getCurStateName() + " (" + revTankState + ")");
                setState(autoStates.SWERVE_DONE, 0);
                break;
        }

        // Report changes if not done

        if ( debugActive || ( revTankState != autoStates.SWERVE_DONE )) {
            loopEndReporting();
        }

        swerveDebug(500, "SwerveAuto::loop", "LOOP PASS DONE");
    }


    // ***********************************************************************
    // stop
    // ***=-*******************************************************************
    // Performs any actions that are necessary when the OpMode is disabled.
    // The system calls this member once when the OpMode is disabled.

    @Override
    public void stop() {
        swerveDebug(500, "SwerveAuto::stop", "START");

//        completely shut down tensorFlow
        vision.shutdown();


        // Call the super/base class stop method
        super.stop();

        swerveDebug(500, "SwerveAuto::stop", "DONE");
    }


    // ***********************************************************************
    // setState
    // ***********************************************************************
    // Set a new state to run, with a delay before it activates
    private void setState(autoStates myTarget, double myDelay) {

        // when debugging, be ready to stop
        if (debugActive && (debugStates-- < 1)) {
            swerveDebug(500, "SwerveAuto::setState", "DEBUG LIMIT - end now");
            myTarget = autoStates.SWERVE_DONE;
        }

        // Report change of state
        if (myTarget != revTankState) {
            swerveDebug(500, "SwerveAuto::setState", "STATE CHANGE--from '" +
                    getCurStateName() + "'  to '" +
                    getStateName(myTarget));
        }

        // Record the starting time for the new state
        stateStartTime = getRuntime();
        // Recored the intended delay
        stateWaitTime = myDelay;

        // set the state...
        revTankState = myTarget;

        // Send telemetry data to the driver station.
        swerveLog("State", "Autonomous State: " + getCurStateName() +
                ", state time = " + swerveNumberFormat.format(getRuntime() - stateStartTime));
    }


    // ***********************************************************************
    // checkStateReady
    // ***********************************************************************
    // Is the target delay past?
    // Any other conditions to wait for before changing?
    private Boolean checkStateReady() {
        // Check current time and requested delay
        if (!checkStateElapsed(stateWaitTime)) {
            return Boolean.FALSE;
        }

        // Add waits for motor positions or anything else here...

        return Boolean.TRUE;
    }


    // ***********************************************************************
    // checkStateElapsed
    // ***********************************************************************
    // Is the target delay past?
    private Boolean checkStateElapsed(double myDelay) {
        double curTime;
        double curDelay;

        curTime = getRuntime();
        curDelay = (curTime - stateStartTime) * 1000;

        // Check current time and requested delay
        if (curDelay < myDelay) {
            checkReport = "== current (" +
                    swerveNumberFormat.format(curDelay) + "' less than '" +
                    swerveNumberFormat.format(myDelay);

            // ONLY show this for very high debug levels, or it will overflow the logs
            swerveDebug(5000, "SwerveAuto::checkStateElapsed", "**Delaying** " + checkReport);

            if (debugLevel < 5000) {
                telemetry.addData("CheckElapsed", checkReport);
            }

            return Boolean.FALSE;
        } else if (debugLevel < 5000) {
            telemetry.addData("CheckElapsed", "--DONE--");
        }

        // Add waits for motor positions or anything else here...

        return Boolean.TRUE;
    }


    // ***********************************************************************
    // orientRobot
    // ***********************************************************************
    // turn the robot to a specific orientation
    private Boolean orientRobot(double newOrientationDegrees) {

        double newOrienation = newOrientationDegrees;
        double turnSpeed;

        // be sure we are not using automation
        ourSwerve.setSwerveMode(SwerveDrive.swerveModes.SWERVE_DRIVER);

        // check robot orientation
        ourSwerve.checkOrientation();

        // turn until within ~10 degrees
        while (Math.abs(newOrienation - ourSwerve.curHeading) > 5.0) {

            // never take longer than 1.5 seconds
            if (checkStateElapsed(10000)) {
                // stop the robot
                ourSwerve.stopRobot();

                return (Boolean.FALSE);
            }

            // turn faster if we need to turn more
            if (Math.abs(newOrienation - ourSwerve.curHeading) > 90.0) {
                turnSpeed = 0.20;
            } else {
                turnSpeed = 0.10;
            }
            if (newOrienation > ourSwerve.curHeading) {
                turnSpeed = -turnSpeed;
            }

            // turn the robot
            ourSwerve.driveRobot(0.0, 0.0, turnSpeed, 0.0, targetSilver);
        }

        // stop the robot
        ourSwerve.stopRobot();

        return (Boolean.TRUE);
    }

}