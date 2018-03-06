package com.lab.multiplexer.tomtom.Activity;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.lab.multiplexer.tomtom.Activity.Helper.AppController;
import com.lab.multiplexer.tomtom.Activity.Helper.ConnectivityReceiver;
import com.lab.multiplexer.tomtom.Activity.Helper.EndPoints;
import com.lab.multiplexer.tomtom.Activity.Model.Biker;
import com.lab.multiplexer.tomtom.Activity.Model.NearBiker;
import com.lab.multiplexer.tomtom.Activity.Model.User;
import com.lab.multiplexer.tomtom.Activity.Parser.DirectionsJSONParser;
import com.lab.multiplexer.tomtom.R;
import android.provider.Settings.Secure;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.view.View.GONE;

/**
 * Created by Majid on 5/20/2017.
 */

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback, View.OnClickListener, GoogleMap.OnMapLongClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, PlaceSelectionListener, ConnectivityReceiver.ConnectivityReceiverListener {

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    //Address Object for get address later
    Address address = null;
    Address currentaddress = null;
    Address destinationaddress = null;
    String notficationToken = "", bikerNotficationToken = "";
    SharedPreferences pref;
    SharedPreferences.Editor editor;
    boolean isInternetAvailabe = false;
    //Key for Permission
    private static final int MAP_PERMISSION_KEY = 111;
    private static final int REQUEST_SELECT_PLACE = 911;


    //Google Map
    private GoogleMap mMap;

    ///Polyline
    Polyline roadpolyline;

    //To store longitude and latitude from map
    private double longitude;
    private double latitude;

    //Buttons
    private Button requestForRide;

    //Google ApiClient
    private GoogleApiClient googleApiClient;

    ///map Marker
    MarkerOptions originMarkerOption, destinationMarkerOption, bikerMarkerOption;

    Marker origiMarker, destinationMaerker;
    //Origin & destination Latitude Longitude
    LatLng origin, destination;

    //Distance & Duration
    String rideDeatailsStr = " ";
    String destinationAddressStr = " ";
    //Layout
    LinearLayout linearLayout;
    TextView raidDetails, currentLocation;
    //// Must be Unique and Come from sever
    protected String ID = "2187340";

    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference bikerRef = database.getReference("Driver");
    DatabaseReference userRef = database.getReference("Rider").child(ID);
    DatabaseReference onlineBikerRef = database.getReference("Online");
    DatabaseReference loggedInBikerRef = database.getReference("LoggedIn");
    DatabaseReference onRideBikerRef = database.getReference("OnRide");

    User user;


    DataSnapshot mDataSnapshot;

    LocationManager locationManager;

    List<NearBiker>  nearBikerByDistance;
    List<Biker> bikerList;
    List<Biker> nearBikerList;
    ArrayList<String> onlineBikerKey, loggedInBikerKey, onRideBikerKey;

    ProgressDialog prog_dialog;
    public double bikerUserMaxDistance = 2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getSupportActionBar().setTitle("Find your ride");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        pref = getApplicationContext().getSharedPreferences("MyPref", MODE_PRIVATE);
        editor = pref.edit();
        notficationToken = pref.getString("regId", "");
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        String token = FirebaseInstanceId.getInstance().getToken();
        //Initializing googleapi client
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        //Initializing views and adding onclick listeners
        linearLayout =  findViewById(R.id.ridedetailsLayout);
        raidDetails =  findViewById(R.id.destinationTimeTV);

        requestForRide = (Button) findViewById(R.id.findRides);
        currentLocation = (TextView) findViewById(R.id.currentPositionTV);
        requestForRide.setOnClickListener(this);

        //   setDirection.setClickable(false);

        onlineBikerKey = new ArrayList<>();
        onRideBikerKey = new ArrayList<>();
        loggedInBikerKey = new ArrayList<>();
        bikerList = new ArrayList<Biker>();
        nearBikerList = new ArrayList<Biker>();

// Load Biker Information from Firebse
        //updateBikerListInformation();


        nearBikerByDistance = new ArrayList<NearBiker>();


      /*  Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                if(nearBikerByDistance.size()>0){
                    notficationToken = nearBikerByDistance.get(0).getBiker().getNotificationToken();
                } else {
                    Toast.makeText(getApplicationContext(), "Sorry no biker found please search again", Toast.LENGTH_SHORT).show();
                }
                *//*for (int i =0 ; i<nearBikerByDistance.size();i++){
                    Log.e("NearBiker Phone : " , nearBikerByDistance.get(i).getBiker().getPhoneNumber()+ "Distance :" +nearBikerByDistance.get(i).getDistance()+" ");
                    notficationToken = nearBikerByDistance.get(i).getBiker().getNotificationToken();
                }*//*
            }
        },3000);*/


    }

    public void showLoading() {
        prog_dialog = ProgressDialog.show(MapActivity.this, "",
                "Please wait...", true);
        prog_dialog.setCancelable(false);
        prog_dialog.show();
    }

    private void sendNotification(final String regId, final String userName, final String userPhone,
                                  final String userImg, final String from, final String to, final String another_regId,final String current_location) {

        StringRequest strReq = new StringRequest(Request.Method.POST, EndPoints.SEND_NOTIFICATION,
                new Response.Listener<String>() {

                    @Override
                    public void onResponse(String response) {

                        if (prog_dialog.isShowing()) {
                            prog_dialog.dismiss();
                        }
                        Log.d("Response", response.toString());
                        try {

                            JSONObject obj = new JSONObject(response);
                            Toast.makeText(MapActivity.this, obj.toString(), Toast.LENGTH_SHORT).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


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
                params.put("regId", regId);
                params.put("userName", userName);
                params.put("userPhone", userPhone);
                params.put("userImg", userImg);
                params.put("from", from.replace("Destination : ", ""));
                params.put("to", to);
                params.put("another_regId", another_regId);
                params.put("current_location",current_location);
                Log.i("Posting params: ", params.toString());
                return params;
            }

        };


        AppController.getInstance().addToRequestQueue(strReq, " ");
    }


    public List getUpdatedNearBiker() {
        updateBikerOject();

        return nearBikerByDistance;
    }

    public void updateBikerListInformation() {

        updateOnlineBiker();
        updateOnRideBiker();
        updateLoggedInBiker();


        // this method collect all data as biker object and store in List for single time
        // For further update just call this method and this will give you all biker and nearest all biker in a List
        updateBikerOject();

    }

    void updateBikerOject() {

        bikerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                List<String> nearBikers =new ArrayList<>();
                if (!bikerList.isEmpty() || !nearBikerList.isEmpty()) {
                    bikerList.clear();
                    nearBikerList.clear();
                }
                for (DataSnapshot data : dataSnapshot.getChildren()) {
                    Biker tempBiker = data.getValue(Biker.class);
                    bikerList.add(tempBiker);
                    nearBikerCheck(tempBiker);
                    nearBikers.add(data.getValue().toString());

                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void nearBikerCheck(Biker tempBiker) {
        try{
            double distance = distanceBtweenBikerAndRaider(new LatLng(Double.valueOf(tempBiker.getCurrentLat()), Double.valueOf(tempBiker.getCurrentLong())), origin);
            if (distance <= bikerUserMaxDistance && distance >= 0.0) {
                Log.e("Distance", distance + "");
                nearBikerList.add(tempBiker);
                nearBikerByDistance.add(new NearBiker(distance, tempBiker));
                updateNearBikerListByDistance();
            } else {
                Log.d("TAG", "No data");
            }
        }catch (Exception e){
            e.getMessage();
        }


    }

    public void updateNearBikerListByDistance() {

        if (nearBikerByDistance != null) {
            Collections.sort(nearBikerByDistance, new Comparator<NearBiker>() {
                @Override
                public int compare(NearBiker o1, NearBiker o2) {
                    return Double.compare(o1.getDistance(), o2.getDistance());
                }
            });
        }
    }

    private void updateLoggedInBiker() {

        loggedInBikerRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {

                loggedInBikerKey.add(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

                loggedInBikerKey.remove(loggedInBikerKey.indexOf(dataSnapshot.getKey()));
                Log.e("Size", String.valueOf(loggedInBikerKey.size()));
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    void updateOnRideBiker() {
        onRideBikerRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                onRideBikerKey.add(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

                onRideBikerKey.remove(onlineBikerKey.indexOf(dataSnapshot.getKey()));

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    void updateOnlineBiker() {


        onlineBikerRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                onlineBikerKey.add(dataSnapshot.getKey());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

                onlineBikerKey.remove(onlineBikerKey.indexOf(dataSnapshot.getKey()));


            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        /*onlineBikerRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {


                if (!onlineBikerKey.isEmpty()) {

                    onlineBikerKey.clear();

                }


                for (DataSnapshot ds : dataSnapshot.getChildren()) {

                    onlineBikerKey.add(ds.getKey());
                    Log.e("Added", ds.getKey());


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/
    }


    private boolean checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        return isConnected;
    }

    @Override
    protected void onResume() {
        super.onResume();

        // register connection status listener
        AppController.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {

        isInternetAvailabe = isConnected;
    }


    public double distanceBtweenBikerAndRaider(LatLng bikerlatlng, LatLng raiderLatlong) {


        double latitude = bikerlatlng.latitude;
        double longitude = bikerlatlng.longitude;
        double distance = 0;
        Location crntLocation = new Location("crntlocation");
        crntLocation.setLatitude(raiderLatlong.latitude);
        crntLocation.setLongitude(raiderLatlong.longitude);

        Location newLocation = new Location("newlocation");
        newLocation.setLatitude(latitude);
        newLocation.setLongitude(longitude);


//float distance = crntLocation.distanceTo(newLocation);  in meters
        distance = crntLocation.distanceTo(newLocation) / 1000; // in km


        return distance;
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }
    @Override
    protected void onStop() {
//        googleApiClient.disconnect();
        super.onStop();
    }

    //Getting current location
    private void getCurrentLocation() {
        mMap.clear();
        //    Log.d("Detection", "getcurrentlocation");

        //Check Permission for then android 6.0+
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MAP_PERMISSION_KEY);
            return;
        }
        //collect location from location service
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        mMap.setMyLocationEnabled(true); //enable current position floating button
        if (location != null) {
            //Getting longitude and latitude
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            //origin = new LatLng(latitude, longitude);
            //  Log.d("Detection", "Loaction not null");

            ///User location check that check that user is inside or outside dhaka
            String address = getAddress(new LatLng(latitude, longitude));
            if (address.equalsIgnoreCase("DHAKA")) {
                // Draw map over map fragment
                drawMap();

                //Autocomplete Location search initialization
                locationSearch();
            } else {
                Toast.makeText(this, "  This Service Only Available Inside Dhaka City ", Toast.LENGTH_LONG).show();
            }
        }
    }

    //Function to Draw the map
    private void drawMap() {

        //  Log.d("Detection", "Draw googl map");

        //Creating a LatLng Object to store Coordinates
        LatLng latLng = new LatLng(latitude, longitude);
        this.origin = latLng;

        //Adding  Origin marker to map
        originMarkerOption = new MarkerOptions()
                .position(latLng)
                .draggable(false) //setting position
                .title("Your Position")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerstart));
        origiMarker = mMap.addMarker(originMarkerOption);


        destinationMarkerOption = new MarkerOptions()
                .position(latLng)
                .title("Destination")
                .icon(BitmapDescriptorFactory.fromResource(R.mipmap.markerend));

        destinationMaerker = mMap.addMarker(destinationMarkerOption);
        destinationMaerker.setVisible(false);


        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
        //Animating the camera
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));


        Geocoder gc = new Geocoder(this);
        try {
            List<Address> list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);

            currentaddress = list.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        currentLocation.setText(currentaddress.getAddressLine(0) + ", " + currentaddress.getAddressLine(1));
        updateBikerListInformation();
    }
    //When MAp is ready. This method is called 1st after oncreate
    @Override
    public void onMapReady(GoogleMap googleMap) {
        // Log.d("Detection", "on map ready");
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
        mMap.setOnMapLongClickListener(this);


        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(latitude, longitude)));
        //Animating the camera
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            checkLocationPermission();
            return;
        }
    }
    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }
    //Permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MAP_PERMISSION_KEY) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(this, "Permission accepted ", Toast.LENGTH_SHORT).show();

            }
        }
    }
    @Override
    public void onConnected(Bundle bundle) {
        //   Log.d("Detection", "onconnected");

        //Set current Position after onMapReady
        getCurrentLocation();


    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

        Toast.makeText(this, connectionResult.toString(), Toast.LENGTH_SHORT).show();
    }

    //Set destination on map long click operation
    @Override
    public void onMapLongClick(LatLng latLng) {

        if (checkConnection()) {


            // Get address From GeoCode Api and Geocode class
            String address = getAddress(latLng);
            Log.e("Address", address);

            //check address that is inside or outside dhaka city
            if (address.equalsIgnoreCase("DHAKA")) {
                //  Log.e("Address ", address + " inside dhaka ");
                //Clearing all the markers
                this.destination = latLng;
                linearLayout.setVisibility(GONE);
                Geocoder gc = new Geocoder(this);
                try {
                    List<Address> list = gc.getFromLocation(destination.latitude, destination.longitude, 1);

                    destinationaddress = list.get(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                destinationAddressStr = (destinationaddress.getAddressLine(0) + ", " + destinationaddress.getAddressLine(1));
                markerPosition(latLng, destinationAddressStr);

                if (roadpolyline != null) {
                    roadpolyline.remove();
                }
                drawDirectionPolyLine(this.origin, this.destination);

                //Moving the camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(destination));
                //Animating the camera
                mMap.animateCamera(CameraUpdateFactory.zoomTo(14));

                raidDetails.setText(rideDeatailsStr);
                linearLayout.setVisibility(View.VISIBLE);

            } else {
                //  Log.e("Address " , address + " inside dhaka ");
                Toast.makeText(this, "  This Service Only Available Inside Dhaka City ", Toast.LENGTH_LONG).show();

                if (roadpolyline != null) {
                    roadpolyline.remove();
                }
                //Moving the camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
                //Animating the camera
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                destination = null;
                linearLayout.setVisibility(View.GONE);
            }
        } else {
            Toast.makeText(this, " Please Turn On Your Internet Connection", Toast.LENGTH_SHORT).show();
        }
    }
    //Set Marker position
    public void markerPosition(LatLng latLng, String destinationAddressStr) {

        destinationMaerker.setPosition(latLng);
        destinationMaerker.setTitle(destinationAddressStr);
        destinationMaerker.setVisible(true);

    }


    // Get Address from CeoCoder and return locality from address class
    public String getAddress(LatLng latLng) {

        Geocoder gc = new Geocoder(this);
        try {
            List<Address> list = gc.getFromLocation(latLng.latitude, latLng.longitude, 1);

            address = list.get(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return String.valueOf(address.getLocality());
    }
    @Override
    public void onClick(View v) {
        if (checkConnection()) {
            if (v == requestForRide) {
                if (origin != null && destination != null) {
                    showLoading();
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
//                            getUpdatedNearBiker();
                           // String android_device_id = Secure.getString(getApplicationContext().getContentResolver(), Secure.ANDROID_ID);

                            //getUpdateBiker er location
                            if (nearBikerByDistance.size() > 0) {
                                bikerNotficationToken = nearBikerByDistance.get(0).getBiker().getNotificationToken();
                                sendNotification(notficationToken, pref.getString("user_name", ""), pref.getString("user_phn", ""), pref.getString("user_img", ""),
                                        currentLocation.getText().toString(), destinationAddressStr, bikerNotficationToken,origin.toString());

                            } else {
                                Toast.makeText(getApplicationContext(), "Sorry no biker found please search again", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(MapActivity.this, MapActivity.class);
                                startActivity(intent);
                                finish();
                            }
                /*for (int i =0 ; i<nearBikerByDistance.size();i++){
                    Log.e("NearBiker Phone : " , nearBikerByDistance.get(i).getBiker().getPhoneNumber()+ "Distance :" +nearBikerByDistance.get(i).getDistance()+" ");
                    notficationToken = nearBikerByDistance.get(i).getBiker().getNotificationToken();
                }*/
                        }

                    }, 3000);
                } else if (destination == null) {
                    Toast.makeText(getApplicationContext(), "Please select your destination first", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, " Please Turn On Your Internet Connection", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void refreshLocation() {


        //Check Permission for then android 6.0+
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MAP_PERMISSION_KEY);
            return;
        }
        //collect location from location service
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        mMap.setMyLocationEnabled(true); //enable current position floating button


        if (location != null) {
            //Getting longitude and latitude
            longitude = location.getLongitude();
            latitude = location.getLatitude();

            this.origin = new LatLng(latitude, longitude);

        }


    }

    // Draw Poly line Over road
    public void drawDirectionPolyLine(LatLng origin, LatLng destination) {

        // Getting URL to the Google Directions API
        String url = getDirectionsUrl(origin, destination);

        DownloadTask downloadTask = new DownloadTask();

        // Start downloading json data from Google Directions API
        downloadTask.execute(url);

    }


    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }


    // A class to parse the Google Places in JSON format
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {
        String distance = "";
        String duration = "";

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();
            String distance = "";
            String duration = "";


            if (result.size() < 1) {
                Toast.makeText(getBaseContext(), "Too Short Distance ", Toast.LENGTH_SHORT).show();
                return;
            }


            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    if (j == 0) {
                        // Get distance from the list
                        distance = point.get("distance");
                        continue;

                    } else if (j == 1) {

                        // Get duration from the list
                        duration = point.get("duration");
                        continue;
                    }

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);// my current possition Lat Lon

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.parseColor("#e64702"));

            }

            String[] arr_distance = distance.split(" "); //split on space
            Double digit_distance = Double.parseDouble(arr_distance[0]); // First element of distance as a double to digit.
            DecimalFormat df = new DecimalFormat("#.##");
            String unit_distance = arr_distance[1];

            String[] arr_duration = duration.split(" ");
            Double digit_duration = Double.parseDouble(arr_duration[0]);//first element of duration as double valur
            Double fare_InDouble;
            int acctualFare;
            switch (unit_distance) {
                case "m":
                    fare_InDouble = 25 + ((digit_distance * 12)/1000) + (0.5 * digit_duration);
                    acctualFare = fare_InDouble.intValue();
                    //set Distance and Duration in textview
                    raidDetails.setText("   Distance :  " + distance + " \n   Duration : " + duration + " \n   Fare rate : " + acctualFare + " tk");

                    //System.out.println("DISTANCEHECKER" + df.format(digit_distance));
                    break;
                case "km":
                    //System.out.println("DISTANCEHECKER" + df.format(digit_distance*1000));
                    fare_InDouble = 25 + (digit_distance * 12) + (0.5 * digit_duration);
                    acctualFare = fare_InDouble.intValue();
                    //set Distance and Duration in textview
                    raidDetails.setText("   Distance :  " + distance + " \n   Duration : " + duration + " \n   Fare rate : " + acctualFare + " tk");

                    break;//more Code Omitted.as per your requirement
            }


            // Drawing polyline in the Google Map for the i-th route
            roadpolyline = mMap.addPolyline(lineOptions);
        }
    }

    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;


        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }


    //A method to download json data from url
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    //Auto Complete Search Location
    public void locationSearch() {

        //AutoComplete search Filter setup
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_NONE)
                .setCountry("BD")
                .build();
        //initialization placeAutocomplete fragment widget
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_fragment);


        autocompleteFragment.setHint("Set Destination here ");
        autocompleteFragment.setFilter(typeFilter);
        autocompleteFragment.setMenuVisibility(true);
        autocompleteFragment.setBoundsBias(null);
        autocompleteFragment.setOnPlaceSelectedListener(this);
        autocompleteFragment.getView().findViewById(R.id.place_autocomplete_clear_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //example : way to access view from PlaceAutoCompleteFragment
                //((EditText) autocompleteFragment.getView().findViewById(R.id.place_autocomplete_search_input)).setText("");
                startActivity(new Intent(MapActivity.this, MapActivity.class));
                finish();
                overridePendingTransition(0, 0);
            }
        });
    }

    @Override
    public void onPlaceSelected(Place place) {

        if (checkConnection()) {
            String address = getAddress(place.getLatLng());
            if (address.equalsIgnoreCase("DHAKA")) {

                this.destination = place.getLatLng();


                //Moving the camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(destination));
                //Animating the camera
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));


                destinationAddressStr = place.getAddress().toString();
                markerPosition(destination, destinationAddressStr);

                if (roadpolyline != null) {
                    roadpolyline.remove();
                }
                drawDirectionPolyLine(this.origin, this.destination);

                raidDetails.setText(rideDeatailsStr);
                linearLayout.setVisibility(View.VISIBLE);


            } else {
                //  Log.e("Address " , address + " inside dhaka ");
                Toast.makeText(this, " This Service Only Available Inside Dhaka City", Toast.LENGTH_LONG).show();

                //Moving the camera
                mMap.moveCamera(CameraUpdateFactory.newLatLng(origin));
                //Animating the camera
                mMap.animateCamera(CameraUpdateFactory.zoomTo(13));
                this.destination = null;
                if (roadpolyline != null) {
                    roadpolyline.remove();
                }
                destinationMaerker.setVisible(false);
                linearLayout.setVisibility(View.GONE);


            }


        } else {
            Toast.makeText(this, " Please Turn On Your Internet Connection ", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onError(Status status) {

        Toast.makeText(this, "Place selection failed: " + status.getStatusMessage(),
                Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_SELECT_PLACE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                this.onPlaceSelected(place);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                this.onError(status);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
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
            Intent i = new Intent(MapActivity.this, Settings.class);
            startActivity(i);
            return true;
        } else if (id == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
