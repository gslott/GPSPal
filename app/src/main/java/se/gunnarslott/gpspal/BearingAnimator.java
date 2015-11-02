package se.gunnarslott.gpspal;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * Created by Gunnar Slott (mail@gunnarslott.se) on 15-05-24.
 * This class tries to smooth out the jerkiness of reported bearing from a location manager.
 * It´s a thread that calcs and reports if the bearing value should go up or down and has a
 * built in timer to set the speed of degrees/sec movement
 */

class BearingAnimator extends Thread {

    private final String TAG = "BearingAnimator";
    private Boolean quit = false;
    private final Handler h;
    //Current position in animation, striving towards above given value
    private int roundedCurrentValue = 0;
    private int roundedLastReportedBearing = 0;


    public void setNewBearing(Float newBearing){


        //Recieve bearing from GPS, save last reported value
        roundedLastReportedBearing = Math.round(newBearing);

        Log.d(TAG, "lastReportedBearing = " + newBearing);

    }


    private int calcBearing(){
    /*This method calcs wheter the new bearing is east or west of the old one.
    *It´s a little tricky since it may pass true north on the shortest way
     */

        int distance1;
        int distance2;

        if (roundedCurrentValue > roundedLastReportedBearing) {

            //Calc distance between values if go to west
            distance1 = roundedCurrentValue - roundedLastReportedBearing;

            //Calc distance go to east
            distance2 = (360 - roundedCurrentValue) + roundedLastReportedBearing;
            //(Distance to North)+(distance from North to new_direction)

            //Return if the new value is west (go -1 degree) or east (go +1 degree)
            if (distance1 < distance2) {
                return -1;
            } else return 1;

        } else if (roundedCurrentValue < roundedLastReportedBearing) {

            //Calc distance go east
            distance1 = roundedLastReportedBearing - roundedCurrentValue;

            //Calc distance go west
            distance2 = (360 - roundedLastReportedBearing) + roundedCurrentValue;
            //(Distance to North)+(distance from North to new_direction)

            if (distance1 < distance2) {
                return 1;
            } else return -1;
        }

        //Bearing hasn´t changed
        return 0;
    }

    public BearingAnimator() {
        h = MainActivity.handler;
        this.start();
    }

    public void quit(Boolean b){

        //Stops the while loop in run()
        quit = b;
    }

    private void updateView(){

        //Creates a message obj and sends it back via the handler given in the constructor
        Message msgObj = h.obtainMessage();
        msgObj.arg1 = roundedCurrentValue;
        h.sendMessage(msgObj);
    }

    @Override
    public void run() {

        Log.d(TAG, "Run");

            try {
              while (!quit) {

                  int cb = calcBearing();

                  //Handle turns passing true north
                  if (roundedCurrentValue == 359 && cb == 1){
                      roundedCurrentValue = 0;

                  } else if (roundedCurrentValue == 0 && cb == -1){
                      roundedCurrentValue = 359;

                  } else roundedCurrentValue = roundedCurrentValue + cb;

                  updateView();
                  //Animation fps
                  int ANIMATION_SLEEP_TIME = 100;
                  sleep(ANIMATION_SLEEP_TIME);
                }

            }catch (InterruptedException e) {
                    e.printStackTrace();
            }
    }
}


