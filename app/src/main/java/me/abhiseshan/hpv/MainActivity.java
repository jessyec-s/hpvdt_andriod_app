package me.abhiseshan.hpv;

/*testing to see committ message */
/*second test*/

import android.animation.ValueAnimator;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.List;

import io.palaima.smoothbluetooth.Device;
import io.palaima.smoothbluetooth.SmoothBluetooth;

public class MainActivity extends AppCompatActivity {

    int currentSpeed = 0, newSpeed = 0;
    int currentDist = 0, newDist = 0;
    int currentCadence = 0, newCadence = 0;
    int currentLeftWarning = 0, newLeftWarning = 0;
    int currentRightWarning = 0, newRightWarning = 0;

    //int currentFrontTP = 0, newFrontTP = 0;
    //int currentRearTP = 0, newRearTP = 0;

    float currentFloatDist = 0, newFloatDist = 0;

    Boolean isLandingGearOut = false;
    Boolean isLeftIndicatorOn = false;
    Boolean isRightIndicatorOn = false;
    Boolean isHeadlightOn = false;

    ImageView leftArrow;
    ImageView rightArrow;
    ImageView connectionStatusIndicator;
    ImageView landingGear;
    ImageView headLight;
    ImageView LeftWarningSignal;
    ImageView RightWarningSignal;

    TextView speedTV;
    TextView distTV;
    TextView cadenceTV;
    //TextView frontTireTV;
    //TextView rearTireTV;
    TextView distFloatTV;

    Boolean toggle = false;

    View decorView;

    //private final String address = "20:15:08:31:33:01";

    private SmoothBluetooth mSmoothBluetooth;

    Thread indicatorThread;

    boolean threadInterrupt = false;

    String recvMsg = "";
    private static char DELIMITER = '#';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Hiding the system bars. This is done so that we can get the full screen view.
        decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE);

        //Initialize all views
        leftArrow = (ImageView) findViewById(R.id.leftArrow);
        rightArrow = (ImageView) findViewById(R.id.rightArrow);
        connectionStatusIndicator = (ImageView) findViewById(R.id.connection);
        //landingGear = (ImageView) findViewById(R.id.landingGear);
        headLight = (ImageView) findViewById(R.id.headlight);

        LeftWarningSignal = (ImageView) findViewById(R.id.flashing_left);
        RightWarningSignal = (ImageView) findViewById(R.id.flashing_right);

        speedTV = (TextView) findViewById(R.id.speedTextView);
        distTV = (TextView) findViewById(R.id.DistanceTextView);
        cadenceTV = (TextView) findViewById(R.id.CadenceTextView);
        //frontTireTV = (TextView) findViewById(R.id.tireFront);
        //rearTireTV = (TextView) findViewById(R.id.tireBack);
        distFloatTV = (TextView) findViewById(R.id.distFloatTextView);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onStart(){
        super.onStart();

        //Setting everything to off state
        connectionStatusIndicator.setColorFilter(getResources().getColor(R.color.blackOverlay));
        headLight.setColorFilter(getResources().getColor(R.color.blackOverlay));
        leftArrow.setColorFilter(getResources().getColor(R.color.blackOverlay));
        landingGear.setColorFilter(getResources().getColor(R.color.blackOverlay));
        rightArrow.setColorFilter(getResources().getColor(R.color.blackOverlay));
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume(){
        super.onResume();

        mSmoothBluetooth = new SmoothBluetooth(this, SmoothBluetooth.ConnectionTo.OTHER_DEVICE, SmoothBluetooth.Connection.SECURE, mListener);
        mSmoothBluetooth.tryConnection();

        threadInterrupt = false;

        //Keep running always and if either left or right indicator is on, it will change
        indicatorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                //If either left or right is on, value depends on toggle. Therefore it will switch states every 2000ms.
                while (!threadInterrupt){
                    if (isLeftIndicatorOn || isRightIndicatorOn) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                toggle(leftArrow, isLeftIndicatorOn && toggle);
                                toggle(rightArrow, isRightIndicatorOn && toggle);
                            }
                        });
                    }

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        //e.printStackTrace();
                    }
                    toggle = !toggle;
                }
            }
        });

        indicatorThread.start();
    }


    //Toggle between on and off states of an image view by setting a colour filter grey (0ff) and white (On)
    @SuppressWarnings("deprecation")
    private void toggle(ImageView img, Boolean status){
        if (status)
            img.clearColorFilter();
        else
            img.setColorFilter(getResources().getColor(R.color.blackOverlay));
    }

    private void toggleColour(ImageView img, Integer status) {
        if (status == 0) //no warning
            img.setColorFilter(getResources().getColor(R.color.greenOverlay));
        else if (status == 1) //70%
            img.setColorFilter(getResources().getColor(R.color.yellowOverlay));
        else if (status == 2) //80%
            img.setColorFilter(getResources().getColor(R.color.orangeOverlay));
        else if (status == 3) //90%
            img.setColorFilter(getResources().getColor(R.color.redOverlay));
        else
            img.clearColorFilter();
    }


    //Aniate the changing of numbers in a text view
    private void animateTextView(int initialValue, int finalValue, final TextView textview) {

        ValueAnimator valueAnimator = ValueAnimator.ofInt(initialValue, finalValue);
        valueAnimator.setDuration(1500);

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                //String b = String.format("%03d", a); for 000
                textview.setText(valueAnimator.getAnimatedValue().toString());
            }
        });
        valueAnimator.start();
    }

    //Helps with making the UI full screen
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);}
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    private SmoothBluetooth.Listener mListener = new SmoothBluetooth.Listener() {
        @Override
        public void onBluetoothNotSupported() {
            //device does not support bluetooth
            //Toast.makeText(MainActivity.this, "Bluetooth not found", Toast.LENGTH_SHORT).show();
            /*new MaterialDialog.Builder(getApplicationContext())
                    .title("Device Incompatible")
                    .content("This device is not compatible with the application. Please install on another device")
                    .positiveText("Okay")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show(); */
        }

        @Override
        public void onBluetoothNotEnabled() {
            //bluetooth is disabled, probably call Intent request to enable bluetooth
            //Toast.makeText(MainActivity.this, "Please Enable Bluetooth", Toast.LENGTH_SHORT).show();

            /*new MaterialDialog.Builder(getApplicationContext())
                    .title("Bluetooth not Enabled")
                    .content("Please switch on bluetooth and start the application again.")
                    .positiveText("Okay")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show(); */
        }

        @Override
        public void onConnecting(Device device) {
            //called when connecting to particular device
            Toast.makeText(MainActivity.this, "Connecting to " + device.getName(), Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "Connecting to " + device.getName());
        }

        @Override
        public void onConnected(Device device) {
            //called when connected to particular device
            toggle(connectionStatusIndicator, true);
            Toast.makeText(MainActivity.this, "Connected to " + device.getName(), Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "Connected to " + device.getName());
        }

        @Override
        public void onDisconnected() {
            //called when disconnected from device
            toggle(connectionStatusIndicator, false);
            Toast.makeText(MainActivity.this, "Disconnected", Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "Disconnected. Trying to connect.");
            mSmoothBluetooth.tryConnection();
        }

        @Override
        public void onConnectionFailed(Device device) {
            //called when connection failed to particular device
            toggle(connectionStatusIndicator, false);
            //Toast.makeText(MainActivity.this, "Connection Failed to " + device.getName(), Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "Connection failed to " + device.getName() + ". Trying again");
            mSmoothBluetooth.tryConnection();
        }

        @Override
        public void onDiscoveryStarted() {
        }

        @Override
        public void onDiscoveryFinished() {

        }

        @Override
        public void onNoDevicesFound() {
            //Toast.makeText(MainActivity.this, "No Devices Found", Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "no devices found");

            /*new MaterialDialog.Builder(getApplicationContext())
                    .title("No devices Found")
                    .content("Please pair the HC-06 bluetooth module before trying to connect")
                    .positiveText("Okay")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            finish();
                        }
                    })
                    .show();*/
            //called when no devices found
        }

        @Override
        public void onDevicesFound(final List<Device> deviceList,
                                   final SmoothBluetooth.ConnectionCallback connectionCallback) {
            //receives discovered devices list and connection callback
            //you can filter devices list and connect to specific one
            //connectionCallback.connectTo(deviceList.get(position));

            for (int i = 0; i < deviceList.size(); i++){

                Toast.makeText(MainActivity.this, deviceList.get(i).getName(), Toast.LENGTH_SHORT).show();
                System.out.println("PRINTING DEVICES:");
                System.out.println(deviceList.get(i).getName());
                System.out.println(deviceList.get(i).getName());
                if (deviceList.get(i).getName().contentEquals("HC-05")) {
                    connectionCallback.connectTo(deviceList.get(i));
                    return;
                }
            }

            Toast.makeText(MainActivity.this, "Could not find the device", Toast.LENGTH_SHORT).show();
            Log.d("Bluetooth", "Could not find the device");
            toggle(connectionStatusIndicator, false);
        }

        @Override
        public void onDataReceived(int data) {
            //receives all bytes
            List<Integer> mBuffer = new ArrayList<>();
            mBuffer.add(data);

            if (!mBuffer.isEmpty()){
                StringBuilder sb = new StringBuilder();
                for (int integer : mBuffer) {
                    sb.append((char)integer);
                }
                mBuffer.clear();
                recvMsg = recvMsg.concat(sb.toString());
            }

            /*
             * We are basically appending everything to the string buffer until we reach the delimiter. The default delimiter
             * set by me is # but could have been changed.
             *
             * When we detect a delimiter or more than one delimiter, we split the strings to m number of strings where m
             * is the number of delimiters + 1.
             * We consider the size - 2, the second last string to update the GUI since it is the latest fully received message.
             * The size - 1 that is the last string is incomplete and hence is pushed back into the buffer.
             *
             */

            if (recvMsg.contains("" + DELIMITER)){
                String[] splitStrings = recvMsg.split("\\s*" + DELIMITER + "\\s*");
                if (splitStrings.length >= 2) {
                    updateGUI(splitStrings[splitStrings.length - 2]);
                    recvMsg = splitStrings[splitStrings.length - 1];
                }
            }
        }
    };

    private void updateGUI(String data) {

        Log.d("Data", data);

        String[] parseString = data.split("\\s*,\\s*");

        if (parseString.length != 8) /*changed this to 8*/
            return;

        Log.d("strnmg", parseString[4]);

        newSpeed = (int) Double.parseDouble(parseString[3]); //changed this to 3
        newSpeed = (int)(newSpeed * 3.6); //Converting to km/h
        newDist = (int) Double.parseDouble(parseString[4]);
        newFloatDist = ((float) newDist)/1000;
        newDist = newDist/1000; //Converting into km
        newFloatDist -= newDist;
        newFloatDist *= 10; //Account for one decimal place

        newCadence = Integer.parseInt(parseString[5]); //changed this to 5
        //newFrontTP = Integer.parseInt(parseString[7]);
        //newRearTP = Integer.parseInt(parseString[8]);
        newLeftWarning = Integer.parseInt(parseString[6]);
        newRightWarning = Integer.parseInt(parseString[7]);


        Log.d("Updating UI", parseString[0] + "," + parseString[1] + "," + parseString[2] + "," + parseString[3] + "," + parseString[4] + "," + parseString[5] + "," + parseString[6] + "," + parseString[7]);
/*
        if (parseString[0].equals("1") != isLandingGearOut) {
            isLandingGearOut = parseString[0].equals("1");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toggle(landingGear, isLandingGearOut);
                }
            });
        } */

        if (parseString[0].equals("1") != isHeadlightOn) {
            isHeadlightOn = parseString[0].equals("1");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("headlight", "Changing");
                    toggle(headLight, isHeadlightOn);
                }
            });
        }

        /*prototype function to control left, right warnings */

        if (newLeftWarning != currentLeftWarning) {
            currentLeftWarning = newLeftWarning;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("LeftWarning", "Changing");
                    toggleColour(LeftWarningSignal, currentLeftWarning); //changing the left warning colour
                }
            });

        }

        if (newRightWarning != currentRightWarning) {
            currentRightWarning = newRightWarning;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d("LeftWarning", "Changing");
                    toggleColour(RightWarningSignal, currentRightWarning); //changing the right warning colour

                }
            });

        }

        if (parseString[1].equals("1") != isLeftIndicatorOn) {
            isLeftIndicatorOn = parseString[1].equals("1");
            //To make sure the left signal is off when signal changes to 0
            if (!isLeftIndicatorOn)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toggle(leftArrow, false);
                    }
                });
        }

        if (parseString[2].equals("1") != isRightIndicatorOn) {
            isRightIndicatorOn = parseString[2].equals("1");
            //To make sure the right signal is off when signal changes to 0
            if (!isRightIndicatorOn)
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toggle(rightArrow, false);
                    }
                });
        }


        if (newSpeed != currentSpeed) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    animateTextView(currentSpeed, newSpeed, speedTV);
                }
            });
            currentSpeed = newSpeed;
        }

        if (newDist != currentDist) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    distTV.setText("" + newDist);
                    //animateTextView(currentDist, newDist, distTV);
                }
            });
            currentDist = newDist;
        }

        if (newFloatDist != currentFloatDist) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String floatText = "." + newFloatDist;
                    distFloatTV.setText(floatText);
                }
            });
            currentFloatDist = newFloatDist;
        }

        if (newCadence != currentCadence) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    cadenceTV.setText("" + newCadence);
                    //animateTextView(currentCadence, newCadence, cadenceTV);
                }
            });
            currentCadence = newCadence;
        }
/*
        if (newFrontTP != currentFrontTP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    animateTextView(currentFrontTP, newFrontTP, frontTireTV);
                }
            });
            currentFrontTP = newFrontTP;
        }

        if (newRearTP != currentRearTP) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    animateTextView(currentRearTP, newRearTP, rearTireTV);
                }
            });
            currentRearTP = newRearTP;
        } */
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSmoothBluetooth.stop();
    }
}