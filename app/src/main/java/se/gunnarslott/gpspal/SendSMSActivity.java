package se.gunnarslott.gpspal;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBarActivity;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import static android.telephony.PhoneNumberUtils.isGlobalPhoneNumber;

/**
 * Created by Gunnar Slott
 * Activity for sending SMS with current location as a Google map link
 */
public class SendSMSActivity extends ActionBarActivity implements View.OnClickListener {

    private final String TAG="SendSMSActivity";


    private EditText etNmbr;
    private EditText etMessage;

    public SendSMSActivity(){
        Log.d(TAG, "Contructor");
    }

    private void sendSMS(String phoneNumber, String message)
    {

            Log.d(TAG, "has telephone cap");
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, null, null);

            //TODO Add and handle callback on sent message




    }

    @Override
    public void onPause() {
        super.onPause();


    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.d(TAG, "onRestoreInstanceState");
    }

    @Override
    protected void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        //Get lat/long passed from MainActivity
        Intent callingIntent = getIntent();

        double last_lat = callingIntent.getDoubleExtra("lat", 0);
        double last_long = callingIntent.getDoubleExtra("long", 0);

        Log.d(TAG, Double.toString(last_lat));
        Log.d(TAG, Double.toString(last_long));

        //Set message as Google map link
        //Format: http://maps.google.com/?q=<lat>,<lng>
        etMessage.setText("http://maps.google.com/?q=" + String.valueOf(last_lat) + "," + String.valueOf(last_long));


    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_send_sms);

        //Get button id:s and set click listeners
        Button btnSend = (Button) findViewById(R.id.btnSend);
        btnSend.setOnClickListener(this);
        Button btnCancle = (Button) findViewById(R.id.btnCancel);
        btnCancle.setOnClickListener(this);

        //Get text fields
        etNmbr = (EditText)findViewById(R.id.etPhoneNumber);
        etMessage = (EditText)findViewById(R.id.etMessageBody);

    }

    private void showToast(String s){

        //General showToast method
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, s, duration);
        toast.show();
    }

    @Override
    public void onClick(View v) {
        int view_id = v.getId();
        switch (view_id) {

            //Send SMS
            case R.id.btnSend:

                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                    // THIS PHONE HAS SMS FUNCTIONALITY
                    Log.d(TAG, "has telephone cap");

                    String phnnmbr = etNmbr.getText().toString();

                    if (isGlobalPhoneNumber(phnnmbr)) {
                        sendSMS(phnnmbr, etMessage.getText().toString());
                    } else showToast(getString(R.string.not_valid_phonenumber));

                    Intent i = new Intent(this, MainActivity.class);
                    startActivity(i);
                    finish();

                } else {

                    // THIS PHONE HAS NOT SMS FUNCTIONALITY
                    Log.d(TAG, "has NOT telephone cap");

                    AlertDialog alertDialog = new AlertDialog.Builder(SendSMSActivity.this).create();
                    //TODO add references to resourses instead of text
                    alertDialog.setTitle("No SMS");
                    alertDialog.setMessage(getResources().getString(R.string.no_sms_capability));

                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });

                    alertDialog.show();


                }
                break;

            //Go back to main activity
            case R.id.btnCancel:

                Intent ii = new Intent(this, MainActivity.class);
                startActivity(ii);
                finish();
            break;
        }

    }
}
