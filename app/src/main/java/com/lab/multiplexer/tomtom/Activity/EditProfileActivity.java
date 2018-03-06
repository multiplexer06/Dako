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
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.facebook.CallbackManager;
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
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import static com.lab.multiplexer.tomtom.Activity.Registration.isValidEmail;


public class EditProfileActivity extends AppCompatActivity implements View.OnClickListener {


    CircleImageView profileImage;
    CircleImageView selectImage;
    TextView phoneNumber;
    EditText edtName, edtAge, edtEmail;
    RadioGroup radioGender, radioLocation;
    RadioButton radioButton, radioMale, radioFemale, radioDhaka, radioChittagong;
    Button updateButton;
    ProgressDialog prog_dialog;
    HttpPost httppost;
    long totalSize = 0;

    SharedPreferences pref;
    SharedPreferences.Editor editor;
    RadioGroup genderSelection, locationSelection;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private CallbackManager callbackManager;
    public String filename = "";
    boolean isPhotoSelected = false;
    String femaleChooser = "-", genderSex = "-", location = "-";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        callbackManager = CallbackManager.Factory.create();
        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        phoneNumber = findViewById(R.id.phone_number);


        edtName = findViewById(R.id.edit_name);
        edtAge = findViewById(R.id.edit_age);
        edtEmail = findViewById(R.id.edit_email);

        radioMale = findViewById(R.id.radioMale);
        radioFemale = findViewById(R.id.radioFemale);
        radioDhaka = findViewById(R.id.radioDhaka);
        radioChittagong = findViewById(R.id.radioCtg);
        profileImage = findViewById(R.id.profile_image);

        radioGender = findViewById(R.id.radioGender);
        radioLocation = findViewById(R.id.radioLocation);

        profileImage = findViewById(R.id.profile_image);
        profileImage.setOnClickListener(this);

        selectImage = findViewById(R.id.select_image);
        selectImage.setOnClickListener(this);

        updateButton = findViewById(R.id.upload_button);
        updateButton.setOnClickListener(this);

        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);

        String phone = pref.getString("user_phn", "").trim();
        String name = pref.getString("user_name", "").trim();
        String age = pref.getString("age", "").trim();
        String email = pref.getString("email", "").trim();
        String gender = pref.getString("gender", "").trim();
        String location = pref.getString("location", "").trim();

        phoneNumber.setText(phone);
        edtName.setText(name);
        edtAge.setText(age);
        edtEmail.setText(email);
        if (pref.getString("user_img", "").equals("No image")) {
            Picasso.with(this).load(R.drawable.profile_picture).into(profileImage);
        } else {
            Picasso.with(this).load(pref.getString("user_img", "").trim()).error(R.drawable.user).into(profileImage);
        }

        if (gender.equals("Male")) {
            radioMale.setChecked(true);
        } else {
            radioFemale.setChecked(true);
        }

        if (location.equals("Dhaka")) {
            radioDhaka.setChecked(true);
        } else {
            radioChittagong.setChecked(true);
        }
        radioFemale.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    femaleChooserDialog("Please choose your priority");
                }
            }
        });


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                //Write your logic here
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.profile_image:
                verifyStoragePermissions(EditProfileActivity.this);
                break;

            case R.id.select_image:
                verifyStoragePermissions(EditProfileActivity.this);
                break;

            case R.id.upload_button:
                if (edtName.getText().toString().equals("")) {
                    edtName.setError("Name is required");
                } else if (edtAge.getText().toString().equals("")) {
                    edtAge.setError("Age is required");
                } else if (!isValidEmail(edtEmail.getText().toString())) {
                    edtEmail.setError("Please enter your email correctly");
                } else {
                    radioButton = findViewById(radioGender.getCheckedRadioButtonId());
                    genderSex = radioButton.getText().toString();
                    radioButton = findViewById(radioLocation.getCheckedRadioButtonId());
                    location = radioButton.getText().toString();
                    if (genderSex.equals("Female") && femaleChooser.equals("-")) {
                        femaleChooserDialog("Please choose your priority to proceed");
                    } else if (!isInternetAvailable()) {
                        Snackbar snackbar = Snackbar
                                .make(v, "No internet connections", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    } else {
                        if (isPhotoSelected) {
                            new UploadPictureToServer().execute(filename);
                        } else {
                            showLoading();
                            if (pref.contains("user_img")) {
                                if (!pref.getString("user_img", "").equals("")) {
                                    updateUser(edtName.getText().toString(), pref.getString("user_id", ""), pref.getString("user_img", ""), genderSex, location
                                            , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString());
                                } else {
                                    updateUser(edtName.getText().toString(), pref.getString("user_id", ""), "No image", genderSex, location
                                            , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString());
                                }
                            } else {
                                updateUser(edtName.getText().toString(), pref.getString("user_id", ""), "No image", genderSex, location
                                        , femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString());
                            }

                        }

                    }
                }
                break;

            default:
                break;
        }

    }

    public void showLoading() {
        prog_dialog = ProgressDialog.show(EditProfileActivity.this, "",
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

                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                } else {
//                    responseString = sourceFile.toString();
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
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
//            editor.putString("user_img", result.trim());
//            editor.commit();
            updateUser(edtName.getText().toString(), pref.getString("user_id", ""), result.trim(), genderSex,
                    location, femaleChooser, edtAge.getText().toString(), edtEmail.getText().toString());
            super.onPostExecute(result);
        }

    }


    private void verifyStoragePermissions(Activity activity) {
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
            isPhotoSelected = true;

            //ekhane khela hobe
          /*  Intent i = new Intent(SharingActivity.this,ImageUpActivity.class);
            i.putExtra("filePath", filename);
            i.putExtra("isImage", true);
            startActivity(i);*/

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }


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

    public static String getFilename() {
        File file = new File(Environment.getExternalStorageDirectory().getPath(), "Dako/Profile Images");
        if (!file.exists()) {
            file.mkdirs();
        }
        String uriSting = (file.getAbsolutePath() + "/" + System.currentTimeMillis() + ".jpg");
        return uriSting;

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


    public boolean isInternetAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        return cm.getActiveNetworkInfo() != null;

    }

    private void updateUser(final String passenger_name, final String userID, final String passenger_img, final String gender,
                            final String location, final String female_bike_chooser,
                            final String age, final String email) {

        StringRequest strReq = new StringRequest(Request.Method.POST, EndPoints.UPDATE_PROFILE,
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
                            SharedPreferences pref = getSharedPreferences("MyPref",MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putString("user_name", obj.getString("passenger_name"));
                            editor.putString("user_img", obj.getString("passenger_img"));
                            editor.putString("age", obj.getString("age"));
                            editor.putString("gender", obj.getString("gender"));
                            editor.putString("location", obj.getString("location"));
                            editor.putString("email", obj.getString("email"));
                            editor.putString("female_bike_chooser", obj.getString("female_bike_chooser"));
                            editor.apply();
                            //anna
                            Toast.makeText(EditProfileActivity.this, message, Toast.LENGTH_LONG).show();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent i = new Intent(EditProfileActivity.this, MainActivity.class);
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
            }
        }) {

            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<String, String>();
                params.put("passenger_name", passenger_name);
                params.put("user_id", userID);
                params.put("passenger_img", passenger_img);
                params.put("gender", gender);
                params.put("location", location);
                params.put("female_bike_chooser", female_bike_chooser);
                params.put("age", age);
                params.put("email", email);

                Log.i("Posting params: ", params.toString());
                return params;
            }

        };
        AppController.getInstance().addToRequestQueue(strReq, " ");
    }

}
