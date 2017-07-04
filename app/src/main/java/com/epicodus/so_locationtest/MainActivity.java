package com.epicodus.so_locationtest;

import android.Manifest;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.support.v7.app.AppCompatActivity;
import com.epicodus.so_locationtest.Services.GeoCodingService;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.Query;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import butterknife.ButterKnife;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 111;

    private String TAG = MainActivity.class.getSimpleName();
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;
    private String city;
    private Query swarmReportQuery;
    private FirebaseRecyclerAdapter mFirebaseAdapter;
    private Double currenLatitude;
    private Double currentLongitude;
    private String userName;
    private String userId;
    private FirebaseAuth auth;
    private FirebaseAuth.AuthStateListener authListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpBlankAdapter();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(10 * 1000)
                .setFastestInterval(1 * 1000);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d("personal", "got into onConnected");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_FINE_LOCATION);
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location == null) {
            Log.d("personal", "location null");
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        } else {
            Log.d("personal", "location not null");
            handleNewLocation(location);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        Log.d("personal", "got into permissionResults");
        Log.d("personal", "request code is " + Integer.toString(requestCode) );
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("personal", "permission was granted");
                    Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                    if (location == null) {
                        Log.d("personal", "location is null inside permission");
                        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
                    } else {
                        Log.d("personal", "about to call new location");
                        handleNewLocation(location);
                    }
                }
            }
        }
    }

    private void handleNewLocation(Location location) {
        Log.d("personal", "got to new location");
        currenLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();

        Geocoder gcd = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(currenLatitude, currentLongitude, 1);
            if (addresses.size() > 0) {
                city = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();

                if (currenLatitude != null && currentLongitude != null) {
                    Log.d("personal", "Got lat and long");
                    setUpFirebaseAdapter(city);
                }
            } else {
                city = "unknown";
                Log.d("personal", "couldnt' get an address from the location");
            }
        } catch (IOException e) {
            Log.d("personal", "getFromLocation didn't work");
            e.printStackTrace();
            getCityFromHttpCall();

        }

    }

    public void getCityFromHttpCall() {
        if (currenLatitude != null && currentLongitude != null) {
            final GeoCodingService geoCodingService = new GeoCodingService();
            geoCodingService.getCity(Double.toString(currenLatitude), Double.toString(currentLongitude), new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String jsonData = response.body().string();
                        Log.d("personal", jsonData);
                        if (response.isSuccessful()) {
                            city = GeoCodingService.processResults(jsonData);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.d("personal", "IO exception");
                        city = "all";
                    } catch (JSONException e) {
                        Log.d("personal", "JSON exception");
                        city = "all";
                    }
                    setUpFirebaseAdapter(city);
                }
            });
        }
    }


    private void setUpFirebaseAdapter(final String city) {
        Log.d("personal", "got to setUpFirebaseAdapter");
    }

    private void setUpBlankAdapter() {
        Log.d("personal", "setUpBlankAdapter done");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mFirebaseAdapter != null) {
            mFirebaseAdapter.cleanup();
        }

    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d("personal", "Location services suspended. Please reconnect");

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("personal", "Location services failed with code " + connectionResult.getErrorCode());
        }
    }


    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d("personal", "location changed");
        handleNewLocation(location);
    }

}
