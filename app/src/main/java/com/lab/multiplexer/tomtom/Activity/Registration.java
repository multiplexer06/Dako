package com.lab.multiplexer.tomtom.Activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;
import com.lab.multiplexer.tomtom.Activity.Helper.AndroidMultiPartEntity;
import com.lab.multiplexer.tomtom.Activity.Helper.AppController;
import com.lab.multiplexer.tomtom.Activity.Helper.EndPoints;
import com.lab.multiplexer.tomtom.R;
import com.squareup.picasso.Picasso;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lab.multiplexer.tomtom.R.id.edt_name;
import static com.lab.multiplexer.tomtom.R.id.login_button;

public class Registration extends AppCompatActivity {
    LoginButton loginButton;
    private CallbackManager callbackManager;
    String dp;
    EditText edtName, edtAge, edtEmail;
    RadioGroup radioSex, radioLocation;
    RadioButton radioButton, radioFemaleButton,radioMaleButton;
    Button saveBtn;
    String femaleChooser = "-", genderSex = "-", location = "-";
    RelativeLayout photoChooser;
    public String filename = "";
    CircleImageView profileImage, add;
    boolean isPhotoUploaded = false;
    HttpPost httppost;
    long totalSize = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    ProgressDialog prog_dialog;
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);
        FacebookSdk.sdkInitialize(getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        getSupportActionBar().setTitle("Create Profile");
        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();
        List<String> permissions = new ArrayList<String>();
        permissions.add("user_friends");
        permissions.add("email");
        permissions.add("user_birthday");
        permissions.add("public_profile");
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
        edtName = (EditText) findViewById(edt_name);
        edtAge = (EditText) findViewById(R.id.edtAge);
        edtEmail = (EditText) findViewById(R.id.edtEmail);
        radioSex = (RadioGroup) findViewById(R.id.radioSex);
        radioLocation = (RadioGroup) findViewById(R.id.radioLocation);
        saveBtn = (Button) findViewById(R.id.saveBtn);
        radioFemaleButton = (RadioButton) findViewById(R.id.radioFemale);
        radioMaleButton = (RadioButton) findViewById(R.id.radioMale);
        profileImage = (CircleImageView) findViewById(R.id.profile_image);
        add = (CircleImageView) findViewById(R.id.add);
        radioFemaleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    femaleChooserDialog("Please choose your priority");
                }
            }
        });
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions(Registration.this);
            }
        });
        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyStoragePermissions(Registration.this);
            }
        });

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (edtName.getText().toString().equals("")) {
                    edtName.setError("Please enter your name first");
                } else if (edtAge.getText().toString().equals("")) {
                    edtAge.setError("Please enter your age here");
                } else if (!isValidEmail(edtEmail.getText().toString())) {
                    edtEmail.setError("Please enter your email correctly");
                } else {
                    radioButton = (RadioButton) findViewById(radioSex.getCheckedRadioButtonId());
                    genderSex = radioButton.getText().toString();
                    radioButton = (RadioButton) findViewById(radioLocation.getCheckedRadioButtonId());
                    location = radioButton.getText().toString();
                    if (genderSex.equals("Female") && femaleChooser.equals("-")) {
                        femaleChooserDialog("Please choose your priority to proceed");
                    } else if (!isInternetAvailable()) {
                        Snackbar snackbar = Snackbar
                                .make(v, "No internet connections", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else {
                        if (isPhotoUploaded) {
                            new UploadPictureToServer().execute(filename);
                        } else {
                            showLoading();
                            if(pref.contains("user_img")){
                                if(!pref.getString("user_img","").equals("")){
                                    createUser(edtName.getText().toString(), pref.getString("user_phn", ""), pref.getString("user_img",""), genderSex, location
                                            , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString(),pref.getString("regId",""));
                                } else {
                                    createUser(edtName.getText().toString(), pref.getString("user_phn", ""), "No image", genderSex, location
                                            , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString(),pref.getString("regId",""));
                                }
                            } else {
                                createUser(edtName.getText().toString(), pref.getString("user_phn", ""), "No image", genderSex, location
                                        , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString(),pref.getString("regId",""));
                            }
                        }
                    }
                }
            }
        });
    }
    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }
    public final static boolean isValidEmail(CharSequence target) {
        if (target == null) {
            return false;
        } else {
            return android.util.Patterns.EMAIL_ADDRESS.matcher(target).matches();
        }
    }
    public void showLoading() {
        prog_dialog = ProgressDialog.show(Registration.this, "",
                "Please wait...", true);
        prog_dialog.setCancelable(false);
        prog_dialog.show();
    }

    private class UploadPictureToServer extends AsyncTask<String, Integer, String> {
        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
            showLoading();
            super.onPreExecute();
        }
        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            // updating percentage value
        }
        @Override
        protected String doInBackground(String... params) {
            String s = params[0];
            return uploadFile(s);
        }
        @SuppressWarnings("deprecation")
        private String uploadFile(String value) {
            String responseString = null;
            HttpClient httpclient = new DefaultHttpClient();
            httppost = new HttpPost(EndPoints.UPOLOAD_PIC);
            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(new AndroidMultiPartEntity.ProgressListener() {
                    @Override
                    public void transferred(long num) {
                        publishProgress((int) ((num / (float) totalSize) * 100));
                    }
                });
                File sourceFile = new File(value);
                // Adding file data to http body
                entity.addPart("image", new FileBody(sourceFile));
                // Extra parameters if you want to pass to server
                //entity.addPart("caption", new StringBody(ed1.getText().toString().trim(),"application/json", Charset.forName("UTF-8")));
                //entity.addPart("user_name", new StringBody(username));
                totalSize = entity.getContentLength();
                httppost.setEntity(entity);
                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();
                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200){
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                }else{
                    responseString = "Error occurred! Http Status Code: " + statusCode;
                }
            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }
            return responseString;
        }
        @Override
        protected void onPostExecute(String result) {
            Log.e("Tag", "img:" + result);
            //  Toast.makeText(getApplicationContext(), "URL: " + result, Toast.LENGTH_SHORT).show();
            // sendMessage(result.trim(), "image");
            editor.putString("user_img", result.toString().trim());
            editor.commit();
            createUser(edtName.getText().toString(), pref.getString("user_phn", ""), result.toString().trim(), genderSex, location, femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString(),pref.getString("regId",""));
            // showing the server response in an alert dialog
            //showAlert(result);
            //  finish();
            super.onPostExecute(result);
        }
    }
    private void createUser(final String passenger_name, final String passenger_phn, final String passenger_img, final String gender,
                            final String location, final String female_bike_chooser,
                            final String age, final String email, final String firebase_notification_token) {

        StringRequest strReq = new StringRequest(Request.Method.POST, EndPoints.CREATE_USER,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {
                        Log.d("Response", response.toString());
                        try {
                            if (prog_dialog.isShowing()) {
                                prog_dialog.dismiss();
                            }
                            JSONObject obj = new JSONObject(response);
                            String message = obj.getString("message");
                            editor.putString("user_id", obj.getString("user_id"));
                            editor.putString("user_img", passenger_img);
                            editor.putString("user_name", obj.getString("user_name"));
                            editor.putString("age", obj.getString("age"));
                            editor.putString("gender", obj.getString("gender"));
                            editor.putString("location", obj.getString("location"));
                            editor.putString("email", obj.getString("email"));
                            editor.putString("female_bike_chooser",female_bike_chooser);
                            editor.putString("firebase_notification_token",obj.getString("firebse_notification_token"));
                            editor.commit();
                            Toast.makeText(Registration.this, message, Toast.LENGTH_LONG).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent i = new Intent(Registration.this, MainActivity.class);
                        startActivity(i);
                        finish();

                    }
                }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("local Reg", "Error: " + error.getMessage());
                if (prog_dialog.isShowing()) {
                    prog_dialog.dismiss();
                }
                Toast.makeText(getApplicationContext(), "Please check your internet connection", Toast.LENGTH_SHORT).show();
                // Toast.makeText(getActivity().getApplicationContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                //  progressBar.setVisibility(View.GONE);
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("passenger_name", passenger_name);
                params.put("passenger_phn", passenger_phn);
                params.put("passenger_img", passenger_img);
                params.put("gender", gender);
                params.put("location", location);
                params.put("female_bike_chooser", female_bike_chooser);
                params.put("age", age);
                params.put("email", email);
                params.put("firebase_notification_token", firebase_notification_token);
                Log.i("Posting params: ", params.toString());
                return params;
            }

        };


        AppController.getInstance().addToRequestQueue(strReq, " ");
    }


    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int WritePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        } else if (WritePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        } else {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
            photoPickerIntent.setType("image/*");
            activity.startActivityForResult(photoPickerIntent, 1);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = data.getData();
                    String[] filePathColumn = {MediaStore.Images.Media.DATA};

                    Cursor cursor = getContentResolver().query(
                            selectedImage, filePathColumn, null, null, null);
                    cursor.moveToFirst();
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    String filePath = cursor.getString(columnIndex);
                    cursor.close();
                    compressImage(filePath);
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permissions granted.
                    Toast.makeText(this, "Thanks for allowing the permissions", Toast.LENGTH_LONG).show();
                    Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                    photoPickerIntent.setType("image/*");
                    startActivityForResult(photoPickerIntent, 1);
                } else {
                    // no permissions granted.
                    Toast.makeText(this, "Sorry you can't proceed without allowing the permissions", Toast.LENGTH_LONG).show();
                }
                return;
            }
        }
    }


    public void compressImage(String imageUri) {

        String filePath = getRealPathFromURI(imageUri);
        Bitmap scaledBitmap = null;

        BitmapFactory.Options options = new BitmapFactory.Options();

//      by setting this field as true, the actual bitmap pixels are not loaded in the memory. Just the bounds are loaded. If
//      you try the use the bitmap here, you will get null.
        options.inJustDecodeBounds = true;
        Bitmap bmp = BitmapFactory.decodeFile(filePath, options);

        int actualHeight = options.outHeight;
        int actualWidth = options.outWidth;

//      max Height and width values of the compressed image is taken as 816x612

        float maxHeight = 816.0f;
        float maxWidth = 612.0f;
        float imgRatio = actualWidth / actualHeight;
        float maxRatio = maxWidth / maxHeight;

//      width and height values are set maintaining the aspect ratio of the image

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                imgRatio = maxHeight / actualHeight;
                actualWidth = (int) (imgRatio * actualWidth);
                actualHeight = (int) maxHeight;
            } else if (imgRatio > maxRatio) {
                imgRatio = maxWidth / actualWidth;
                actualHeight = (int) (imgRatio * actualHeight);
                actualWidth = (int) maxWidth;
            } else {
                actualHeight = (int) maxHeight;
                actualWidth = (int) maxWidth;

            }
        }

//      setting inSampleSize value allows to load a scaled down version of the original image

        options.inSampleSize = calculateInSampleSize(options, actualWidth, actualHeight);

//      inJustDecodeBounds set to false to load the actual bitmap
        options.inJustDecodeBounds = false;

//      this options allow android to claim the bitmap memory if it runs low on memory
        options.inPurgeable = true;
        options.inInputShareable = true;
        options.inTempStorage = new byte[16 * 1024];

        try {
//          load the bitmap from its path
            bmp = BitmapFactory.decodeFile(filePath, options);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();

        }
        try {
            scaledBitmap = Bitmap.createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888);
        } catch (OutOfMemoryError exception) {
            exception.printStackTrace();
        }

        float ratioX = actualWidth / (float) options.outWidth;
        float ratioY = actualHeight / (float) options.outHeight;
        float middleX = actualWidth / 2.0f;
        float middleY = actualHeight / 2.0f;

        Matrix scaleMatrix = new Matrix();
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        Canvas canvas = new Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(bmp, middleX - bmp.getWidth() / 2, middleY - bmp.getHeight() / 2, new Paint(Paint.FILTER_BITMAP_FLAG));

//      check the rotation of the image and display it properly
        ExifInterface exif;
        try {
            exif = new ExifInterface(filePath);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, 0);

            Matrix matrix = new Matrix();
            if (orientation == 6) {
                matrix.postRotate(90);

            } else if (orientation == 3) {
                matrix.postRotate(180);

            } else if (orientation == 8) {
                matrix.postRotate(270);

            }
            scaledBitmap = Bitmap.createBitmap(scaledBitmap, 0, 0,
                    scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        FileOutputStream out = null;
        filename = getFilename();
        try {
            out = new FileOutputStream(filename);

//          write the compressed bitmap at the destination specified by filename.
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);

            deleteFolder();
            //ekhane upload er kaaj hobe

            Picasso.with(this).load(new File(filename)).into(profileImage);
            isPhotoUploaded = true;

            //ekhane khela hobe
          /*  Intent i = new Intent(SharingActivity.this,ImageUpActivity.class);
            i.putExtra("filePath", filename);
            i.putExtra("isImage", true);
            startActivity(i);*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


    }


    public static String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "Dako/Profile Images");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

    }


    public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        final float totalPixels = width * height;
        final float totalReqPixelsCap = reqWidth * reqHeight * 2;
        while (totalPixels / (inSampleSize * inSampleSize) > totalReqPixelsCap) {
            inSampleSize++;
        }

        return inSampleSize;
    }


    private String getRealPathFromURI(String contentURI) {
        Uri contentUri = Uri.parse(contentURI);
        Cursor cursor = getContentResolver().query(contentUri, null, null, null, null);
        if (cursor == null) {
            return contentUri.getPath();
        } else {
            cursor.moveToFirst();
            int index = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            return cursor.getString(index);
        }
    }


    private void deleteFolder() {
        File dir = new File(Environment.getExternalStorageDirectory() + "/Pictures/test");
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                new File(dir, children[i]).delete();
            }

            dir.delete();
        }
    }

    public void femaleChooserDialog(String title) {
        LayoutInflater mLayoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View view = mLayoutInflater.inflate(R.layout.female_biker_chooser_dialog, null);
        final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(title);
        alertDialog.setCancelable(false);
        alertDialog.setIcon(R.drawable.logo);
        final RadioGroup radioPrefGroup = (RadioGroup) view.findViewById(R.id.radioGroup);
       /* radioPrefGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton radioPrefButton=(RadioButton)view.findViewById(checkedId);

                alertDialog.dismiss();
            }
        });*/
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RadioButton radioPrefButton = (RadioButton) view.findViewById(radioPrefGroup.getCheckedRadioButtonId());
                femaleChooser = radioPrefButton.getText().toString();
                dialog.dismiss();
            }
        });


        alertDialog.setView(view);
        alertDialog.show();
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
                            Log.i("LoginActivity", response.toString());
                            // Get facebook data from login

                            Bundle bFacebookData = getFacebookData(object);
                            edtName.append(bFacebookData.getString("first_name") + " " + bFacebookData.getString("last_name"));
                            edtAge.append(parseAge(bFacebookData.getString("birthday")));
                            Log.e("Facebook gender", bFacebookData.getString("gender"));
                            Log.e("Facebook birthday", bFacebookData.getString("birthday"));
                            if(!bFacebookData.containsKey("email")){
                                //do nothing
                            }else {
                                Log.i("email", bFacebookData.getString("email").toString());
                                edtEmail.append(bFacebookData.getString("email"));
                            }
                            if(bFacebookData.getString("gender").equals("male")){
                                radioMaleButton.setChecked(true);
                            } else if(bFacebookData.getString("gender").equals("female")){
                                radioFemaleButton.setChecked(true);
                            } else {
                                //do nothing
                            }
                            Picasso.with(Registration.this).load(dp).into(profileImage);
                            editor.putString("user_img", dp);
                            editor.commit();
                            LoginManager.getInstance().logOut();

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

                                        Intent intent = new Intent(Registration.this,MainActivity.class);
                                        startActivity(intent);
                }

                @Override
                public void onCancel() {
                    // App code
                }

                @Override
                public void onError(FacebookException exception) {
                    Toast.makeText(Registration.this, "Sorry We couldn't connect to your facebook.try again", Toast.LENGTH_SHORT).show();
                }
            });

        }
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public String parseAge(String birthdateStr) {
        String year = "", monthNumber = "", day = "";
        SimpleDateFormat df = new SimpleDateFormat("dd/mm/yyyy");
        try {
            Date birthdate = df.parse(birthdateStr);
            String dayOfTheWeek = (String) DateFormat.format("EEEE", birthdate); // Thursday
            day = (String) DateFormat.format("dd", birthdate); // 20
            String monthString = (String) DateFormat.format("MMM", birthdate); // Jun
            monthNumber = (String) DateFormat.format("mm", birthdate); // 06
            year = (String) DateFormat.format("yyyy", birthdate); // 2013

        } catch (ParseException e) {
            e.printStackTrace();
        }


        return getAge(Integer.parseInt(year), Integer.parseInt(monthNumber), Integer.parseInt(day));
    }

    private String getAge(int year, int month, int day) {
        Log.e("Facebook year", year + "");
        Log.e("Facebook month", month + "");
        Log.e("Facebook day", day + "");
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();
        dob.set(year, month, day);
        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }
        Integer ageInt = new Integer(age);
        String ageS = ageInt.toString();
        return ageS;
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


}
