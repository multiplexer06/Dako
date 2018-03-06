package com.lab.multiplexer.tomtom.Activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.accountkit.Account;
import com.facebook.accountkit.AccountKit;
import com.facebook.accountkit.AccountKitCallback;
import com.facebook.accountkit.AccountKitError;
import com.facebook.accountkit.AccountKitLoginResult;
import com.facebook.accountkit.PhoneNumber;
import com.facebook.accountkit.ui.AccountKitActivity;
import com.facebook.accountkit.ui.AccountKitConfiguration;
import com.facebook.accountkit.ui.LoginType;
import com.facebook.accountkit.ui.SkinManager;
import com.facebook.accountkit.ui.UIManager;
import com.google.firebase.iid.FirebaseInstanceId;
import com.lab.multiplexer.tomtom.Activity.Helper.AppController;
import com.lab.multiplexer.tomtom.Activity.Helper.EndPoints;
import com.lab.multiplexer.tomtom.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.lab.multiplexer.tomtom.Activity.SplashScreen.MULTIPLE_PERMISSIONS;

public class Login extends AppCompatActivity {
    String[] permissions = new String[]{
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
    };
    String firebase_notification_token;
    public static int APP_REQUEST_CODE = 99;
    ProgressDialog prog_dialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        getSupportActionBar().hide();
        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();

        firebase_notification_token = FirebaseInstanceId.getInstance().getToken();
        Button login_btn = (Button) findViewById(R.id.login_btn);
        login_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkPermissions()) {
                    //  permissions  granted.
                    if (isInternetAvailable()) {
                        phoneLogin();

                    } else {
                        Snackbar snackbar = Snackbar
                                .make(v, "No internet connections", Snackbar.LENGTH_LONG);

                        snackbar.show();
                    }
                }

            }
        });
    }

    public void showLoading(){
        prog_dialog = ProgressDialog.show(Login.this, "",
                "Please wait...", true);
        prog_dialog.setCancelable(false);
        prog_dialog.show();
    }


    private void checkUser(final String passenger_phn,final String firebase_token) {

        StringRequest strReq = new StringRequest(Request.Method.POST, EndPoints.CHECK_USER,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response.toString());
                        try {
                            if(prog_dialog.isShowing()){
                                prog_dialog.dismiss();
                            }
                            JSONObject obj = new JSONObject(response);
                            String getMessageFromServer = obj.getString("message");
                            if (getMessageFromServer.equals("User Already Exist")) {
                                editor.putString("user_id",obj.getString("user_id"));
                                editor.putString("user_img",obj.getString("passenger_img"));
                                editor.putString("user_name",obj.getString("passenger_name"));
                                editor.commit();
                                Toast.makeText(Login.this,"Welcome back to Dako!!!",Toast.LENGTH_LONG).show();
                                Intent i = new Intent(Login.this,MainActivity.class);
                                startActivity(i);
                                finish();
                            } else {
                                Intent i = new Intent(Login.this,Registration.class);
                                startActivity(i);
                                finish();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("local Reg", "Error: " + error.getMessage());
                if(prog_dialog.isShowing()){
                    prog_dialog.dismiss();
                }
                Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show();
                //  progressBar.setVisibility(View.GONE);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("passenger_phn", passenger_phn);
                params.put("firebase_token", firebase_token);
                Log.i("Posting params: ", params.toString());

                return params;
            }

        };


        AppController.getInstance().addToRequestQueue(strReq, " ");
    }




    public void phoneLogin() {
        final Intent intent = new Intent(this, AccountKitActivity.class);
        AccountKitConfiguration.AccountKitConfigurationBuilder configurationBuilder =
                new AccountKitConfiguration.AccountKitConfigurationBuilder(
                        LoginType.PHONE,
                        AccountKitActivity.ResponseType.TOKEN); // or .ResponseType.TOKEN
        UIManager uiManager = new SkinManager(
                SkinManager.Skin.CONTEMPORARY,
                getResources().getColor(R.color.colorAccent),
                R.drawable.background,
                SkinManager.Tint.WHITE,
                0.10);

        configurationBuilder.setUIManager(uiManager);
        configurationBuilder.setReadPhoneStateEnabled(true);
        configurationBuilder.setReceiveSMS(true);
        // ... perform additional configuration ...
        intent.putExtra(
                AccountKitActivity.ACCOUNT_KIT_ACTIVITY_CONFIGURATION,
                configurationBuilder.build());
        startActivityForResult(intent, APP_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(
            final int requestCode,
            final int resultCode,
            final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == APP_REQUEST_CODE) { // confirm that this response matches your request
            AccountKitLoginResult loginResult = data.getParcelableExtra(AccountKitLoginResult.RESULT_KEY);
            String toastMessage;
            if (loginResult.getError() != null) {
                toastMessage = loginResult.getError().getErrorType().getMessage();
                //showErrorActivity(loginResult.getError());
                Toast.makeText(this, loginResult.getError() + "", Toast.LENGTH_LONG).show();
            } else if (loginResult.wasCancelled()) {
                toastMessage = "Login Cancelled";
            } else {
                if (loginResult.getAccessToken() != null) {
                    toastMessage = "Success:" + loginResult.getAccessToken().getAccountId();
                } else {
                    toastMessage = String.format(
                            "Success:%s...",
                            loginResult.getAuthorizationCode().substring(0, 10));
                }

                // If you have an authorization code, retrieve it from
                // loginResult.getAuthorizationCode()
                // and pass it to your server and exchange it for an access token.

                // Success! Start your next activity...
                getPhoneNumber();
            }

            // Surface the result to your user in an appropriate way.
           /* Toast.makeText(
                    this,
                    toastMessage,
                    Toast.LENGTH_LONG)
                    .show();*/


        }
    }

    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;

    }

    public void getPhoneNumber() {

        AccountKit.getCurrentAccount(new AccountKitCallback<Account>() {
            @Override
            public void onSuccess(final Account account) {
                // Get Account Kit ID
                String accountKitId = account.getId();

                // Get phone number
                PhoneNumber phoneNumber = account.getPhoneNumber();
                String phoneNumberString = phoneNumber.toString();
                Log.e("Login", phoneNumberString);
                editor.putString("user_phn",phoneNumberString);
                editor.commit();
                showLoading();
                checkUser(phoneNumberString,pref.getString("regId",""));
                // Get email
                //String email = account.getEmail();
            }

            @Override
            public void onError(final AccountKitError error) {
                // Handle Error
                Log.e("Login", error.toString());

            }
        });


    }

    private boolean checkPermissions() {
        int result;
        List<String> listPermissionsNeeded = new ArrayList<>();
        for (String p : permissions) {
            result = ContextCompat.checkSelfPermission(this, p);
            if (result != PackageManager.PERMISSION_GRANTED) {
                listPermissionsNeeded.add(p);
            }
        }
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    Toast.makeText(this, "Thanks for allowing the permissions", Toast.LENGTH_LONG).show();
                    if (isInternetAvailable()) {
                        phoneLogin();

                    } else {
                        Toast.makeText(this, "No internet connection!", Toast.LENGTH_LONG).show();
                    }
                } else {
                    // no permissions granted.
                    Toast.makeText(this, "Sorry you can't proceed without allowing the permissions", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }
}
