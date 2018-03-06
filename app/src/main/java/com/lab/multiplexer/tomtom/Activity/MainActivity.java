package com.lab.multiplexer.tomtom.Activity;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.RelativeLayout;

import com.lab.multiplexer.tomtom.Activity.Helper.ConnectivityReceiver;
import com.lab.multiplexer.tomtom.R;

public class MainActivity extends AppCompatActivity {
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    RelativeLayout orderBtn;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();
        editor.putBoolean("logged_in",true);
        editor.commit();

        orderBtn = (RelativeLayout) findViewById(R.id.btnOrderRide);
        orderBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkConnection()){

                    if (isGpsEnable()){

                        startActivity(new Intent(MainActivity.this, MapActivity.class));
                    }
                    else {
                        Snackbar snackbar = Snackbar
                                .make(v, "Please Turn On your GPS Or Location", Snackbar.LENGTH_LONG);

                        snackbar.show();
                    }
                }
                else {
                    Snackbar snackbar = Snackbar
                            .make(v, "No internet connection", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            }
        });
    }

    private boolean checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        return isConnected;
    }

    public boolean isGpsEnable(){

        final LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean gpsProviderEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);

        return gpsProviderEnabled;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(MainActivity.this,Settings.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onStop() {
        super.onStop();
        //AccountKit.logOut();
    }
}
