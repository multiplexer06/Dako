package com.lab.multiplexer.tomtom.Activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.accountkit.AccountKit;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.lab.multiplexer.tomtom.R;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.hdodenhof.circleimageview.CircleImageView;

import static com.lab.multiplexer.tomtom.R.id.login_button;

public class Settings extends AppCompatActivity {
    TextView txtName, txtPhn;


    TextView tvAbout;
    TextView tvBanla;
    TextView fbLoginText;
    TextView tvFreeRides;
    TextView tvPromotions;
    TextView tvSupport;
    TextView tvLogout;
    TextView tvHistory;

Intent starterIntent;

    RelativeLayout languageButton;
    CircleImageView profileImage;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    RelativeLayout btnLogOut, btnFbConnect;
    @BindView(R.id.support_button)
    RelativeLayout supportButton;
    LoginButton loginButton;
    String dp;

    Button updateProfile;
    private CallbackManager callbackManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);
        FacebookSdk.sdkInitialize(getApplicationContext());

        tvAbout = findViewById(R.id.tv_about);
        tvBanla = findViewById(R.id.tv_banla);
        fbLoginText = findViewById(R.id.fbLoginText);
        tvFreeRides =  findViewById(R.id.tv_free_rides);
        tvPromotions = findViewById(R.id.tv_promotions);
        tvSupport = findViewById(R.id.tv_support);
        tvLogout = findViewById(R.id.tv_logut);
        tvHistory = findViewById(R.id.tv_history);




        languageButton = findViewById(R.id.language_layout);

        callbackManager = CallbackManager.Factory.create();
        getSupportActionBar().setTitle("Settings");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();
        txtName = (TextView) findViewById(R.id.txtName);
        txtPhn = (TextView) findViewById(R.id.txtPhn);
        profileImage = (CircleImageView) findViewById(R.id.profile_image);
        updateProfile = findViewById(R.id.update_button);
        updateProfile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Settings.this, EditProfileActivity.class);
                startActivity(intent);
            }
        });
        txtName.setText(pref.getString("user_name", ""));
        txtPhn.setText(pref.getString("user_phn", ""));
        Log.e("Tag", "img:" + pref.getString("user_img", "").toString().trim());
        if (pref.getString("user_img", "").equals("No image")) {
            Picasso.with(this).load(R.drawable.user).into(profileImage);
        } else {
            Picasso.with(this).load(pref.getString("user_img", "").toString().trim()).error(R.drawable.user).into(profileImage);
        }

        btnLogOut = (RelativeLayout) findViewById(R.id.relBtnLogOut);
        btnLogOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callTheLogOutAlertDialog();
            }
        });

        List<String> permissions = new ArrayList<String>();
        permissions.add("user_friends");
        permissions.add("email");
        permissions.add("user_birthday");
        permissions.add("public_profile");
        supportButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent supportIntent = new Intent(Settings.this, SupportActivity.class);
                startActivity(supportIntent);
            }
        });
        loginButton = (LoginButton) findViewById(login_button);
        loginButton.setReadPermissions(permissions);
        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isNetworkAvailable()) {
                    Snackbar snackbar = Snackbar
                            .make(v, "No internet connections", Snackbar.LENGTH_LONG);

                    snackbar.show();
                }
            }
        });
        fbLoginText = (TextView) findViewById(R.id.fbLoginText);
        btnFbConnect = (RelativeLayout) findViewById(R.id.fbLoginButton);
        if (isConnectedWithFacbook(pref.getString("user_img", ""))) {
            fbLoginText.setText("Connected with facebook");
            fbLoginText.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnFbConnect.setEnabled(false);
        }
        btnFbConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loginButton.performClick();
            }
        });

    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (activeNetworkInfo != null && activeNetworkInfo.isConnected()) {
            loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    // App code

                    String accessToken = loginResult.getAccessToken().getToken();
                    Log.i("accessToken", accessToken);

                    GraphRequest request = GraphRequest.newMeRequest(loginResult.getAccessToken(), new GraphRequest.GraphJSONObjectCallback() {
                        @Override
                        public void onCompleted(JSONObject object, GraphResponse response) {
                            Log.i("Settings Facebook", response.toString());
                            // Get facebook data from login

                            Bundle bFacebookData = getFacebookData(object);
                            //edtName.append( bFacebookData.getString("first_name") + " " + bFacebookData.getString("last_name"));
                            Log.e("Facebook name", bFacebookData.getString("first_name") + " " + bFacebookData.getString("last_name"));
                            Log.e("Facebook gender", bFacebookData.getString("gender"));
                            Log.e("Facebook birthday", bFacebookData.getString("birthday"));
                            editor.putString("user_img", dp);
                            editor.putString("user_name", bFacebookData.getString("first_name") + " " + bFacebookData.getString("last_name"));
                            editor.commit();
                            Toast.makeText(Settings.this, "Your profile information has been updated locally", Toast.LENGTH_LONG).show();
                            LoginManager.getInstance().logOut();
                            Intent i = new Intent(Settings.this, Settings.class);
                            startActivity(i);
                            finish();
                            overridePendingTransition(0, 0);
                            /*if(!bFacebookData.containsKey("email")){
                                Log.i("email", "null");
                                prog_dialog = ProgressDialog.show(MainActivity.this, "",
                                        "Please wait...", true);
                                prog_dialog.setCancelable(false);
                                prog_dialog.show();
                                checkEmail(bFacebookData.getString("idFacebook"),"FB");
                            }else {
                                prog_dialog = ProgressDialog.show(MainActivity.this, "",
                                        "Please wait...", true);
                                prog_dialog.setCancelable(false);
                                prog_dialog.show();
                                Log.i("email", bFacebookData.getString("email").toString());
                                checkEmail(bFacebookData.getString("email"),"FB");
                            }*/

                       /* editor.putString("user_id", bFacebookData.getString("idFacebook"));
                        editor.putString("name", name);
                        editor.putBoolean("status", true);
                        editor.commit();
                        // db.insertUser(name, bFacebookData.getString("email"), bFacebookData.getString("idFacebook"),0,0);
                        Log.i("Bundle Data", bFacebookData.getString("first_name") + bFacebookData.getString("email") + bFacebookData.getString("idFacebook"));
                        String email = bFacebookData.getString("email");
                        String device_id = Settings.Secure.getString(MainActivity.this.getContentResolver(),
                                Settings.Secure.ANDROID_ID);
                        if (email != null) {
                            backEndUserDataSending(name, bFacebookData.getString("email"), bFacebookData.getString("idFacebook"), 0, profilePicUrl, device_id);
                        } else if (profilePicUrl == null || profilePicUrl.equals("")) {
                            backEndUserDataSending(name, bFacebookData.getString("email"), bFacebookData.getString("idFacebook"), 0, "No Image", device_id);
                        } else {
                            backEndUserDataSending(name, "No Email", bFacebookData.getString("idFacebook"), 0, profilePicUrl, device_id);
                        }

                        editor.putString("photo_url", profilePicUrl);
                        editor.commit();*/

                            // backEndUserDataSending(name, bFacebookData.getString("email"), bFacebookData.getString("idFacebook"), 0);
                                               /* Intent playIntent = new Intent(MainActivity.this, MainActivityPlay.class);
                                                Log.actual_score("Intent", "Dhur");
                                                startActivity(playIntent);
                                                overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                                finish();*/
                        }
                    });
                    Bundle parameters = new Bundle();
                    parameters.putString("fields", "id, first_name, last_name, email,gender, birthday, location");
                    request.setParameters(parameters);
                    request.executeAsync();


                                        /*if (!db.checkQuestionTableData()) {
                                            new MainActivity.databaseWork().execute();

                                        }
                                        else {
                                            Intent playIntent = new Intent(MainActivity.this, MainActivityPlay.class);
                                            startActivity(playIntent);
                                            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                                            finish();
                                        }*/
                }

                @Override
                public void onCancel() {
                    // App code
                }

                @Override
                public void onError(FacebookException exception) {
                    // App code
                }
            });

        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public boolean isConnectedWithFacbook(String sentence) {
        boolean bool = false;
        String search = "facebook";
        if (sentence.toLowerCase().indexOf(search.toLowerCase()) != -1) {
            //System.out.println("I found the keyword");
            bool = true;
        } else {
            System.out.println("not found");
        }
        return bool;
    }

    private Bundle getFacebookData(JSONObject object) {
        Bundle bundle = new Bundle();
        try {
            String id = object.getString("id");

            try {
                URL profile_pic = new URL("https://graph.facebook.com/" + id + "/picture?width=200&height=200");
                // profilePicUrl = profile_pic + "";
                Log.i("profile_pic", profile_pic + "");
                dp = profile_pic.toString();
                bundle.putString("profile_pic", profile_pic.toString());
            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            }
            bundle.putString("idFacebook", id);
            if (object.has("first_name"))
                bundle.putString("first_name", object.getString("first_name"));
            if (object.has("last_name"))
                bundle.putString("last_name", object.getString("last_name"));
            if (object.has("email"))
                bundle.putString("email", object.getString("email"));
            if (object.has("gender"))
                bundle.putString("gender", object.getString("gender"));
            if (object.has("birthday"))
                bundle.putString("birthday", object.getString("birthday"));
            if (object.has("location"))
                bundle.putString("location", object.getJSONObject("location").getString("name"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return bundle;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }


    public void callTheLogOutAlertDialog() {
        new AlertDialog.Builder(this)
                .setTitle(" ")
                .setMessage("Are you sure you want to log out?")
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with log out
                        LoginManager.getInstance().logOut();
                        AccountKit.logOut();
                        editor.putBoolean("logged_in", false);
                        editor.commit();
                        Intent i = new Intent(Settings.this, Login.class);
                        startActivity(i);
                        finishAffinity();
                    }
                })
                .setIcon(R.drawable.logo)
                .show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
