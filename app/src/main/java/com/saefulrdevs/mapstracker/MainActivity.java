package com.saefulrdevs.mapstracker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Address;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.saefulrdevs.mapstracker.databinding.ActivityMainBinding;

import org.osmdroid.bonuspack.location.GeocoderNominatim;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.MapView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private AutoCompleteTextView autoCompleteTextView;
    private TextView textViewLongitudeLatitude, textViewCurrentLoc;
    private GeocoderNominatim geocoder;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean locationUpdatesStarted = false;
    private LocationCallback locationCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setBackgroundDrawable(new ColorDrawable(getResources().getColor(R.color.blue_light)));

        mapView = binding.mapView;
        autoCompleteTextView = binding.autoCompleteTextView;
        textViewCurrentLoc = binding.textViewCurrentLoc;
        textViewLongitudeLatitude = binding.textViewLongitudeLatitude;
        Button btnSearch = binding.btnSearch;
        FloatingActionButton fabLiveUser = binding.fabLiveUser;
        FloatingActionButton fabQuestion = binding.fabQuestion;

        Configuration.getInstance().setUserAgentValue(getPackageName());
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE);
        mapView.setUseDataConnection(true);

        CustomZoomButtonsController zoomController = mapView.getZoomController();
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        mapView.setMultiTouchControls(true);
        mapView.invalidate();

        geocoder = new GeocoderNominatim("Maps Tracker");
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        autoCompleteTextView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (!query.isEmpty()) {
                    fetchLocationSuggestions(query);
                }
            }
        });

        btnSearch.setOnClickListener(view -> {
            String query = autoCompleteTextView.getText().toString().trim();
            if (!query.isEmpty()) {
                performSearch(query);
            }
        });

        fabLiveUser.setOnClickListener(view -> {
            checkLocationPermission();
        });

        fabQuestion.setOnClickListener(v -> {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            View popupView = LayoutInflater.from(this).inflate(R.layout.activity_popup, null);
            alertDialogBuilder.setView(popupView);
            AlertDialog alertDialog = alertDialogBuilder.create();

            alertDialog.show();
        });

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                // Mendapatkan lokasi terbaru
                Location location = locationResult.getLastLocation();
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                updateUserLocation(latitude, longitude);
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (locationUpdatesStarted) {
            startLocationUpdates();
        } else {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdates();
    }

    private void startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.requestLocationUpdates(createLocationRequest(), locationCallback, null); // Hapus parameter getMainLooper()
        locationUpdatesStarted = true;
    }

    private void stopLocationUpdates() {
        if (locationUpdatesStarted) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationUpdatesStarted = false;
        }
    }

    private void fetchLocationSuggestions(String query) {
        new AsyncTask<String, Void, List<Address>>() {
            @Override
            protected List<Address> doInBackground(String... params) {
                try {
                    return geocoder.getFromLocationName(params[0], 5);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(List<Address> results) {
                if (results != null && results.size() > 0) {
                    List<String> suggestions = new ArrayList<>();
                    for (Address address : results) {
                        suggestions.add(address.getAddressLine(0));
                    }
                    showLocationSuggestions(suggestions);
                }
            }
        }.execute(query);
    }

    private void showLocationSuggestions(List<String> suggestions) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, suggestions);
        autoCompleteTextView.setAdapter(adapter);
        autoCompleteTextView.showDropDown();
    }

    private void performSearch(String query) {
        new AsyncTask<String, Void, Address>() {
            @Override
            protected Address doInBackground(String... params) {
                try {
                    // Lakukan pencarian dengan menggunakan teks pencarian sebagai query
                    List<Address> addresses = geocoder.getFromLocationName(params[0], 1);
                    if (addresses != null && addresses.size() > 0) {
                        return addresses.get(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Address result) {
                if (result != null) {
                    GeoPoint point = new GeoPoint(result.getLatitude(), result.getLongitude());
                    mapView.getController().setCenter(point);
                    mapView.getController().setZoom(20);


                    String latitudeText = "Latitude: " + result.getLatitude();
                    String longitudeText = "Longitude: " + result.getLongitude();
                    textViewLongitudeLatitude.setText(latitudeText + "\n" + longitudeText);

                    getAddressFromLocation(result.getLatitude(), result.getLongitude());
                } else {
                    Toast.makeText(MainActivity.this, "Pencarian tidak ditemukan.", Toast.LENGTH_SHORT).show();
                }
            }
        }.execute(query);
    }

    private void getAddressFromLocation(double latitude, double longitude) {
        new AsyncTask<Double, Void, Address>() {
            @Override
            protected Address doInBackground(Double... params) {
                try {
                    List<Address> addresses = geocoder.getFromLocation(params[0], params[1], 1);
                    if (addresses != null && addresses.size() > 0) {
                        return addresses.get(0);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Address result) {
                if (result != null) {
                    String locality = result.getLocality();
                    String subAdminArea = result.getSubAdminArea();
                    String adminArea = result.getAdminArea();
                    String countryName = result.getCountryName();

                    StringBuilder addressBuilder = new StringBuilder();
                    if (locality != null) {
                        addressBuilder.append(locality);
                    }
                    if (subAdminArea != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(subAdminArea);
                    }
                    if (adminArea != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(adminArea);
                    }
                    if (countryName != null) {
                        if (addressBuilder.length() > 0) addressBuilder.append(", ");
                        addressBuilder.append(countryName);
                    }
                    textViewCurrentLoc.setText(addressBuilder.toString());
                } else {
                    textViewCurrentLoc.setText("Alamat tidak ditemukan.");
                }
            }
        }.execute(latitude, longitude);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            enableMyLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation();
            } else {
                Toast.makeText(this, "Izin akses lokasi ditolak.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        fusedLocationClient.requestLocationUpdates(createLocationRequest(), new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    double latitude = location.getLatitude();
                    double longitude = location.getLongitude();
                    updateUserLocation(latitude, longitude);
                }
            }
        }, getMainLooper());
    }

    private void updateUserLocation(double latitude, double longitude) {
        mapView.getController().setCenter(new GeoPoint(latitude, longitude));
        mapView.getController().setZoom(20);

        String latitudeText = "Latitude: " + latitude;
        String longitudeText = "Longitude: " + longitude;
        textViewLongitudeLatitude.setText(latitudeText + "\n" + longitudeText);

        getAddressFromLocation(latitude, longitude);
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return locationRequest;
    }
}
